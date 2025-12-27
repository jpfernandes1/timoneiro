"use client";
import { useState, useEffect } from "react";
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
  Settings,
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
} from "lucide-react";
import { cn } from "@/lib/utils";
import Link from 'next/link';
import boat1 from "@/src/assets/boat-1.jpg";
import boat2 from "@/src/assets/boat-2.jpg";
import boat3 from "@/src/assets/boat-3.jpg";

const historicoReservas = [
  {
    id: 1,
    barco: "Lancha Azimut 55",
    imagem: boat1,
    localizacao: "Angra dos Reis, RJ",
    dataInicio: "15/11/2024",
    dataFim: "16/11/2024",
    valor: 2500,
    status: "concluido",
  },
  {
    id: 2,
    barco: "Veleiro Beneteau 40",
    imagem: boat2,
    localizacao: "Ilhabela, SP",
    dataInicio: "28/12/2024",
    dataFim: "30/12/2024",
    valor: 5400,
    status: "confirmado",
  },
  {
    id: 3,
    barco: "Catamarã Lagoon 42",
    imagem: boat3,
    localizacao: "Búzios, RJ",
    dataInicio: "05/10/2024",
    dataFim: "05/10/2024",
    valor: 3200,
    status: "cancelado",
  },
];

const meusBarcos = [
  {
    id: 1,
    nome: "Lancha Ferretti 45",
    imagem: boat1,
    localizacao: "Guarujá, SP",
    preco: 2100,
    avaliacao: 4.8,
    reservas: 12,
    status: "ativo",
  },
  {
    id: 2,
    nome: "Veleiro Bavaria 38",
    imagem: boat2,
    localizacao: "Ubatuba, SP",
    preco: 1500,
    avaliacao: 4.6,
    reservas: 8,
    status: "ativo",
  },
];

const favoritos = [
  {
    id: 1,
    nome: "Iate Sunseeker 68",
    imagem: boat1,
    localizacao: "Florianópolis, SC",
    preco: 5500,
    avaliacao: 4.9,
  },
  {
    id: 2,
    nome: "Escuna Pirata",
    imagem: boat3,
    localizacao: "Paraty, RJ",
    preco: 1200,
    avaliacao: 4.6,
  },
];

const statusConfig = {
  concluido: {
    label: "Concluído",
    icon: CheckCircle,
    className: "bg-accent/20 text-accent",
  },
  confirmado: {
    label: "Confirmado",
    icon: Clock,
    className: "bg-primary/20 text-primary",
  },
  cancelado: {
    label: "Cancelado",
    icon: XCircle,
    className: "bg-destructive/20 text-destructive",
  },
};

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState("reservas");
  const router = useRouter();

  // Function to switch tabs and update the hash in the URL.
  const changeTab = (tab: string) => {
    setActiveTab(tab);
    // Updates the hash in the URL without reloading the page.
    if (typeof window !== 'undefined') {
      window.history.replaceState(null, '', `#${tab}`);
    }
  };

  // Effect to read the URL hash when loading the component.
  useEffect(() => {
    const handleHashChange = () => {
      const hash = window.location.hash.substring(1); // Removes "#"
      const validTabs = ["reservas", "barcos", "favoritos", "perfil", "pagamentos", "notificacoes"];
      
      if (hash && validTabs.includes(hash)) {
        setActiveTab(hash);
      }
    };

    // Perform during initial assembly.
    handleHashChange();

    // Add listener for hash changes.
    window.addEventListener('hashchange', handleHashChange);

    // Cleanup
    return () => {
      window.removeEventListener('hashchange', handleHashChange);
    };
  }, []);

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
                  </button>
                  <button
                    onClick={() => changeTab("barcos")}
                    className={cn(
                      "w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors text-left",
                      activeTab === "barcos"
                        ? "bg-primary/10 text-primary"
                        : "text-muted-foreground hover:bg-muted"
                    )}
                  >
                    <Anchor className="w-5 h-5" />
                    Meus Barcos
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
                  <button className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-destructive hover:bg-destructive/10 transition-colors">
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
                      <Button variant="ocean">Nova Reserva</Button>
                    </Link>
                  </div>

                  <div className="space-y-4">
                    {historicoReservas.map((reserva) => {
                      const status =
                        statusConfig[
                          reserva.status as keyof typeof statusConfig
                        ];
                      return (
                        <div
                          key={reserva.id}
                          className="bg-card rounded-xl shadow-card p-4 flex flex-col md:flex-row gap-4"
                        >
                          <img
                            src={reserva.imagem.src}
                            alt={reserva.barco}
                            className="w-full md:w-40 h-32 object-cover rounded-lg"
                          />
                          <div className="flex-1">
                            <div className="flex items-start justify-between mb-2">
                              <div>
                                <h3 className="font-semibold text-foreground">
                                  {reserva.barco}
                                </h3>
                                <div className="flex items-center gap-1 text-sm text-muted-foreground">
                                  <MapPin className="w-4 h-4" />
                                  {reserva.localizacao}
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
                                {reserva.dataInicio} - {reserva.dataFim}
                              </span>
                            </div>
                            <div className="flex items-center justify-between">
                              <span className="font-bold text-primary">
                                R$ {reserva.valor.toLocaleString()}
                              </span>
                              <div className="flex gap-2">
                                {reserva.status === "concluido" && (
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
                </div>
              )}

              {/* My Boats */}
              {activeTab === "barcos" && (
                <div className="space-y-6 animate-fade-up">
                  <div className="flex items-center justify-between">
                    <h1 className="font-display text-2xl font-bold text-foreground">
                      Meus Barcos
                    </h1>
                    <Link href="/register-boat">
                      <Button variant="ocean">Adicionar Barco</Button>
                    </Link>
                  </div>

                  <div className="grid md:grid-cols-2 gap-6">
                    {meusBarcos.map((barco) => (
                      <div
                        key={barco.id}
                        className="bg-card rounded-xl shadow-card overflow-hidden"
                      >
                        <div className="relative aspect-video">
                          <img
                            src={barco.imagem.src}
                            alt={barco.nome}
                            className="w-full h-full object-cover"
                          />
                          <Badge className="absolute top-3 right-3 bg-accent text-accent-foreground">
                            {barco.status === "ativo" ? "Ativo" : "Inativo"}
                          </Badge>
                        </div>
                        <div className="p-5">
                          <h3 className="font-display text-lg font-semibold text-foreground mb-2">
                            {barco.nome}
                          </h3>
                          <div className="flex items-center gap-1 text-sm text-muted-foreground mb-3">
                            <MapPin className="w-4 h-4" />
                            {barco.localizacao}
                          </div>
                          <div className="flex items-center gap-4 text-sm mb-4">
                            <div className="flex items-center gap-1">
                              <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                              <span>{barco.avaliacao}</span>
                            </div>
                            <span className="text-muted-foreground">
                              {barco.reservas} reservas
                            </span>
                          </div>
                          <div className="flex items-center justify-between pt-4 border-t border-border">
                            <span className="font-bold text-primary">
                              R$ {barco.preco.toLocaleString()}/dia
                            </span>
                            <div className="flex gap-2">
                              <Button variant="outline" size="icon">
                                <Edit className="w-4 h-4" />
                              </Button>
                              <Button
                                variant="outline"
                                size="icon"
                                className="text-destructive hover:text-destructive"
                              >
                                <Trash2 className="w-4 h-4" />
                              </Button>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Favorites */}
              {activeTab === "favoritos" && (
                <div className="space-y-6 animate-fade-up">
                  <h1 className="font-display text-2xl font-bold text-foreground">
                    Favoritos
                  </h1>

                  <div className="grid md:grid-cols-2 gap-6">
                    {favoritos.map((barco) => (
                      <Link
                        key={barco.id}
                        href={`/barco/${barco.id}`}
                        className="group bg-card rounded-xl shadow-card overflow-hidden hover:shadow-elevated transition-all"
                      >
                        <div className="relative aspect-video">
                          <img
                            src={barco.imagem.src}
                            alt={barco.nome}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                          />
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