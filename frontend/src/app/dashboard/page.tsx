"use client";
import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import Navbar from "@/src/components/Navbar";
import Footer from "@/src/components/Footer";
import { Button } from "@/src/components/ui/button";
import { Input } from "@/src/components/ui/input";
import { Label } from "@/src/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/src/components/ui/tabs";
import { Badge } from "@/src/components/ui/badge";
import {
  User,
  Anchor,
  Calendar,
  Heart,
  CreditCard,
  Bell,
  LogOut,
  MapPin,
  Star,
  Clock,
  CheckCircle,
  XCircle,
  Edit,
  Trash2,
  AlertCircle,
  Loader2,
  Plus,
} from "lucide-react";
import { cn } from "@/lib/utils";
import Link from 'next/link';
// API
import { bookingApi, authApi, boatApi, BookingResponse, BoatResponseDTO } from "@/src/lib/api";

const favorites = [
  {
    id: 1,
    nome: "Iate Sunseeker 68",
    localizacao: "Florianópolis, SC",
    preco: 5500,
    avaliacao: 4.9,
  },
  {
    id: 2,
    nome: "Escuna Pirata",
    localizacao: "Paraty, RJ",
    preco: 1200,
    avaliacao: 4.6,
  },
];

const statusConfig = {
  FINISHED: {
    label: "Concluído",
    icon: CheckCircle,
    className: "bg-accent/20 text-accent",
  },
  CONFIRMED: {
    label: "Confirmado",
    icon: Clock,
    className: "bg-primary/20 text-primary",
  },
  CANCELLED: {
    label: "Cancelado",
    icon: XCircle,
    className: "bg-destructive/20 text-destructive",
  },
  PENDING: {
    label: "Pendente",
    icon: Clock,
    className: "bg-yellow-500/20 text-yellow-600",
  },
};

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState("reservas");
  const [bookings, setBookings] = useState<BookingResponse[] | null>(null); // Changed to null for initial state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pagination, setPagination] = useState({
    page: 0,
    totalPages: 0,
    totalElements: 0,
  });
  const [myBoats, setMyBoats] = useState<BoatResponseDTO[]>([]);
  const [boatsLoading, setBoatsLoading] = useState(false);
  const [boatsError, setBoatsError] = useState<string | null>(null);
  const [boatsPagination, setBoatsPagination] = useState({
  page: 0,
  totalPages: 0,
  totalElements: 0,
  });

  const router = useRouter();

  // Format date from ISO string to Brazilian format
  const formatDate = (dateString: string): string => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('pt-BR');
    } catch (error) {
      console.error('Error formatting date:', error);
      return dateString;
    }
  };

  // Format location from address
  const formatLocation = (address: any): string => {
    if (!address) return 'Location not available';
    
    const { city, state } = address;
    if (city && state) {
      return `${city}, ${state}`;
    }
    return city || state || 'Location not available';
  };

  // Load bookings from API - wrapped in useCallback to prevent infinite re-renders
  const loadBookings = useCallback(async (page: number = 0) => {
    // Prevent execution during server-side rendering
    if (typeof window === 'undefined') return;
    if (activeTab !== "reservas") return;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await bookingApi.getMyBookings(page, 10);
      setBookings(response.content);
      setPagination({
        page: response.number,
        totalPages: response.totalPages,
        totalElements: response.totalElements,
      });
    } catch (err: any) {
      console.error('Error loading bookings:', err);
      
      if (err.status === 401) {
        // Token expired or invalid
        authApi.logout();
        router.push('/login');
        return;
      }
      
      setError(err.message || 'Error loading bookings. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [activeTab, router]);

  const loadMyBoats = useCallback(async (page: number = 0) => {
  if (typeof window === 'undefined') return;
  if (activeTab !== "meus-barcos") return;
  
  setBoatsLoading(true);
  setBoatsError(null);
  
  try {
    const response = await boatApi.getMyBoats(page, 10);
    setMyBoats(response.content);
    setBoatsPagination({
      page: response.number,
      totalPages: response.totalPages,
      totalElements: response.totalElements,
    });
  } catch (err: any) {
    console.error('Error loading boats:', err);
    
    if (err.status === 401) {
      authApi.logout();
      router.push('/login');
      return;
    }
    
    setBoatsError(err.message || 'Erro ao carregar barcos. Tente novamente.');
  } finally {
    setBoatsLoading(false);
  }
}, [activeTab, router]);

// Adicione funções de navegação de página para barcos:
const handleBoatsNextPage = () => {
  if (boatsPagination.page < boatsPagination.totalPages - 1) {
    loadMyBoats(boatsPagination.page + 1);
  }
};

const handleBoatsPrevPage = () => {
  if (boatsPagination.page > 0) {
    loadMyBoats(boatsPagination.page - 1);
  }
};

  // Function to switch tabs and update the hash in the URL
  const changeTab = (tab: string) => {
    setActiveTab(tab);
    // Updates the hash in the URL without reloading the page
    if (typeof window !== 'undefined') {
      window.history.replaceState(null, '', `#${tab}`);
    }
  };

  // Effect to read the URL hash when loading the component
  useEffect(() => {
    const handleHashChange = () => {
      const hash = window.location.hash.substring(1); // Remove "#"
      const validTabs = ["reservas", "meus-barcos", "favoritos", "perfil", "pagamentos", "notificacoes"];
      
      if (hash && validTabs.includes(hash)) {
        setActiveTab(hash);
      }
    };

    // Perform during initial mount
    handleHashChange();

    // Add listener for hash changes
    window.addEventListener('hashchange', handleHashChange);

    // Cleanup
    return () => {
      window.removeEventListener('hashchange', handleHashChange);
    };
  }, []);

  // Load data when tab changes
  useEffect(() => {
    if (activeTab === "reservas") {
      loadBookings();
    } else if (activeTab === "meus-barcos"){
      loadMyBoats();
    }
  }, [activeTab, loadBookings, loadMyBoats]);

  const handleLogout = () => {
    authApi.logout();
    router.push('/login');
  };

  const handleNextPage = () => {
    if (pagination.page < pagination.totalPages - 1) {
      loadBookings(pagination.page + 1);
    }
  };

  const handlePrevPage = () => {
    if (pagination.page > 0) {
      loadBookings(pagination.page - 1);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <main className="pt-24 pb-16">
        <div className="container mx-auto px-4">
          <div className="grid lg:grid-cols-4 gap-8">
            {/* Sidebar */}
            <aside className="lg:col-span-1">
              <div className="bg-card rounded-2xl shadow-card p-6 sticky top-24">
                {/* Profile */}
                <div className="text-center mb-6 pb-6 border-b border-border">
                  <div className="w-20 h-20 rounded-full gradient-ocean mx-auto mb-4 flex items-center justify-center text-2xl font-bold text-primary-foreground">
                    JD
                  </div>
                  <h2 className="font-display text-xl font-bold text-foreground">
                    João da Silva
                  </h2>
                  <p className="text-muted-foreground text-sm">
                    joao@email.com
                  </p>
                  <div className="flex items-center justify-center gap-1 mt-2">
                    <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                    <span className="text-sm font-medium">4.9</span>
                    <span className="text-sm text-muted-foreground">
                      (24 avaliações)
                    </span>
                  </div>
                </div>

                {/* Menu */}
                <nav className="space-y-2">
                  <button
                    onClick={() => changeTab("reservas")}
                    className={cn(
                      "w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors text-left",
                      activeTab === "reservas"
                        ? "bg-primary/10 text-primary"
                        : "text-muted-foreground hover:bg-muted"
                    )}
                  >
                    <Calendar className="w-5 h-5" />
                    Minhas Reservas
                    {bookings && bookings.length > 0 && (
                      <Badge className="ml-auto bg-primary text-primary-foreground">
                        {bookings.length}
                      </Badge>
                    )}
                  </button>
                  <button
                    onClick={() => changeTab("meus-barcos")}
                    className={cn(
                      "w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors text-left",
                      activeTab === "meus-barcos"
                        ? "bg-primary/10 text-primary"
                        : "text-muted-foreground hover:bg-muted"
                    )}
                  >
                    <Anchor className="w-5 h-5" />
                    Meus Barcos
                     {myBoats.length > 0 && (
                      <Badge className="ml-auto bg-primary text-primary-foreground">
                          {myBoats.length}
                      </Badge>
                     )}
                  </button>
                  <button
                    onClick={() => changeTab("favoritos")}
                    className={cn(
                      "w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors text-left",
                      activeTab === "favoritos"
                        ? "bg-primary/10 text-primary"
                        : "text-muted-foreground hover:bg-muted"
                    )}
                  >
                    <Heart className="w-5 h-5" />
                    Favoritos
                  </button>
                  <button
                    onClick={() => changeTab("perfil")}
                    className={cn(
                      "w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors text-left",
                      activeTab === "perfil"
                        ? "bg-primary/10 text-primary"
                        : "text-muted-foreground hover:bg-muted"
                    )}
                  >
                    <User className="w-5 h-5" />
                    Editar Perfil
                  </button>
                  <button
                    onClick={() => changeTab("pagamentos")}
                    className={cn(
                      "w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors text-left",
                      activeTab === "pagamentos"
                        ? "bg-primary/10 text-primary"
                        : "text-muted-foreground hover:bg-muted"
                    )}
                  >
                    <CreditCard className="w-5 h-5" />
                    Pagamentos
                  </button>
                  <button
                    onClick={() => changeTab("notificacoes")}
                    className={cn(
                      "w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors text-left",
                      activeTab === "notificacoes"
                        ? "bg-primary/10 text-primary"
                        : "text-muted-foreground hover:bg-muted"
                    )}
                  >
                    <Bell className="w-5 h-5" />
                    Notificações
                  </button>
                </nav>

                <div className="mt-6 pt-6 border-t border-border">
                  <button 
                    onClick={handleLogout}
                    className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-destructive hover:bg-destructive/10 transition-colors"
                  >
                    <LogOut className="w-5 h-5" />
                    Sair
                  </button>
                </div>
              </div>
            </aside>

            {/* Main Content */}
            <div className="lg:col-span-3">
              {/* Reservations */}
              {activeTab === "reservas" && (
                <div className="space-y-6 animate-fade-up">
                  <div className="flex items-center justify-between">
                    <h1 className="font-display text-2xl font-bold text-foreground">
                      Minhas Reservas
                    </h1>
                    <Link href="/search">
                      <Button variant="ocean">
                        <Plus className="w-4 h-4 mr-2" />
                        Nova Reserva
                      </Button>
                    </Link>
                  </div>

                  {loading ? (
                    <div className="flex flex-col items-center justify-center py-12">
                      <Loader2 className="w-12 h-12 text-primary animate-spin mb-4" />
                      <p className="text-muted-foreground">Carregando suas reservas...</p>
                    </div>
                  ) : error ? (
                    <div className="bg-destructive/10 border border-destructive/20 rounded-xl p-6 text-center">
                      <AlertCircle className="w-12 h-12 text-destructive mx-auto mb-4" />
                      <h3 className="font-semibold text-foreground mb-2">Erro ao carregar reservas</h3>
                      <p className="text-muted-foreground mb-4">{error}</p>
                      <Button variant="outline" onClick={() => loadBookings()}>
                        Tentar novamente
                      </Button>
                    </div>
                  ) : bookings === null ? (
                    // Initial state - not loaded yet
                    <div className="text-center py-12 text-muted-foreground">
                      Inicializando...
                    </div>
                  ) : bookings.length === 0 ? (
                    <div className="bg-card rounded-xl shadow-card p-8 text-center">
                      <Calendar className="w-16 h-16 text-muted-foreground mx-auto mb-4 opacity-50" />
                      <h3 className="font-semibold text-foreground mb-2">Nenhuma reserva encontrada</h3>
                      <p className="text-muted-foreground mb-6">
                        Você ainda não fez nenhuma reserva. Que tal explorar nossos barcos?
                      </p>
                      <Link href="/search">
                        <Button variant="ocean">Explorar Barcos</Button>
                      </Link>
                    </div>
                  ) : (
                    <>
                      <div className="space-y-4">
                        {bookings.map((booking) => {
                          const status = statusConfig[booking.status as keyof typeof statusConfig];
                          return (
                            <div
                              key={booking.id}
                              className="bg-card rounded-xl shadow-card p-4 flex flex-col md:flex-row gap-4"
                            >
                              {/* Image placeholder - update when BoatBasicDTO has imageUrl */}
                              <div className="w-full md:w-40 h-32 bg-linear-to-br from-gray-100 to-gray-200 rounded-lg flex items-center justify-center">
                                <Anchor className="w-12 h-12 text-gray-400" />
                              </div>
                              
                              <div className="flex-1">
                                <div className="flex items-start justify-between mb-2">
                                  <div>
                                    <h3 className="font-semibold text-foreground">
                                      {booking.boat.name}
                                    </h3>
                                    <div className="flex items-center gap-1 text-sm text-muted-foreground">
                                      <MapPin className="w-4 h-4" />
                                      {formatLocation(booking.boat.address)}
                                    </div>
                                  </div>
                                  <Badge
                                    className={cn(
                                      "flex items-center gap-1",
                                      status.className
                                    )}
                                  >
                                    <status.icon className="w-3 h-3" />
                                    {status.label}
                                  </Badge>
                                </div>
                                <div className="flex items-center gap-4 text-sm text-muted-foreground mb-3">
                                  <span>
                                    {formatDate(booking.startDate)} - {formatDate(booking.endDate)}
                                  </span>
                                </div>
                                <div className="flex items-center justify-between">
                                  <span className="font-bold text-primary">
                                    R$ {booking.totalPrice.toLocaleString('pt-BR', {
                                      minimumFractionDigits: 2,
                                      maximumFractionDigits: 2,
                                    })}
                                  </span>
                                  <div className="flex gap-2">
                                    {booking.status === "FINISHED" && (
                                      <Button variant="outline" size="sm">
                                        Avaliar
                                      </Button>
                                    )}
                                    <Button variant="outline" size="sm">
                                      Ver detalhes
                                    </Button>
                                  </div>
                                </div>
                              </div>
                            </div>
                          );
                        })}
                      </div>

                      {/* Pagination */}
                      {pagination.totalPages > 1 && (
                        <div className="flex items-center justify-between pt-6 border-t border-border">
                          <div className="text-sm text-muted-foreground">
                            Mostrando {bookings.length} de {pagination.totalElements} reservas
                          </div>
                          <div className="flex items-center gap-2">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={handlePrevPage}
                              disabled={pagination.page === 0 || loading}
                            >
                              Anterior
                            </Button>
                            <span className="text-sm text-foreground">
                              Página {pagination.page + 1} de {pagination.totalPages}
                            </span>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={handleNextPage}
                              disabled={pagination.page >= pagination.totalPages - 1 || loading}
                            >
                              Próxima
                            </Button>
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}

              {/* My Boats */}
              {activeTab === "meus-barcos" && (
              <div className="space-y-6 animate-fade-up">
                <div className="flex items-center justify-between">
                  <h1 className="font-display text-2xl font-bold text-foreground">
                    Meus Barcos
                  </h1>
                  <Link href="/register-boat">
                    <Button variant="ocean">
                      <Plus className="w-4 h-4 mr-2" />
                      Adicionar Barco
                    </Button>
                  </Link>
                </div>
                          
                {boatsLoading ? (
                  <div className="flex flex-col items-center justify-center py-12">
                    <Loader2 className="w-12 h-12 text-primary animate-spin mb-4" />
                    <p className="text-muted-foreground">Carregando seus barcos...</p>
                  </div>
                ) : boatsError ? (
                  <div className="bg-destructive/10 border border-destructive/20 rounded-xl p-6 text-center">
                    <AlertCircle className="w-12 h-12 text-destructive mx-auto mb-4" />
                    <h3 className="font-semibold text-foreground mb-2">Erro ao carregar barcos</h3>
                    <p className="text-muted-foreground mb-4">{boatsError}</p>
                    <Button variant="outline" onClick={() => loadMyBoats()}>
                      Tentar novamente
                    </Button>
                  </div>
                ) : myBoats.length === 0 ? (
                  <div className="bg-card rounded-xl shadow-card p-8 text-center">
                    <Anchor className="w-16 h-16 text-muted-foreground mx-auto mb-4 opacity-50" />
                    <h3 className="font-semibold text-foreground mb-2">Nenhum barco cadastrado</h3>
                    <p className="text-muted-foreground mb-6">
                      Você ainda não cadastrou nenhum barco. Que tal oferecer seu barco para aluguel?
                    </p>
                    <Link href="/register-boat">
                      <Button variant="ocean">Cadastrar Barco</Button>
                    </Link>
                  </div>
                ) : (
                  <div className="grid md:grid-cols-2 gap-6">
                    {myBoats.map((barco) => (
                      <div
                        key={barco.id}
                        className="bg-card rounded-xl shadow-card overflow-hidden"
                      >
                        <div className="relative aspect-video">
                          {barco.photos && barco.photos.length > 0 ? (
                            <img
                              src={barco.photos[0]}
                              alt={barco.name}
                              className="w-full h-full object-cover"
                            />
                          ) : (
                            <div className="w-full h-full bg-linear-to-br from-gray-100 to-gray-200 flex items-center justify-center">
                              <Anchor className="w-16 h-16 text-gray-400" />
                            </div>
                          )}
                          <Badge className="absolute top-3 right-3 bg-accent text-accent-foreground">
                            Ativo
                          </Badge>
                        </div>
                        <div className="p-5">
                          <h3 className="font-display text-lg font-semibold text-foreground mb-2">
                            {barco.name}
                          </h3>
                          <div className="flex items-center gap-1 text-sm text-muted-foreground mb-3">
                            <MapPin className="w-4 h-4" />
                            {barco.city && barco.state ? `${barco.city}, ${barco.state}` : barco.marina || 'Localização não disponível'}
                          </div>
                          <div className="flex items-center gap-4 text-sm mb-4">
                            <div className="flex items-center gap-1">
                              <User className="w-4 h-4" />
                              <span>Capacidade: {barco.capacity} pessoas</span>
                            </div>
                            <span className="text-muted-foreground">
                              {barco.type}
                            </span>
                          </div>
                          <div className="flex items-center justify-between pt-4 border-t border-border">
                            <span className="font-bold text-primary">
                              R$ {barco.pricePerHour.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}/hora
                            </span>
                            <div className="flex gap-2">
                              <Link href={`/boats/edit/${barco.id}`}>
                                <Button variant="outline" size="icon">
                                  <Edit className="w-4 h-4" />
                                </Button>
                              </Link>
                              <Button
                                variant="outline"
                                size="icon"
                                className="text-destructive hover:text-destructive"
                                onClick={() => {
                                  // TODO: Implementar deleção com confirmação
                                  console.log('Deletar barco:', barco.id);
                                }}
                              >
                                <Trash2 className="w-4 h-4" />
                              </Button>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
                {boatsPagination.totalPages > 1 && (
                <div className="flex items-center justify-between pt-6 border-t border-border">
                  <div className="text-sm text-muted-foreground">
                    Mostrando {myBoats.length} de {boatsPagination.totalElements} barcos
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleBoatsPrevPage}
                      disabled={boatsPagination.page === 0 || boatsLoading}
                    >
                      Anterior
                    </Button>
                    <span className="text-sm text-foreground">
                      Página {boatsPagination.page + 1} de {boatsPagination.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleBoatsNextPage}
                      disabled={boatsPagination.page >= boatsPagination.totalPages - 1 || boatsLoading}
                    >
                      Próxima
                    </Button>
                  </div>
                </div>
              )}
              </div>
            )}

              {/* Favorites */}
              {activeTab === "favoritos" && (
                <div className="space-y-6 animate-fade-up">
                  <h1 className="font-display text-2xl font-bold text-foreground">
                    Favoritos
                  </h1>

                  <div className="grid md:grid-cols-2 gap-6">
                    {favorites.map((barco) => (
                      <Link
                        key={barco.id}
                        href={`/barco/${barco.id}`}
                        className="group bg-card rounded-xl shadow-card overflow-hidden hover:shadow-elevated transition-all"
                      >
                        <div className="relative aspect-video">
                          <div className="w-full h-full bg-linear-to-br from-gray-100 to-gray-200 flex items-center justify-center">
                            <Anchor className="w-16 h-16 text-gray-400" />
                          </div>
                          <button
                            className="absolute top-3 right-3 w-10 h-10 bg-card/90 rounded-full flex items-center justify-center"
                            onClick={(e) => {
                              e.preventDefault();
                              // TODO: Remove from favorites
                            }}
                          >
                            <Heart className="w-5 h-5 fill-destructive text-destructive" />
                          </button>
                        </div>
                        <div className="p-5">
                          <h3 className="font-display text-lg font-semibold text-foreground mb-2 group-hover:text-primary transition-colors">
                            {barco.nome}
                          </h3>
                          <div className="flex items-center gap-1 text-sm text-muted-foreground mb-3">
                            <MapPin className="w-4 h-4" />
                            {barco.localizacao}
                          </div>
                          <div className="flex items-center justify-between">
                            <span className="font-bold text-primary">
                              R$ {barco.preco.toLocaleString()}/dia
                            </span>
                            <div className="flex items-center gap-1">
                              <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                              <span className="text-sm">{barco.avaliacao}</span>
                            </div>
                          </div>
                        </div>
                      </Link>
                    ))}
                  </div>
                </div>
              )}

              {/* Profile */}
              {activeTab === "perfil" && (
                <div className="space-y-6 animate-fade-up">
                  <h1 className="font-display text-2xl font-bold text-foreground">
                    Editar Perfil
                  </h1>

                  <div className="bg-card rounded-xl shadow-card p-6">
                    <div className="space-y-6">
                      <div className="flex items-center gap-6">
                        <div className="w-24 h-24 rounded-full gradient-ocean flex items-center justify-center text-3xl font-bold text-primary-foreground">
                          JD
                        </div>
                        <div>
                          <Button variant="outline" size="sm">
                            Alterar foto
                          </Button>
                          <p className="text-sm text-muted-foreground mt-2">
                            JPG, PNG. Máximo 5MB.
                          </p>
                        </div>
                      </div>

                      <div className="grid md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label htmlFor="nome">Nome completo</Label>
                          <Input id="nome" defaultValue="João da Silva" />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="email">E-mail</Label>
                          <Input
                            id="email"
                            type="email"
                            defaultValue="joao@email.com"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="telefone">Telefone</Label>
                          <Input
                            id="telefone"
                            defaultValue="(21) 99999-9999"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="cpf">CPF</Label>
                          <Input id="cpf" defaultValue="123.456.789-00" />
                        </div>
                      </div>

                      <div className="flex justify-end gap-3 pt-4 border-t border-border">
                        <Button variant="outline">Cancelar</Button>
                        <Button variant="ocean">Salvar alterações</Button>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Payments */}
              {activeTab === "pagamentos" && (
                <div className="space-y-6 animate-fade-up">
                  <h1 className="font-display text-2xl font-bold text-foreground">
                    Pagamentos
                  </h1>

                  <div className="bg-card rounded-xl shadow-card p-6">
                    <h3 className="font-semibold text-foreground mb-4">
                      Métodos de pagamento
                    </h3>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between p-4 border border-border rounded-lg">
                        <div className="flex items-center gap-3">
                          <CreditCard className="w-8 h-8 text-primary" />
                          <div>
                            <p className="font-medium text-foreground">
                              Visa •••• 4242
                            </p>
                            <p className="text-sm text-muted-foreground">
                              Expira em 12/25
                            </p>
                          </div>
                        </div>
                        <Badge>Padrão</Badge>
                      </div>
                    </div>
                    <Button variant="outline" className="mt-4">
                      Adicionar novo cartão
                    </Button>
                  </div>
                </div>
              )}

              {/* Notifications */}
              {activeTab === "notificacoes" && (
                <div className="space-y-6 animate-fade-up">
                  <h1 className="font-display text-2xl font-bold text-foreground">
                    Notificações
                  </h1>

                  <div className="bg-card rounded-xl shadow-card p-6">
                    <div className="space-y-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="font-medium text-foreground">
                            E-mails de reservas
                          </p>
                          <p className="text-sm text-muted-foreground">
                            Receba atualizações sobre suas reservas
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          defaultChecked
                          className="w-5 h-5"
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="font-medium text-foreground">
                            Promoções e ofertas
                          </p>
                          <p className="text-sm text-muted-foreground">
                            Fique por dentro das melhores ofertas
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          defaultChecked
                          className="w-5 h-5"
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="font-medium text-foreground">
                            Lembretes de viagem
                          </p>
                          <p className="text-sm text-muted-foreground">
                            Lembretes antes das suas viagens
                          </p>
                        </div>
                        <input
                          type="checkbox"
                          defaultChecked
                          className="w-5 h-5"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};

export default Dashboard;