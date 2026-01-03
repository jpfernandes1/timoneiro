"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Navbar from "@/src/components/Navbar";
import Footer from "@/src/components/Footer";
import { Button } from "@/src/components/ui/button";
import { Label } from "@/src/components/ui/label";
import { Input } from "@/src/components/ui/input";
import { RadioGroup, RadioGroupItem } from "@/src/components/ui/radio-group";
import {
  Calendar,
  Clock,
  Users,
  Ruler,
  MapPin,
  CreditCard,
  Shield,
  CheckCircle,
  AlertCircle,
  ArrowLeft,
  Loader2,
} from "lucide-react";
import { cn } from "@/src/lib/utils";
import Link from "next/link";
import { buildUrl } from "@/src/lib/api";

// Interfaces
interface Boat {
  id: number;
  name: string;
  description: string;
  type: string;
  capacity: number;
  length: number;
  speed: number;
  fabrication: number;
  amenities: string[];
  photos: string[];
  pricePerHour: number;
  city: string;
  state: string;
  marina: string;
  ownerName: string;
  ownerId: number;
}

interface BookingResponse {
  id: number;
  status: string;
  totalPrice: number;
  startDate: string;
  endDate: string;
  boat: {
    name: string;
  };
}

const CheckoutPageContent = () => {
  const searchParams = useSearchParams();
  const router = useRouter();

  // Query string reservation data
  const boatId = searchParams.get("boatId");
  const startDate = searchParams.get("startDate");
  const startTime = searchParams.get("startTime");
  const endDate = searchParams.get("endDate");
  const endTime = searchParams.get("endTime");
  const durationHours = searchParams.get("durationHours");
  const totalPriceParam = searchParams.get("totalPrice");

  // States
  const [boat, setBoat] = useState<Boat | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [processing, setProcessing] = useState(false);
  const [bookingResult, setBookingResult] = useState<BookingResponse | null>(null);
  
  // Payment form states
  const [paymentMethod, setPaymentMethod] = useState<"CREDIT_CARD" | "PIX" | "BOLETO">("CREDIT_CARD");
  const [cardData, setCardData] = useState({
    cardNumber: "",
    holderName: "",
    expirationDate: "",
    cvv: "",
  });

  // Check if all the necessary parameters are present.
  useEffect(() => {
    if (!boatId || !startDate || !startTime || !endDate || !endTime || !durationHours || !totalPriceParam) {
      setError("Dados de reserva incompletos. Por favor, volte e selecione novamente.");
      setLoading(false);
      return;
    }

    // Search boat details
    fetchBoatDetails();
  }, []);

  const fetchBoatDetails = async () => {
    try {
      const response = await fetch(buildUrl(`/boats/${boatId}`));
      if (!response.ok) {
        throw new Error("Barco não encontrado");
      }
      const data: Boat = await response.json();
      setBoat(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao carregar barco");
    } finally {
      setLoading(false);
    }
  };

  const handleCardDataChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setCardData((prev) => ({ ...prev, [name]: value }));
  };

  const formatCardNumber = (value: string) => {
    return value.replace(/\D/g, '').replace(/(\d{4})/g, '$1 ').trim();
  };

const handleExpirationDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  let value = e.target.value.replace(/\D/g, '');
  if (value.length > 2) {
    value = value.slice(0, 2) + '/' + value.slice(2, 4);
  } else if (value.length === 2) {
    value = value + '/';
  }
  setCardData(prev => ({ ...prev, expirationDate: value }));
};

// New helper function
const formatDateForBackend = (dateString: string, timeString: string) => {
  // Ensure that the date is treated as local, without timezone conversion.
  const [year, month, day] = dateString.split('-');
  const [hours, minutes] = timeString.split(':');
  
  // Returns in ISO format without timezone
  return `${year}-${month}-${day}T${hours}:${minutes}:00`;
};

  const handleBooking = async () => {
    if (!boatId || !startDate || !startTime || !endDate || !endTime) {
      alert("Dados incompletos");
      return;
    }

    // Validate card details if it's a credit card.
    if (paymentMethod === "CREDIT_CARD") {
      if (!cardData.cardNumber || !cardData.holderName || !cardData.expirationDate || !cardData.cvv) {
        alert("Por favor, preencha todos os dados do cartão.");
        return;
      }
    }

    // 1. Obtain JWT token - check different storage locations
  const token = localStorage.getItem("token") || 
                sessionStorage.getItem("token") ||
                document.cookie.split('; ').find(row => row.startsWith('token='))?.split('=')[1];

  console.log('🔑 Token encontrado:', token ? 'SIM' : 'NÃO');
  
  if (!token) {
    alert("Por favor, faça login para continuar com a reserva.");
    // Redirect to login with return to checkout.
    const redirectUrl = `/auth?redirect=/booking/checkout?${searchParams.toString()}`;
    router.push(redirectUrl);
    return;
  }

   // 2. Check if the token is valid
  try {
    const testResponse = await fetch(buildUrl(`/auth/validate`), {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (testResponse.status === 401) {
      localStorage.removeItem("token");
      sessionStorage.removeItem("token");
      alert("Sua sessão expirou. Por favor, faça login novamente.");
      router.push(`/auth?redirect=/booking/checkout?${searchParams.toString()}`);
      return;
    }
  } catch (error) {
    console.log("⚠️ Validação de token não disponível, continuando...");
  }

    // Prepare data for the backend.
    const startDateTime = formatDateForBackend(startDate!, startTime!);
    const endDateTime = formatDateForBackend(endDate!, endTime!);

    const bookingRequest = {
      boatId: parseInt(boatId),
      startDate: startDateTime,
      endDate: endDateTime,
      paymentMethod: paymentMethod,
      mockCardData: paymentMethod === "CREDIT_CARD" ? {
        cardNumber: cardData.cardNumber.replace(/\s/g, ''),
        holderName: cardData.holderName,
        expirationDate: cardData.expirationDate,
        cvv: cardData.cvv
      } : null
    };

    console.log("📤 Enviando reserva:", JSON.stringify(bookingRequest, null, 2));
  console.log("🔑 Token sendo enviado:", token.substring(0, 20) + "...");

    setProcessing(true);
    try {
      const response = await fetch(buildUrl(`/api/bookings`), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`,
        },
        body: JSON.stringify(bookingRequest),
      });

      console.log("📥 Resposta do backend:", response.status);

       if (response.status === 401) {
      localStorage.removeItem("token");
      sessionStorage.removeItem("token");
      alert("Sua sessão expirou. Por favor, faça login novamente.");
      router.push(`/auth?redirect=/booking/checkout?${searchParams.toString()}`);
      return;
    }

      if (!response.ok) {
      let errorMessage = "Erro ao criar reserva";
      try {
        const errorData = await response.json();
        console.error("❌ Erro detalhado:", errorData);
        errorMessage = errorData.message || errorData.error || errorMessage;
      } catch (e) {
        console.error("❌ Erro sem JSON:", e);
      }
      throw new Error(`${errorMessage} (Status: ${response.status})`);
    }

      const result: BookingResponse = await response.json();
      setBookingResult(result);
      
      console.log("✅ Reserva criada com sucesso:", result);
      
    } catch (err) {
      console.error("❌ Erro ao processar reserva:", err);
      alert(err instanceof Error ? err.message : "Erro ao processar reserva. Por favor, tente novamente.");
    } finally {
      setProcessing(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="pt-24 container mx-auto px-4">
          <div className="flex items-center justify-center py-16">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
            <span className="ml-3">Carregando detalhes da reserva...</span>
          </div>
        </div>
        <Footer />
      </div>
    );
  }

  if (error || !boat) {
    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="pt-24 container mx-auto px-4">
          <div className="text-center py-16">
            <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
            <h1 className="text-2xl font-bold mb-4">Erro</h1>
            <p className="text-muted-foreground mb-8">{error || "Dados não encontrados"}</p>
            <div className="flex gap-4 justify-center">
              <Button variant="outline" onClick={() => router.back()}>
                <ArrowLeft className="w-4 h-4 mr-2" />
                Voltar
              </Button>
              <Button onClick={() => router.push("/search")}>Buscar barcos</Button>
            </div>
          </div>
        </div>
        <Footer />
      </div>
    );
  }

  // Format values
  const totalPrice = parseFloat(totalPriceParam || "0");
  const duration = parseInt(durationHours || "0");
  
  const formatDate = (dateString: string) => {
  if (!dateString) return "";
  
  // Split the string and create a local date
  const [year, month, day] = dateString.split('-');
  
  // Create LOCAL date (not UTC)
  const localDate = new Date(
    parseInt(year),
    parseInt(month) - 1,  // Month is 0-indexed in JS
    parseInt(day)
  );
  
  return localDate.toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
};

  // Confirmation page after successful booking.
  if (bookingResult) {
    const getStatusText = (status: string) => {
      switch (status) {
        case "PENDING": return "Pendente";
        case "CONFIRMED": return "Confirmada";
        case "CANCELLED": return "Cancelada";
        case "FINISHED": return "Finalizada";
        default: return status;
      }
    };

    const getStatusColor = (status: string) => {
      switch (status) {
        case "PENDING": return "text-yellow-600 bg-yellow-100";
        case "CONFIRMED": return "text-green-600 bg-green-100";
        case "CANCELLED": return "text-red-600 bg-red-100";
        case "FINISHED": return "text-blue-600 bg-blue-100";
        default: return "text-gray-600 bg-gray-100";
      }
    };

    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="pt-24 container mx-auto px-4">
          <div className="max-w-2xl mx-auto">
            {/* Breadcrumb */}
            <div className="flex items-center gap-2 text-sm text-muted-foreground mb-8">
              <Link href="/" className="hover:text-foreground transition-colors">
                Home
              </Link>
              <span>/</span>
              <Link href="/search" className="hover:text-foreground transition-colors">
                Barcos
              </Link>
              <span>/</span>
              <Link href={`/boats/${boat.id}`} className="hover:text-foreground transition-colors">
                {boat.name}
              </Link>
              <span>/</span>
              <span className="text-foreground font-medium">Confirmação</span>
            </div>

            {/* Confirmation */}
            <div className="text-center mb-8">
              <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-6">
                <CheckCircle className="w-10 h-10 text-green-600" />
              </div>
              <h1 className="text-3xl font-bold mb-4">Reserva confirmada!</h1>
              <p className="text-lg text-muted-foreground">
                Sua reserva foi realizada com sucesso. Você receberá um e-mail com os detalhes.
              </p>
            </div>

            {/* Booking details */}
            <div className="bg-card rounded-2xl p-6 shadow-soft mb-8">
              <h2 className="text-xl font-semibold mb-6">Detalhes da reserva</h2>
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Código da reserva:</span>
                  <span className="font-semibold">#{bookingResult.id.toString().padStart(6, '0')}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Barco:</span>
                  <span className="font-semibold">{boat.name}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Período:</span>
                  <span className="font-semibold text-right">
                    {formatDate(startDate!)} {startTime} até<br />
                    {formatDate(endDate!)} {endTime}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Duração:</span>
                  <span className="font-semibold">{duration} horas</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Status:</span>
                  <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(bookingResult.status)}`}>
                    {getStatusText(bookingResult.status)}
                  </span>
                </div>
                <div className="pt-4 border-t border-border">
                  <div className="flex justify-between items-center">
                    <span className="text-xl font-bold">Total pago:</span>
                    <span className="text-3xl font-bold text-primary">
                      R$ {bookingResult.totalPrice.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Button variant="outline" onClick={() => router.push("/dashboard#reservas")} className="flex-1">
                Minhas reservas
              </Button>
              <Button onClick={() => router.push("/search")} className="flex-1">
                Fazer outra reserva
              </Button>
              <Button variant="ocean" onClick={() => router.push(`/boat/${boat.id}`)} className="flex-1">
                Ver detalhes do barco
              </Button>
            </div>

            {/* Aditional information */}
            <div className="mt-8 p-6 bg-blue-50 rounded-xl border border-blue-200">
              <h3 className="font-semibold text-blue-800 mb-2">Próximos passos</h3>
              <ul className="text-blue-700 space-y-2">
                <li className="flex items-start gap-2">
                  <CheckCircle className="w-5 h-5 text-green-500 shrink-0 mt-0.5" />
                  <span>Você receberá um e-mail com todos os detalhes da reserva</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="w-5 h-5 text-green-500 shrink-0 mt-0.5" />
                  <span>Entre em contato com o proprietário para combinar os detalhes do embarque</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="w-5 h-5 text-green-500 shrink-0 mt-0.5" />
                  <span>Leve um documento de identificação no dia do passeio</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
        <Footer />
      </div>
    );
  }

  // Checkout homepage
  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <div className="container mx-auto px-4 pt-24 pb-16">
        {/* Breadcrumb */}
        <div className="flex items-center gap-2 text-sm text-muted-foreground mb-6">
          <Link href="/" className="hover:text-foreground transition-colors">
            Home
          </Link>
          <span>/</span>
          <Link href="/search" className="hover:text-foreground transition-colors">
            Barcos
          </Link>
          <span>/</span>
          <Link href={`/boat/${boat.id}`} className="hover:text-foreground transition-colors">
            {boat.name}
          </Link>
          <span>/</span>
          <span className="text-foreground font-medium">Checkout</span>
        </div>

        <h1 className="text-3xl font-bold mb-2">Finalizar reserva</h1>
        <p className="text-muted-foreground mb-8">Confira os detalhes e preencha os dados para concluir</p>

        <div className="grid lg:grid-cols-3 gap-8">
          {/* Left column: Booking and payment summary */}
          <div className="lg:col-span-2 space-y-8">
            {/* Booking Summary */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h2 className="text-xl font-semibold mb-6">Resumo da reserva</h2>
              <div className="space-y-6">
                <div className="flex gap-4">
                  {boat.photos && boat.photos.length > 0 && (
                    <img
                      src={boat.photos[0]}
                      alt={boat.name}
                      className="w-24 h-24 rounded-xl object-cover"
                    />
                  )}
                  <div>
                    <h3 className="font-semibold text-lg">{boat.name}</h3>
                    <div className="flex items-center gap-2 text-muted-foreground mt-1">
                      <MapPin className="w-4 h-4" />
                      <span>{boat.city}, {boat.state}</span>
                    </div>
                    <div className="flex items-center gap-4 mt-3">
                      <div className="flex items-center gap-1">
                        <Users className="w-4 h-4" />
                        <span className="text-sm">{boat.capacity} pessoas</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <Ruler className="w-4 h-4" />
                        <span className="text-sm">{boat.length}m</span>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 pt-6 border-t border-border">
                  <div>
                    <h4 className="font-medium mb-2">Data e hora de início</h4>
                    <div className="flex items-center gap-2">
                      <Calendar className="w-4 h-4" />
                      <span>{formatDate(startDate!)}</span>
                    </div>
                    <div className="flex items-center gap-2 mt-1">
                      <Clock className="w-4 h-4" />
                      <span>{startTime}</span>
                    </div>
                  </div>
                  <div>
                    <h4 className="font-medium mb-2">Data e hora de término</h4>
                    <div className="flex items-center gap-2">
                      <Calendar className="w-4 h-4" />
                      <span>{formatDate(endDate!)}</span>
                    </div>
                    <div className="flex items-center gap-2 mt-1">
                      <Clock className="w-4 h-4" />
                      <span>{endTime}</span>
                    </div>
                  </div>
                </div>

                <div className="pt-6 border-t border-border">
                  <div className="flex justify-between items-center">
                    <span className="text-muted-foreground">Duração total</span>
                    <span className="font-semibold">{duration} horas</span>
                  </div>
                  <div className="flex justify-between items-center mt-2">
                    <span className="text-muted-foreground">Preço por hora</span>
                    <span>R$ {boat.pricePerHour.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</span>
                  </div>
                  <div className="flex justify-between items-center mt-4 pt-4 border-t border-border">
                    <span className="text-lg font-semibold">Total</span>
                    <span className="text-2xl font-bold text-primary">
                      R$ {totalPrice.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Payment form */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h2 className="text-xl font-semibold mb-6">Pagamento</h2>

              <div className="space-y-6">
                <div>
                  <h3 className="font-medium mb-4">Método de pagamento</h3>
                  <RadioGroup
                    value={paymentMethod}
                    onValueChange={(value: "CREDIT_CARD" | "PIX" | "BOLETO") => setPaymentMethod(value)}
                    className="grid grid-cols-3 gap-4"
                  >
                    <div>
                      <RadioGroupItem
                        value="CREDIT_CARD"
                        id="credit-card"
                        className="peer sr-only"
                      />
                      <label
                        htmlFor="credit-card"
                        className="flex flex-col items-center justify-between rounded-md border-2 border-muted bg-popover p-4 hover:bg-accent hover:text-accent-foreground peer-data-[state=checked]:border-primary [&:has([data-state=checked])]:border-primary cursor-pointer"
                      >
                        <CreditCard className="mb-2 h-6 w-6" />
                        <span>Cartão</span>
                      </label>
                    </div>
                    <div>
                      <RadioGroupItem
                        value="PIX"
                        id="pix"
                        className="peer sr-only"
                      />
                      <label
                        htmlFor="pix"
                        className="flex flex-col items-center justify-between rounded-md border-2 border-muted bg-popover p-4 hover:bg-accent hover:text-accent-foreground peer-data-[state=checked]:border-primary [&:has([data-state=checked])]:border-primary cursor-pointer"
                      >
                        <div className="mb-2 h-6 w-6 flex items-center justify-center">PIX</div>
                        <span>PIX</span>
                      </label>
                    </div>
                    <div>
                      <RadioGroupItem
                        value="BOLETO"
                        id="boleto"
                        className="peer sr-only"
                      />
                      <label
                        htmlFor="boleto"
                        className="flex flex-col items-center justify-between rounded-md border-2 border-muted bg-popover p-4 hover:bg-accent hover:text-accent-foreground peer-data-[state=checked]:border-primary [&:has([data-state=checked])]:border-primary cursor-pointer"
                      >
                        <div className="mb-2 h-6 w-6 flex items-center justify-center">BOL</div>
                        <span>Boleto</span>
                      </label>
                    </div>
                  </RadioGroup>
                </div>

                {paymentMethod === "CREDIT_CARD" && (
                  <div className="space-y-4">
                    <div className="grid md:grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="cardNumber">Número do cartão</Label>
                        <Input
                          id="cardNumber"
                          name="cardNumber"
                          placeholder="1234 5678 9012 3456"
                          value={formatCardNumber(cardData.cardNumber)}
                          onChange={(e) => setCardData(prev => ({ 
                            ...prev, 
                            cardNumber: e.target.value.replace(/\s/g, '') 
                          }))}
                          maxLength={19}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="holderName">Nome no cartão</Label>
                        <Input
                          id="holderName"
                          name="holderName"
                          placeholder="João da Silva"
                          value={cardData.holderName}
                          onChange={handleCardDataChange}
                        />
                      </div>
                    </div>
                    <div className="grid md:grid-cols-3 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="expirationDate">Validade (MM/AA)</Label>
                        <Input
                          id="expirationDate"
                          name="expirationDate"
                          placeholder="MM/AA"
                          value={cardData.expirationDate}
                          onChange={handleExpirationDateChange}
                          maxLength={5}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="cvv">CVV</Label>
                        <Input
                          id="cvv"
                          name="cvv"
                          placeholder="123"
                          value={cardData.cvv}
                          onChange={handleCardDataChange}
                          maxLength={4}
                          type="password"
                        />
                      </div>
                    </div>
                  </div>
                )}

                {paymentMethod === "PIX" && (
                  <div className="text-center p-6 border border-dashed border-border rounded-lg">
                    <p className="text-muted-foreground">
                      Após confirmar a reserva, você receberá um código PIX para pagamento.
                      A reserva será confirmada após a confirmação do pagamento.
                    </p>
                  </div>
                )}

                {paymentMethod === "BOLETO" && (
                  <div className="text-center p-6 border border-dashed border-border rounded-lg">
                    <p className="text-muted-foreground">
                      Após confirmar a reserva, você receberá um boleto bancário para pagamento.
                      A reserva será confirmada após a confirmação do pagamento.
                    </p>
                  </div>
                )}

                <div className="flex items-center gap-3 p-4 bg-primary/10 rounded-lg">
                  <Shield className="w-6 h-6 text-primary" />
                  <p className="text-sm text-muted-foreground">
                    Seus dados de pagamento são criptografados e processados com segurança.
                    Não armazenamos os dados do seu cartão.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Right-hand column: Order summary and confirmation button. */}
          <div className="lg:col-span-1">
            <div className="sticky top-24 bg-card rounded-2xl shadow-elevated p-6">
              <h2 className="text-xl font-semibold mb-6">Confirmar reserva</h2>

              <div className="space-y-4">
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Subtotal</span>
                    <span>R$ {totalPrice.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Taxas</span>
                    <span>R$ 0,00</span>
                  </div>
                  <div className="flex justify-between font-semibold text-lg pt-4 border-t border-border">
                    <span>Total</span>
                    <span className="text-primary">R$ {totalPrice.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</span>
                  </div>
                </div>

                <Button
                  variant="ocean"
                  size="lg"
                  className="w-full h-14 mt-6"
                  onClick={handleBooking}
                  disabled={processing}
                >
                  {processing ? (
                    <>
                      <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                      Processando...
                    </>
                  ) : (
                    "Confirmar reserva"
                  )}
                </Button>

                <p className="text-xs text-center text-muted-foreground mt-4">
                  Ao confirmar, você concorda com nossos{" "}
                  <Link href="/terms" className="underline hover:text-foreground">Termos de serviço</Link> e{" "}
                  <Link href="/privacy" className="underline hover:text-foreground">Política de privacidade</Link>.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
};

// Wrapper with Suspense for useSearchParams
const CheckoutPage = () => (
  <Suspense fallback={
    <div className="min-h-screen bg-background">
      <Navbar />
      <div className="pt-24 container mx-auto px-4">
        <div className="flex items-center justify-center py-16">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
          <span className="ml-3">Carregando checkout...</span>
        </div>
      </div>
      <Footer />
    </div>
  }>
    <CheckoutPageContent />
  </Suspense>
);

export default CheckoutPage;