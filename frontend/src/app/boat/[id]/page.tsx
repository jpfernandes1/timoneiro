"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Navbar from "@/src/components/Navbar";
import Footer from "@/src/components/Footer";
import { Button } from "@/src/components/ui/button";
import { Label } from "@/src/components/ui/label";
import {
  Calendar,
  MapPin,
  Users,
  Anchor,
  Star,
  Heart,
  Share2,
  Clock,
  Ruler,
  Zap,
  CalendarDays,
  CheckCircle,
  ArrowLeft,
  Phone,
  Mail,
  Shield,
  CreditCard,
  ChevronLeft,
  ChevronRight,
  Image as ImageIcon,
} from "lucide-react";
import { cn } from "@/src/lib/utils";
import Link from "next/link";

// Interfaces/types
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

interface Review {
  id: number;
  user: string;
  rating: number;
  date: string;
  comment: string;
  avatar: string;
}

const BoatDetails = () => {
  const params = useParams();
  const router = useRouter();
  const boatId = params.id as string;

  // Estados
  const [boat, setBoat] = useState<Boat | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  const [isFavorite, setIsFavorite] = useState(false);
  const [showAllAmenities, setShowAllAmenities] = useState(false);
  
  // Novos estados para data fim e horas
  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");
  const [startTime, setStartTime] = useState<string>("09:00");
  const [endTime, setEndTime] = useState<string>("17:00");
  const [durationError, setDurationError] = useState<string>("");
  const [durationHours, setDurationHours] = useState<number>(8);

  // Reviews mockados (substituir por dados do backend quando implementado)
  const [reviews] = useState<Review[]>([
    {
      id: 1,
      user: "João Silva",
      rating: 5,
      date: "15/12/2024",
      comment: "Experiência incrível! O barco está impecável e a equipe foi muito atenciosa. Recomendo!",
      avatar: "JS",
    },
    {
      id: 2,
      user: "Maria Santos",
      rating: 4,
      date: "10/12/2024",
      comment: "Barco muito confortável e bem equipado. Passeio perfeito para a família.",
      avatar: "MS",
    },
    {
      id: 3,
      user: "Carlos Oliveira",
      rating: 5,
      date: "05/12/2024",
      comment: "Melhor passeio de barco que já fiz. Superou todas as expectativas!",
      avatar: "CO",
    },
  ]);

  // Buscar dados do barco
  useEffect(() => {
    const fetchBoatDetails = async () => {
      try {
        setLoading(true);
        const response = await fetch(`http://localhost:8080/api/boats/${boatId}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
        });

        if (!response.ok) {
          if (response.status === 404) {
            throw new Error('Barco não encontrado');
          }
          throw new Error(`Erro ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();
        console.log('📦 Dados do barco:', data);
        setBoat(data);
      } catch (err) {
        console.error('❌ Erro ao buscar detalhes do barco:', err);
        setError(err instanceof Error ? err.message : 'Erro desconhecido');
      } finally {
        setLoading(false);
      }
    };

    if (boatId) {
      fetchBoatDetails();
    }
  }, [boatId]);

  // Inicializar datas uma vez quando o componente montar
  useEffect(() => {
    const today = new Date().toISOString().split('T')[0];
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];
    
    setStartDate(today);
    setEndDate(tomorrowStr);
  }, []);

  // Calcular duração em horas
  const calculateDuration = useCallback(() => {
    if (!startDate || !endDate || !startTime || !endTime) return 0;
    
    const startDateTime = new Date(`${startDate}T${startTime}`);
    const endDateTime = new Date(`${endDate}T${endTime}`);
    
    // Validar se a data/hora de fim é depois da data/hora de início
    if (endDateTime <= startDateTime) {
      setDurationError("A data/hora de fim deve ser posterior à data/hora de início");
      return 0;
    }
    
    setDurationError("");
    const durationMs = endDateTime.getTime() - startDateTime.getTime();
    const durationHours = Math.ceil(durationMs / (1000 * 60 * 60));
    return durationHours;
  }, [startDate, endDate, startTime, endTime]);

  // Atualizar duração quando datas/horas mudarem
  useEffect(() => {
    const newDuration = calculateDuration();
    setDurationHours(newDuration);
  }, [startDate, endDate, startTime, endTime, calculateDuration]);

  // Navegação de imagens
  const nextImage = () => {
    if (boat && boat.photos.length > 0) {
      setSelectedImageIndex((prev) => (prev + 1) % boat.photos.length);
    }
  };

  const prevImage = () => {
    if (boat && boat.photos.length > 0) {
      setSelectedImageIndex((prev) => (prev - 1 + boat.photos.length) % boat.photos.length);
    }
  };

  // Calcular preço total
  const calculateTotal = useCallback(() => {
    if (!boat) return 0;
    return boat.pricePerHour * durationHours;
  }, [boat, durationHours]);

  const handleReservation = () => {
    if (durationHours <= 0) {
      alert("Por favor, selecione datas e horários válidos para a reserva.");
      return;
    }
    
    const total = calculateTotal();
    
    // Preparar dados da reserva
    const reservationData = {
      boatId: boat?.id,
      boatName: boat?.name,
      startDate: startDate,
      startTime: startTime,
      endDate: endDate,
      endTime: endTime,
      durationHours: durationHours,
      totalPrice: total,
      pricePerHour: boat?.pricePerHour
    };
    
    console.log("📋 Dados da reserva:", reservationData);
    
    // TODO: Implementar lógica de reserva com API
    alert(`Reserva iniciada para ${boat?.name}\nDe: ${startDate} ${startTime}\nAté: ${endDate} ${endTime}\nDuração: ${durationHours} horas\nTotal: R$ ${total.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`);
  };

  // Comodidades agrupadas por categoria
  const amenitiesByCategory = {
    "Conforto": ["Ar condicionado", "Churrasqueira", "Som ambiente", "Wi-Fi"],
    "Segurança": ["Coletes salva-vidas", "Extintor de incêndio", "GPS", "Rádio VHF"],
    "Lazer": ["Equipamentos de mergulho", "Stand Up Paddle", "Pesca esportiva", "Tábua de snorkel"],
    "Cozinha": ["Fogão", "Geladeira", "Micro-ondas", "Utensílios de cozinha"],
    "Cabines": ["Cabine principal", "Cabine de hóspedes", "Banheiro", "Chuveiro"],
  };

  // Renderização condicional
  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="pt-24 container mx-auto px-4">
          <div className="animate-pulse space-y-6">
            {/* Skeletons */}
            <div className="h-8 bg-gray-200 rounded w-1/4 mb-8"></div>
            <div className="grid lg:grid-cols-2 gap-8">
              <div className="space-y-4">
                <div className="bg-gray-200 rounded-xl aspect-[4/3]"></div>
                <div className="grid grid-cols-4 gap-2">
                  {[...Array(4)].map((_, i) => (
                    <div key={i} className="bg-gray-200 rounded aspect-square"></div>
                  ))}
                </div>
              </div>
              <div className="space-y-4">
                <div className="h-8 bg-gray-200 rounded w-3/4"></div>
                <div className="h-4 bg-gray-200 rounded w-1/2"></div>
                <div className="h-24 bg-gray-200 rounded"></div>
              </div>
            </div>
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
            <div className="text-6xl mb-4">🚤</div>
            <h1 className="font-display text-2xl font-bold text-foreground mb-4">
              {error || "Barco não encontrado"}
            </h1>
            <p className="text-muted-foreground mb-8">
              O barco que você está procurando não está disponível ou não existe.
            </p>
            <div className="flex gap-4 justify-center">
              <Button variant="outline" onClick={() => router.back()}>
                <ArrowLeft className="w-4 h-4 mr-2" />
                Voltar
              </Button>
              <Link href="/search">
                <Button variant="ocean">Explorar barcos</Button>
              </Link>
            </div>
          </div>
        </div>
        <Footer />
      </div>
    );
  }

  // Calcular média de avaliações
  const averageRating = reviews.reduce((acc, review) => acc + review.rating, 0) / reviews.length;
  const total = calculateTotal();

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      {/* Breadcrumb */}
      <div className="container mx-auto px-4 pt-24">
        <div className="flex items-center gap-2 text-sm text-muted-foreground mb-6">
          <Link href="/" className="hover:text-foreground transition-colors">
            Home
          </Link>
          <span>/</span>
          <Link href="/search" className="hover:text-foreground transition-colors">
            Barcos
          </Link>
          <span>/</span>
          <span className="text-foreground font-medium truncate">{boat.name}</span>
        </div>
      </div>

      {/* Main Content */}
      <main className="container mx-auto px-4 pb-16">
        <div className="grid lg:grid-cols-2 gap-8">
          {/* Left Column - Images */}
          <div className="space-y-4">
            {/* Main Image */}
            <div className="relative rounded-2xl overflow-hidden bg-gradient-to-br from-gray-100 to-gray-200 aspect-[4/3]">
              {boat.photos && boat.photos.length > 0 ? (
                <>
                  <img
                    src={boat.photos[selectedImageIndex]}
                    alt={`${boat.name} - Foto ${selectedImageIndex + 1}`}
                    className="w-full h-full object-cover"
                  />
                  {/* Navigation Arrows */}
                  {boat.photos.length > 1 && (
                    <>
                      <button
                        onClick={prevImage}
                        className="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 bg-card/90 backdrop-blur-sm rounded-full flex items-center justify-center hover:bg-card transition-colors"
                      >
                        <ChevronLeft className="w-5 h-5 text-foreground" />
                      </button>
                      <button
                        onClick={nextImage}
                        className="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 bg-card/90 backdrop-blur-sm rounded-full flex items-center justify-center hover:bg-card transition-colors"
                      >
                        <ChevronRight className="w-5 h-5 text-foreground" />
                      </button>
                    </>
                  )}
                  {/* Image Counter */}
                  <div className="absolute bottom-4 left-1/2 -translate-x-1/2 bg-card/90 backdrop-blur-sm px-3 py-1 rounded-full">
                    <span className="text-sm font-medium text-foreground">
                      {selectedImageIndex + 1} / {boat.photos.length}
                    </span>
                  </div>
                </>
              ) : (
                <div className="w-full h-full flex flex-col items-center justify-center p-8">
                  <ImageIcon className="w-16 h-16 text-gray-400 mb-4" />
                  <p className="text-gray-500 text-center">Sem fotos disponíveis</p>
                </div>
              )}
            </div>

            {/* Thumbnails */}
            {boat.photos && boat.photos.length > 1 && (
              <div className="grid grid-cols-4 gap-2">
                {boat.photos.map((photo, index) => (
                  <button
                    key={index}
                    onClick={() => setSelectedImageIndex(index)}
                    className={cn(
                      "relative rounded-lg overflow-hidden aspect-square",
                      selectedImageIndex === index && "ring-2 ring-primary ring-offset-2"
                    )}
                  >
                    <img
                      src={photo}
                      alt={`Thumbnail ${index + 1}`}
                      className="w-full h-full object-cover"
                    />
                  </button>
                ))}
              </div>
            )}

            {/* Owner Info */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h3 className="font-semibold text-foreground mb-4">Sobre o proprietário</h3>
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-full gradient-ocean flex items-center justify-center">
                  <span className="text-xl font-bold text-primary-foreground">
                    {boat.ownerName?.charAt(0) || "P"}
                  </span>
                </div>
                <div className="flex-1">
                  <h4 className="font-medium text-foreground">{boat.ownerName || "Proprietário"}</h4>
                  <p className="text-sm text-muted-foreground">Proprietário na Timoneiro</p>
                  <div className="flex items-center gap-2 mt-2">
                    <div className="flex items-center gap-1">
                      <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                      <span className="text-sm font-medium">5.0</span>
                    </div>
                    <span className="text-sm text-muted-foreground">•</span>
                    <span className="text-sm text-muted-foreground">10 barcos</span>
                  </div>
                </div>
              </div>
              <div className="flex gap-3 mt-6">
                <Button variant="outline" size="sm" className="flex-1">
                  <Phone className="w-4 h-4 mr-2" />
                  Contato
                </Button>
                <Button variant="outline" size="sm" className="flex-1">
                  <Mail className="w-4 h-4 mr-2" />
                  Mensagem
                </Button>
              </div>
            </div>
          </div>

          {/* Right Column - Details */}
          <div className="space-y-8">
            {/* Header */}
            <div>
              <div className="flex items-start justify-between mb-4">
                <div>
                  <h1 className="font-display text-3xl font-bold text-foreground mb-2">
                    {boat.name}
                  </h1>
                  <div className="flex items-center gap-4 text-muted-foreground">
                    <div className="flex items-center gap-2">
                      <MapPin className="w-4 h-4" />
                      <span>{boat.city}, {boat.state}</span>
                      {boat.marina && <span>• {boat.marina}</span>}
                    </div>
                    <div className="flex items-center gap-2">
                      <Anchor className="w-4 h-4" />
                      <span>{boat.type}</span>
                    </div>
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => setIsFavorite(!isFavorite)}
                  >
                    <Heart className={cn("w-5 h-5", isFavorite && "fill-red-500 text-red-500")} />
                  </Button>
                  <Button variant="outline" size="icon">
                    <Share2 className="w-5 h-5" />
                  </Button>
                </div>
              </div>

              {/* Rating */}
              <div className="flex items-center gap-4 mb-6">
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-1">
                    {[...Array(5)].map((_, i) => (
                      <Star
                        key={i}
                        className={cn(
                          "w-5 h-5",
                          i < Math.floor(averageRating)
                            ? "fill-yellow-400 text-yellow-400"
                            : "fill-gray-200 text-gray-200"
                        )}
                      />
                    ))}
                  </div>
                  <span className="font-semibold text-foreground">{averageRating.toFixed(1)}</span>
                </div>
                <span className="text-muted-foreground">•</span>
                <span className="text-muted-foreground">{reviews.length} avaliações</span>
                <span className="text-muted-foreground">•</span>
                <span className="text-muted-foreground">{boat.fabrication}</span>
              </div>
            </div>

            {/* Specifications */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h3 className="font-semibold text-foreground mb-6">Especificações técnicas</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="text-center">
                  <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center mx-auto mb-3">
                    <Users className="w-6 h-6 text-primary-foreground" />
                  </div>
                  <div className="text-foreground font-semibold">{boat.capacity}</div>
                  <div className="text-sm text-muted-foreground">Pessoas</div>
                </div>
                <div className="text-center">
                  <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center mx-auto mb-3">
                    <Ruler className="w-6 h-6 text-primary-foreground" />
                  </div>
                  <div className="text-foreground font-semibold">{boat.length}m</div>
                  <div className="text-sm text-muted-foreground">Comprimento</div>
                </div>
                <div className="text-center">
                  <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center mx-auto mb-3">
                    <Zap className="w-6 h-6 text-primary-foreground" />
                  </div>
                  <div className="text-foreground font-semibold">{boat.speed}</div>
                  <div className="text-sm text-muted-foreground">Nós</div>
                </div>
                <div className="text-center">
                  <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center mx-auto mb-3">
                    <CalendarDays className="w-6 h-6 text-primary-foreground" />
                  </div>
                  <div className="text-foreground font-semibold">{boat.fabrication}</div>
                  <div className="text-sm text-muted-foreground">Ano</div>
                </div>
              </div>
            </div>

            {/* Description */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h3 className="font-semibold text-foreground mb-4">Descrição</h3>
              <p className="text-foreground/80 leading-relaxed whitespace-pre-line">
                {boat.description || "Este barco não possui descrição detalhada."}
              </p>
            </div>

            {/* Amenities */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h3 className="font-semibold text-foreground mb-6">Comodidades e equipamentos</h3>
              <div className="space-y-6">
                {Object.entries(amenitiesByCategory).map(([category, items]) => (
                  <div key={category}>
                    <h4 className="font-medium text-foreground mb-3">{category}</h4>
                    <div className="grid grid-cols-2 gap-2">
                      {items.map((item) => {
                        const hasAmenity = boat.amenities?.some(amenity => 
                          amenity.toLowerCase().includes(item.toLowerCase())
                        );
                        
                        return (
                          <div key={item} className="flex items-center gap-3">
                            <CheckCircle className={cn(
                              "w-5 h-5",
                              hasAmenity 
                                ? "text-green-500" 
                                : "text-gray-300"
                            )} />
                            <span className={cn(
                              "text-sm",
                              hasAmenity 
                                ? "text-foreground" 
                                : "text-muted-foreground"
                            )}>
                              {item}
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
              
              {boat.amenities && boat.amenities.length > 0 && (
                <div className="mt-6 pt-6 border-t border-border">
                  <h4 className="font-medium text-foreground mb-3">Outras comodidades</h4>
                  <div className="flex flex-wrap gap-2">
                    {boat.amenities.map((amenity, index) => (
                      <span 
                        key={index} 
                        className="bg-gray-100 text-gray-700 px-3 py-1 rounded-full text-sm"
                      >
                        {amenity}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Reviews */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <div className="flex items-center justify-between mb-6">
                <h3 className="font-semibold text-foreground">Avaliações</h3>
                <Button variant="outline" size="sm">Ver todas</Button>
              </div>
              
              <div className="space-y-6">
                {reviews.map((review) => (
                  <div key={review.id} className="pb-6 border-b border-border last:border-0 last:pb-0">
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                          <span className="font-medium text-primary">{review.avatar}</span>
                        </div>
                        <div>
                          <h4 className="font-medium text-foreground">{review.user}</h4>
                          <div className="flex items-center gap-1">
                            {[...Array(5)].map((_, i) => (
                              <Star
                                key={i}
                                className={cn(
                                  "w-4 h-4",
                                  i < review.rating
                                    ? "fill-yellow-400 text-yellow-400"
                                    : "fill-gray-200 text-gray-200"
                                )}
                              />
                            ))}
                          </div>
                        </div>
                      </div>
                      <span className="text-sm text-muted-foreground">{review.date}</span>
                    </div>
                    <p className="text-foreground/80">{review.comment}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Booking Panel - Fixed Bottom on Mobile */}
        <div className="lg:hidden fixed bottom-0 left-0 right-0 bg-card border-t border-border p-4 shadow-lg z-40">
          <div className="container mx-auto">
            <div className="flex items-center justify-between">
              <div>
                <div className="text-2xl font-bold text-primary">
                  R$ {boat.pricePerHour.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                </div>
                <div className="text-sm text-muted-foreground">por hora</div>
              </div>
              <Button variant="ocean" size="lg" onClick={handleReservation}>
                Reservar
              </Button>
            </div>
          </div>
        </div>

        {/* Booking Sidebar - Desktop */}
        <div className="lg:col-span-2">
          <div className="sticky top-24 mt-8 bg-card rounded-2xl shadow-elevated p-6">
            <h3 className="font-display text-xl font-bold text-foreground mb-6">Reservar este barco</h3>
            
            <div className="grid lg:grid-cols-3 gap-6">
              {/* Coluna 1: Data Início e Hora Início */}
              <div className="space-y-4">
                {/* Data Início */}
                <div className="space-y-2">
                  <Label htmlFor="start-date">Data início</Label>
                  <div>
                    <input
                      type="date"
                      id="start-date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      className="w-full p-3 border border-border rounded-lg bg-background"
                      min={new Date().toISOString().split('T')[0]}
                    />
                  </div>
                </div>

                {/* Hora Início */}
                <div className="space-y-2">
                  <Label htmlFor="start-time">Hora início</Label>
                  <div>
                    <input
                      type="time"
                      id="start-time"
                      value={startTime}
                      onChange={(e) => setStartTime(e.target.value)}
                      className="w-full p-3 border border-border rounded-lg bg-background"
                    />
                  </div>
                </div>
              </div>

              {/* Coluna 2: Data Fim e Hora Fim */}
              <div className="space-y-4">
                {/* Data Fim */}
                <div className="space-y-2">
                  <Label htmlFor="end-date">Data fim</Label>
                  <div>
                    <input
                      type="date"
                      id="end-date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      className="w-full p-3 border border-border rounded-lg bg-background"
                      min={startDate || new Date().toISOString().split('T')[0]}
                    />
                  </div>
                </div>

                {/* Hora Fim */}
                <div className="space-y-2">
                  <Label htmlFor="end-time">Hora fim</Label>
                  <div>
                    <input
                      type="time"
                      id="end-time"
                      value={endTime}
                      onChange={(e) => setEndTime(e.target.value)}
                      className="w-full p-3 border border-border rounded-lg bg-background"
                    />
                  </div>
                </div>
              </div>

              {/* Coluna 3: Resumo do Preço */}
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label>Resumo da reserva</Label>
                  <div className="p-4 border border-border rounded-lg space-y-3">
                    <div className="text-2xl font-bold text-primary">
                      R$ {total.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                    </div>
                    <div className="text-sm text-muted-foreground space-y-1">
                      <div className="flex justify-between">
                        <span>Duração:</span>
                        <span className="font-medium">{durationHours} horas</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Preço/hora:</span>
                        <span>R$ {boat.pricePerHour.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</span>
                      </div>
                    </div>
                    {durationError && (
                      <div className="text-sm text-red-500 mt-2">{durationError}</div>
                    )}
                  </div>
                </div>

                {/* Botão de Reserva */}
                <div className="pt-2">
                  <Button 
                    variant="ocean" 
                    size="lg" 
                    className="w-full h-14" 
                    onClick={handleReservation}
                    disabled={durationHours <= 0}
                  >
                    <Calendar className="w-5 h-5 mr-2" />
                    Reservar agora
                  </Button>
                </div>
              </div>
            </div>

            {/* Safety Info */}
            <div className="mt-6 pt-6 border-t border-border">
              <div className="flex items-center gap-3 text-sm text-muted-foreground">
                <Shield className="w-5 h-5" />
                <span>Reserva segura com Timoneiro</span>
                <span>•</span>
                <CreditCard className="w-5 h-5" />
                <span>Pagamento 100% seguro</span>
              </div>
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};

export default BoatDetails;