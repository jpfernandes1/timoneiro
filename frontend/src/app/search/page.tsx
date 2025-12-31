"use client";

import { useState, useEffect } from "react";
import Navbar from "@/src/components/Navbar";
import Footer from "@/src/components/Footer";
import { Button } from "@/src/components/ui/button";
import { Input } from "@/src/components/ui/input";
import { Label } from "@/src/components/ui/label";
import { boatApi,  type BoatResponseDTO } from '@/src/lib/api';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/src/components/ui/select";
import { Calendar } from "@/src/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/src/components/ui/popover";
import { Slider } from "@/src/components/ui/slider";
import { Checkbox } from "@/src/components/ui/checkbox";
import {
  Search as SearchIcon,
  MapPin,
  CalendarIcon,
  Users,
  Anchor,
  Star,
  Heart,
  Filter,
  X,
  Image as ImageIcon,
} from "lucide-react";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { cn } from "@/src/lib/utils";
import Link from 'next/link';

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

interface BoatCardProps {
  boat: BoatResponseDTO;
}

const estados = [
  "Rio de Janeiro",
  "São Paulo",
  "Bahia",
  "Santa Catarina",
  "Ceará",
  "Pernambuco",
  "Maranhão"
];

const cidades: Record<string, string[]> = {
  "Rio de Janeiro": ["Angra dos Reis", "Búzios", "Cabo Frio", "Paraty"],
  "São Paulo": ["Ilhabela", "Ubatuba", "Guarujá", "Santos"],
  Bahia: ["Salvador", "Porto Seguro", "Morro de São Paulo", "Itacaré"],
  "Santa Catarina": ["Florianópolis", "Balneário Camboriú", "Bombinhas"],
  Ceará: ["Fortaleza", "Jericoacoara", "Cumbuco"],
  Pernambuco: ["Recife", "Porto de Galinhas", "Fernando de Noronha"],
  Maranhão: ["Porto Franco", "Imperatriz"],
};

const tiposBarco = [
  "Lancha",
  "Veleiro",
  "Iate",
  "Catamarã",
  "Jet Ski",
  "Escuna",
];

const comodidades = [
  "Ar condicionado",
  "Churrasqueira",
  "Som ambiente",
  "Wi-Fi",
  "Equipamentos de mergulho",
  "Stand Up Paddle",
];

const Search = () => {
  // Search form states
  const [estado, setEstado] = useState("");
  const [cidade, setCidade] = useState("");
  const [dataInicio, setDataInicio] = useState<Date>();
  const [dataFim, setDataFim] = useState<Date>();
  const [precoRange, setPrecoRange] = useState([500, 5000]);
  const [capacidade, setCapacidade] = useState("");
  const [tiposSelecionados, setTiposSelecionados] = useState<string[]>([]);
  const [comodidadesSelecionadas, setComodidadesSelecionadas] = useState<string[]>([]);
  const [showFilters, setShowFilters] = useState(false);
  
  // States for backend data
  const [boats, setBoats] = useState<BoatResponseDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filteredBoats, setFilteredBoats] = useState<BoatResponseDTO[]>([]);

  // Function to search for boats from the backend.
  const fetchBoats = async () => {
  try {
    setLoading(true);
    setError(null);
    
    const data = await boatApi.getAllBoats();
    console.log('📦 Dados recebidos do backend:', data);
    setBoats(data);
    setFilteredBoats(data); // initially shows all
    
  } catch (err) {
    console.error('❌ Erro ao buscar barcos:', err);
    setError(err instanceof Error ? err.message : 'Erro desconhecido');
  } finally {
    setLoading(false);
  }
};

  // Boat Card Component
  const BoatCard = ({ boat }: BoatCardProps) => {
    // first photo of the boat or a placeholder.
    const mainPhoto = boat.photos && boat.photos.length > 0 
      ? boat.photos[0] 
      : '/placeholder-boat.jpg';
    
    // Calculate price per day (8 hours as standard)
    const pricePerDay = boat.pricePerHour * 8;
    
    return (
      <Link
        href={`/boat/${boat.id}`}
        className="group bg-card rounded-2xl overflow-hidden shadow-card hover:shadow-elevated transition-all duration-300 block"
      >
        <div className="relative aspect- 4/3 overflow-hidden">
          <div className="w-full h-full bg-linear-to-br from-gray-200 to-gray-300 flex items-center justify-center">
            {mainPhoto.startsWith('http') ? (
              <img
                src={mainPhoto}
                alt={boat.name}
                className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
                onError={(e) => {
                  (e.target as HTMLImageElement).src = '/placeholder-boat.jpg';
                }}
              />
            ) : (
              <div className="flex flex-col items-center justify-center p-4">
                <ImageIcon className="w-12 h-12 text-gray-400 mb-2" />
                <span className="text-sm text-gray-500 text-center">Sem foto disponível</span>
              </div>
            )}
          </div>
          
          {/* Favorite Button */}
          <button
            className="absolute top-4 right-4 w-10 h-10 bg-card/90 backdrop-blur-sm rounded-full flex items-center justify-center hover:bg-card transition-colors"
            onClick={(e) => {
              e.preventDefault();
              // TODO: Add to favorites
            }}
          >
            <Heart className="w-5 h-5 text-foreground" />
          </button>
          
          {/* Type badge */}
          <div className="absolute bottom-4 left-4 bg-card/90 backdrop-blur-sm px-3 py-1 rounded-full">
            <span className="text-sm font-medium text-foreground">
              {boat.type}
            </span>
          </div>
        </div>
        
        <div className="p-5">
          <div className="flex items-start justify-between mb-2">
            <h3 className="font-display text-lg font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-1">
              {boat.name}
            </h3>
            {/* Avaliação - placeholder por enquanto */}
            <div className="flex items-center gap-1 shrink-0">
              <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
              <span className="text-sm font-medium">4.5</span>
              <span className="text-sm text-muted-foreground">(12)</span>
            </div>
          </div>
          
          {/* Location */}
          <div className="flex items-center gap-2 text-muted-foreground mb-3">
            <MapPin className="w-4 h-4 shrink-0" />
            <span className="text-sm truncate">
              {boat.city}, {boat.state}
              {boat.marina && ` • ${boat.marina}`}
            </span>
          </div>
          
          {/* Capacity */}
          <div className="flex items-center gap-2 text-muted-foreground mb-4">
            <Users className="w-4 h-4 shrink-0" />
            <span className="text-sm">Até {boat.capacity} pessoas</span>
          </div>
          
          {/* Amenidades (two first) */}
          {boat.amenities && boat.amenities.length > 0 && (
            <div className="mb-4">
              <div className="flex flex-wrap gap-2">
                {boat.amenities.slice(0, 3).map((amenity, index) => (
                  <span 
                    key={index} 
                    className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded-full"
                  >
                    {amenity}
                  </span>
                ))}
                {boat.amenities.length > 3 && (
                  <span className="text-xs text-muted-foreground px-2 py-1">
                    +{boat.amenities.length - 3}
                  </span>
                )}
              </div>
            </div>
          )}
          
          {/* Price */}
          <div className="flex items-center justify-between pt-4 border-t border-border">
            <div>
              {pricePerDay > 0 ? (
                <>
                  <span className="text-2xl font-bold text-primary">
                    R$ {pricePerDay.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                  </span>
                  <span className="text-muted-foreground">/dia</span>
                  <div className="text-xs text-muted-foreground">
                    R$ {boat.pricePerHour.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}/hora
                  </div>
                </>
              ) : (
                <div className="text-foreground font-medium">Preço sob consulta</div>
              )}
            </div>
            <Button variant="ocean" size="sm">
              Ver detalhes
            </Button>
          </div>
        </div>
      </Link>
    );
  };

  // Filter Functions
  const toggleTipo = (tipo: string) => {
    setTiposSelecionados((prev) =>
      prev.includes(tipo) ? prev.filter((t) => t !== tipo) : [...prev, tipo]
    );
  };

  const toggleComodidade = (comodidade: string) => {
    setComodidadesSelecionadas((prev) =>
      prev.includes(comodidade)
        ? prev.filter((c) => c !== comodidade)
        : [...prev, comodidade]
    );
  };

  // Search function with filters
  const handleSearch = () => {
    let filtered = [...boats];
    
    // Filter by state
    if (estado) {
  filtered = filtered.filter(boat => 
    boat.state.toLowerCase().includes(estado.toLowerCase())
  );
}
    // Filter by city
    if (cidade) {
  filtered = filtered.filter(boat => 
    boat.city.toLowerCase().includes(cidade.toLowerCase())
  );
}
    
    // Filter by price range
    filtered = filtered.filter(boat => {
      const pricePerDay = boat.pricePerHour * 8;
      return pricePerDay >= precoRange[0] && pricePerDay <= precoRange[1];
    });
    
      // Filter by capacity
      if (capacidade) {
    if (capacidade.includes('+')) {
      // For values ​​such as "20+"
      const min = parseInt(capacidade);
      filtered = filtered.filter(boat => boat.capacity >= min);
    } else if (capacidade.includes('-')) {
      // For intervals such as "1-5" or "6-10"
      const [minStr, maxStr] = capacidade.split('-');
      const min = parseInt(minStr);
      const max = parseInt(maxStr);
      filtered = filtered.filter(boat => 
        boat.capacity >= min && boat.capacity <= max
        );
      }
    }
    
    // Filter by types
    if (tiposSelecionados.length > 0) {
      filtered = filtered.filter(boat => 
        tiposSelecionados.includes(boat.type)
      );
    }
    
    // Filter by amenities
    if (comodidadesSelecionadas.length > 0) {
      filtered = filtered.filter(boat => 
        comodidadesSelecionadas.every(comodidade => 
          boat.amenities?.includes(comodidade)
        )
      );
    }
    
    setFilteredBoats(filtered);
    setShowFilters(false);
  };

  // Clear filters
  const clearFilters = () => {
    setEstado("");
    setCidade("");
    setDataInicio(undefined);
    setDataFim(undefined);
    setPrecoRange([500, 5000]);
    setCapacidade("");
    setTiposSelecionados([]);
    setComodidadesSelecionadas([]);
    setFilteredBoats(boats);
  };

  // Load boats when the component is assembled.
  useEffect(() => {
    fetchBoats();
  }, []);

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      {/* Search Header */}
      <section className="pt-24 pb-8 gradient-sky">
        <div className="container mx-auto px-4">
          <h1 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-6">
            Encontre o barco perfeito
          </h1>

          {/* Quick Search Bar */}
          <div className="bg-card rounded-2xl shadow-card p-4 flex flex-wrap gap-4 items-end">
            <div className="flex-1 min-w-[200px]">
              <Label className="text-sm text-muted-foreground mb-1 block">
                Estado
              </Label>
              <Select value={estado} onValueChange={setEstado}>
                <SelectTrigger>
                  <SelectValue placeholder="Selecione o estado" />
                </SelectTrigger>
                <SelectContent>
                  {estados.map((e) => (
                    <SelectItem key={e} value={e}>
                      {e}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex-1 min-w-[200px]">
              <Label className="text-sm text-muted-foreground mb-1 block">
                Cidade
              </Label>
              <Select
                value={cidade}
                onValueChange={setCidade}
                disabled={!estado}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Selecione a cidade" />
                </SelectTrigger>
                <SelectContent>
                  {estado &&
                    cidades[estado]?.map((c) => (
                      <SelectItem key={c} value={c}>
                        {c}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex-1 min-w-[150px]">
              <Label className="text-sm text-muted-foreground mb-1 block">
                Data início
              </Label>
              <Popover>
                <PopoverTrigger asChild>
                  <Button
                    variant="outline"
                    className={cn(
                      "w-full justify-start text-left font-normal",
                      !dataInicio && "text-muted-foreground"
                    )}
                  >
                    <CalendarIcon className="mr-2 h-4 w-4" />
                    {dataInicio
                      ? format(dataInicio, "dd/MM/yyyy", { locale: ptBR })
                      : "Selecione"}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0" align="start">
                  <Calendar
                    mode="single"
                    selected={dataInicio}
                    onSelect={setDataInicio}
                    initialFocus
                    className="pointer-events-auto"
                  />
                </PopoverContent>
              </Popover>
            </div>

            <div className="flex-1 min-w-[150px]">
              <Label className="text-sm text-muted-foreground mb-1 block">
                Data fim
              </Label>
              <Popover>
                <PopoverTrigger asChild>
                  <Button
                    variant="outline"
                    className={cn(
                      "w-full justify-start text-left font-normal",
                      !dataFim && "text-muted-foreground"
                    )}
                  >
                    <CalendarIcon className="mr-2 h-4 w-4" />
                    {dataFim
                      ? format(dataFim, "dd/MM/yyyy", { locale: ptBR })
                      : "Selecione"}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0" align="start">
                  <Calendar
                    mode="single"
                    selected={dataFim}
                    onSelect={setDataFim}
                    initialFocus
                    className="pointer-events-auto"
                  />
                </PopoverContent>
              </Popover>
            </div>

            <Button variant="ocean" size="lg" onClick={handleSearch}>
              <SearchIcon className="w-5 h-5 mr-2" />
              Buscar
            </Button>

            <Button
              variant="outline"
              size="lg"
              onClick={() => setShowFilters(!showFilters)}
              className="lg:hidden"
            >
              <Filter className="w-5 h-5" />
            </Button>
          </div>
        </div>
      </section>

      {/* Main Content */}
      <section className="py-8">
        <div className="container mx-auto px-4">
          <div className="flex gap-8">
            {/* Filters Sidebar */}
            <aside
              className={cn(
                "w-80 shrink-0 space-y-6",
                showFilters
                  ? "fixed inset-0 z-50 bg-background p-6 overflow-auto lg:relative lg:inset-auto lg:z-auto lg:p-0"
                  : "hidden lg:block"
              )}
            >
              {showFilters && (
                <div className="flex items-center justify-between lg:hidden mb-4">
                  <h2 className="font-display text-xl font-bold">Filtros</h2>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => setShowFilters(false)}
                  >
                    <X className="w-5 h-5" />
                  </Button>
                </div>
              )}

              <div className="flex justify-between items-center mb-4">
                <h2 className="font-display text-xl font-bold">Filtros</h2>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={clearFilters}
                  className="text-sm"
                >
                  Limpar tudo
                </Button>
              </div>

              {/* Price Range */}
              <div className="bg-card rounded-xl p-6 shadow-soft">
                <h3 className="font-semibold text-foreground mb-4">
                  Faixa de Preço (por dia)
                </h3>
                <Slider
                  value={precoRange}
                  onValueChange={setPrecoRange}
                  min={0}
                  max={10000}
                  step={100}
                  className="mb-4"
                />
                <div className="flex justify-between text-sm text-muted-foreground">
                  <span>R$ {precoRange[0].toLocaleString()}</span>
                  <span>R$ {precoRange[1].toLocaleString()}</span>
                </div>
              </div>

              {/* Capacity */}
              <div className="bg-card rounded-xl p-6 shadow-soft">
                <h3 className="font-semibold text-foreground mb-4">
                  Capacidade
                </h3>
                <Select value={capacidade} onValueChange={setCapacidade}>
                  <SelectTrigger>
                    <SelectValue placeholder="Número de pessoas" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="1-5">1-5 pessoas</SelectItem>
                    <SelectItem value="6-10">6-10 pessoas</SelectItem>
                    <SelectItem value="11-20">11-20 pessoas</SelectItem>
                    <SelectItem value="20+">20+ pessoas</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {/* Boat Type */}
              <div className="bg-card rounded-xl p-6 shadow-soft">
                <h3 className="font-semibold text-foreground mb-4">
                  Tipo de Barco
                </h3>
                <div className="space-y-3">
                  {tiposBarco.map((tipo) => (
                    <div key={tipo} className="flex items-center space-x-3">
                      <Checkbox
                        id={`tipo-${tipo}`}
                        checked={tiposSelecionados.includes(tipo)}
                        onCheckedChange={() => toggleTipo(tipo)}
                      />
                      <label
                        htmlFor={`tipo-${tipo}`}
                        className="text-sm text-foreground cursor-pointer"
                      >
                        {tipo}
                      </label>
                    </div>
                  ))}
                </div>
              </div>

              {/* Amenities */}
              <div className="bg-card rounded-xl p-6 shadow-soft">
                <h3 className="font-semibold text-foreground mb-4">
                  Comodidades
                </h3>
                <div className="space-y-3">
                  {comodidades.map((comodidade) => (
                    <div
                      key={comodidade}
                      className="flex items-center space-x-3"
                    >
                      <Checkbox
                        id={`comodidade-${comodidade}`}
                        checked={comodidadesSelecionadas.includes(comodidade)}
                        onCheckedChange={() => toggleComodidade(comodidade)}
                      />
                      <label
                        htmlFor={`comodidade-${comodidade}`}
                        className="text-sm text-foreground cursor-pointer"
                      >
                        {comodidade}
                      </label>
                    </div>
                  ))}
                </div>
              </div>

              {showFilters && (
                <Button
                  variant="ocean"
                  className="w-full lg:hidden"
                  onClick={handleSearch}
                >
                  Aplicar Filtros
                </Button>
              )}
            </aside>

            {/* Results */}
            <div className="flex-1">
              {/* Header com contador e ordenação */}
              <div className="flex items-center justify-between mb-6">
                {loading ? (
                  <div className="h-6 w-48 bg-gray-200 rounded animate-pulse"></div>
                ) : error ? (
                  <p className="text-muted-foreground">Erro ao carregar barcos</p>
                ) : (
                  <p className="text-muted-foreground">
                    <span className="font-semibold text-foreground">
                      {filteredBoats.length} barcos
                    </span>{" "}
                    encontrados
                  </p>
                )}
                
                <Select defaultValue="relevancia">
                  <SelectTrigger className="w-48">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="relevancia">Mais relevantes</SelectItem>
                    <SelectItem value="preco-asc">Menor preço</SelectItem>
                    <SelectItem value="preco-desc">Maior preço</SelectItem>
                    <SelectItem value="avaliacao">Melhor avaliação</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {/* Loading State */}
              {loading && (
                <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-6">
                  {[...Array(6)].map((_, index) => (
                    <div key={index} className="animate-pulse">
                      <div className="bg-gray-200 rounded-2xl aspect-4/3 mb-4"></div>
                      <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                      <div className="h-3 bg-gray-200 rounded w-1/2 mb-4"></div>
                      <div className="h-8 bg-gray-200 rounded w-full"></div>
                    </div>
                  ))}
                </div>
              )}

              {/* Error State */}
              {error && !loading && (
                <div className="bg-red-50 border border-red-200 rounded-xl p-8 text-center">
                  <div className="text-red-500 mb-4">❌</div>
                  <h3 className="font-semibold text-foreground mb-2">
                    Erro ao carregar barcos
                  </h3>
                  <p className="text-muted-foreground mb-4">{error}</p>
                  <Button variant="outline" onClick={fetchBoats}>
                    Tentar novamente
                  </Button>
                </div>
              )}

              {/* Empty State */}
              {!loading && !error && filteredBoats.length === 0 && (
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-8 text-center">
                  <div className="text-gray-400 mb-4">🚤</div>
                  <h3 className="font-semibold text-foreground mb-2">
                    Nenhum barco encontrado
                  </h3>
                  <p className="text-muted-foreground mb-4">
                    {boats.length === 0
                      ? "Ainda não há barcos cadastrados no sistema."
                      : "Nenhum barco corresponde aos filtros aplicados."}
                  </p>
                  {boats.length === 0 && (
                    <Link href="/cadastrar-barco">
                      <Button variant="ocean">Cadastre seu primeiro barco</Button>
                    </Link>
                  )}
                </div>
              )}

              {/* Results Grid */}
              {!loading && !error && filteredBoats.length > 0 && (
                <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-6">
                  {filteredBoats.map((boat) => (
                    <BoatCard key={boat.id} boat={boat} />
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      <Footer />
    </div>
  );
};

export default Search;