"use client";

import { useState } from "react";
import Navbar from "@/src/components/Navbar";
import Footer from "@/src/components/Footer";
import { Button } from "@/src/components/ui/button";
import { Input } from "@/src/components/ui/input";
import { Label } from "@/src/components/ui/label";
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
} from "lucide-react";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { cn } from "@/src/lib/utils";
import Link from 'next/link';
import boat1 from "@/src/assets/boat-1.jpg";
import boat2 from "@/src/assets/boat-2.jpg";
import boat3 from "@/src/assets/boat-3.jpg";

const estados = [
  "Rio de Janeiro",
  "São Paulo",
  "Bahia",
  "Santa Catarina",
  "Ceará",
  "Pernambuco",
];

const cidades: Record<string, string[]> = {
  "Rio de Janeiro": ["Angra dos Reis", "Búzios", "Cabo Frio", "Paraty"],
  "São Paulo": ["Ilhabela", "Ubatuba", "Guarujá", "Santos"],
  Bahia: ["Salvador", "Porto Seguro", "Morro de São Paulo", "Itacaré"],
  "Santa Catarina": ["Florianópolis", "Balneário Camboriú", "Bombinhas"],
  Ceará: ["Fortaleza", "Jericoacoara", "Cumbuco"],
  Pernambuco: ["Recife", "Porto de Galinhas", "Fernando de Noronha"],
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

const barcos = [
  {
    id: 1,
    nome: "Lancha Azimut 55",
    imagem: boat1,
    preco: 2500,
    localizacao: "Angra dos Reis, RJ",
    capacidade: 12,
    avaliacao: 4.9,
    reviews: 48,
    tipo: "Lancha",
  },
  {
    id: 2,
    nome: "Veleiro Beneteau 40",
    imagem: boat2,
    preco: 1800,
    localizacao: "Ilhabela, SP",
    capacidade: 8,
    avaliacao: 4.8,
    reviews: 35,
    tipo: "Veleiro",
  },
  {
    id: 3,
    nome: "Catamarã Lagoon 42",
    imagem: boat3,
    preco: 3200,
    localizacao: "Búzios, RJ",
    capacidade: 15,
    avaliacao: 5.0,
    reviews: 62,
    tipo: "Catamarã",
  },
  {
    id: 4,
    nome: "Iate Sunseeker 68",
    imagem: boat1,
    preco: 5500,
    localizacao: "Florianópolis, SC",
    capacidade: 20,
    avaliacao: 4.9,
    reviews: 28,
    tipo: "Iate",
  },
  {
    id: 5,
    nome: "Lancha Ferretti 45",
    imagem: boat2,
    preco: 2100,
    localizacao: "Salvador, BA",
    capacidade: 10,
    avaliacao: 4.7,
    reviews: 41,
    tipo: "Lancha",
  },
  {
    id: 6,
    nome: "Escuna Pirata",
    imagem: boat3,
    preco: 1200,
    localizacao: "Paraty, RJ",
    capacidade: 30,
    avaliacao: 4.6,
    reviews: 89,
    tipo: "Escuna",
  },
];

const Search = () => {
  const [estado, setEstado] = useState("");
  const [cidade, setCidade] = useState("");
  const [dataInicio, setDataInicio] = useState<Date>();
  const [dataFim, setDataFim] = useState<Date>();
  const [precoRange, setPrecoRange] = useState([500, 5000]);
  const [capacidade, setCapacidade] = useState("");
  const [tiposSelecionados, setTiposSelecionados] = useState<string[]>([]);
  const [comodidadesSelecionadas, setComodidadesSelecionadas] = useState<
    string[]
  >([]);
  const [showFilters, setShowFilters] = useState(false);

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

            <Button variant="ocean" size="lg">
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

              {/* Price Range */}
              <div className="bg-card rounded-xl p-6 shadow-soft">
                <h3 className="font-semibold text-foreground mb-4">
                  Faixa de Preço
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
                        id={tipo}
                        checked={tiposSelecionados.includes(tipo)}
                        onCheckedChange={() => toggleTipo(tipo)}
                      />
                      <label
                        htmlFor={tipo}
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
                        id={comodidade}
                        checked={comodidadesSelecionadas.includes(comodidade)}
                        onCheckedChange={() => toggleComodidade(comodidade)}
                      />
                      <label
                        htmlFor={comodidade}
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
                  onClick={() => setShowFilters(false)}
                >
                  Aplicar Filtros
                </Button>
              )}
            </aside>

            {/* Results */}
            <div className="flex-1">
              <div className="flex items-center justify-between mb-6">
                <p className="text-muted-foreground">
                  <span className="font-semibold text-foreground">
                    {barcos.length} barcos
                  </span>{" "}
                  encontrados
                </p>
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

              {/* Results Grid */}
              <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-6">
                {barcos.map((barco) => (
                  <Link
                    key={barco.id}
                    href={`/barco/${barco.id}`}
                    className="group bg-card rounded-2xl overflow-hidden shadow-card hover:shadow-elevated transition-all duration-300"
                  >
                    <div className="relative aspect-[4/3] overflow-hidden">
                      <img
                        src={barco.imagem.src}
                        alt={barco.nome}
                        className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
                      />
                      <button
                        className="absolute top-4 right-4 w-10 h-10 bg-card/90 backdrop-blur-sm rounded-full flex items-center justify-center hover:bg-card transition-colors"
                        onClick={(e) => {
                          e.preventDefault();
                          // TODO: Add to favorites
                        }}
                      >
                        <Heart className="w-5 h-5 text-foreground" />
                      </button>
                      <div className="absolute bottom-4 left-4 bg-card/90 backdrop-blur-sm px-3 py-1 rounded-full">
                        <span className="text-sm font-medium text-foreground">
                          {barco.tipo}
                        </span>
                      </div>
                    </div>
                    <div className="p-5">
                      <div className="flex items-start justify-between mb-2">
                        <h3 className="font-display text-lg font-semibold text-foreground group-hover:text-primary transition-colors">
                          {barco.nome}
                        </h3>
                        <div className="flex items-center gap-1">
                          <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                          <span className="text-sm font-medium">
                            {barco.avaliacao}
                          </span>
                          <span className="text-sm text-muted-foreground">
                            ({barco.reviews})
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-2 text-muted-foreground mb-3">
                        <MapPin className="w-4 h-4" />
                        <span className="text-sm">{barco.localizacao}</span>
                      </div>
                      <div className="flex items-center gap-2 text-muted-foreground mb-4">
                        <Users className="w-4 h-4" />
                        <span className="text-sm">
                          Até {barco.capacidade} pessoas
                        </span>
                      </div>
                      <div className="flex items-center justify-between pt-4 border-t border-border">
                        <div>
                          <span className="text-2xl font-bold text-primary">
                            R$ {barco.preco.toLocaleString()}
                          </span>
                          <span className="text-muted-foreground">/dia</span>
                        </div>
                        <Button variant="ocean" size="sm">
                          Ver detalhes
                        </Button>
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      <Footer />
    </div>
  );
};

export default Search;
