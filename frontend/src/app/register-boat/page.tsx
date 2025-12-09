"use client";

import { useState } from "react";
import Navbar from "@/src/components/Navbar";
import Footer from "@/src/components/Footer";
import { Button } from "@/src/components/ui/button";
import { Input } from "@/src/components/ui/input";
import { Label } from "@/src/components/ui/label";
import { Textarea } from "@/src/components/ui/textarea";
import { Checkbox } from "@/src/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/src/components/ui/select";
import {
  Upload,
  Anchor,
  MapPin,
  DollarSign,
  Users,
  Check,
  Image as ImageIcon,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";

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
  { id: "ar", label: "Ar condicionado" },
  { id: "wifi", label: "Wi-Fi" },
  { id: "som", label: "Som ambiente" },
  { id: "churrasqueira", label: "Churrasqueira" },
  { id: "mergulho", label: "Equipamentos de mergulho" },
  { id: "sup", label: "Stand Up Paddle" },
  { id: "cozinha", label: "Cozinha" },
  { id: "cabines", label: "Cabines" },
  { id: "banheiro", label: "Banheiro" },
  { id: "ancora", label: "Âncora" },
];

const etapas = [
  { numero: 1, titulo: "Dados básicos" },
  { numero: 2, titulo: "Localização" },
  { numero: 3, titulo: "Características" },
  { numero: 4, titulo: "Fotos" },
  { numero: 5, titulo: "Preço" },
];

const RegisterBoat = () => {
  const [etapaAtual, setEtapaAtual] = useState(1);
  const [formData, setFormData] = useState({
    nome: "",
    tipo: "",
    descricao: "",
    estado: "",
    cidade: "",
    marina: "",
    comprimento: "",
    capacidade: "",
    velocidade: "",
    ano: "",
    comodidades: [] as string[],
    fotos: [] as string[],
    precoDiaria: "",
    precoFimDeSemana: "",
    precoSemana: "",
  });

  const updateForm = (field: string, value: string | string[]) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const toggleComodidade = (id: string) => {
    const comodidades = formData.comodidades.includes(id)
      ? formData.comodidades.filter((c) => c !== id)
      : [...formData.comodidades, id];
    updateForm("comodidades", comodidades);
  };

  const proximaEtapa = () => {
    if (etapaAtual < 5) setEtapaAtual(etapaAtual + 1);
  };

  const etapaAnterior = () => {
    if (etapaAtual > 1) setEtapaAtual(etapaAtual - 1);
  };

  const handleSubmit = () => {
    console.log("Cadastro:", formData);
    // TODO: Submit to backend
  };

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <main className="pt-24 pb-16">
        <div className="container mx-auto px-4">
          {/* Header */}
          <div className="text-center mb-12">
            <h1 className="font-display text-3xl md:text-4xl font-bold text-foreground mb-4">
              Cadastre seu barco
            </h1>
            <p className="text-muted-foreground max-w-2xl mx-auto">
              Anuncie sua embarcação e comece a receber reservas. O processo é
              simples e leva apenas alguns minutos.
            </p>
          </div>

          {/* Progress */}
          <div className="max-w-3xl mx-auto mb-12">
            <div className="flex items-center justify-center md:gap-0">
              {etapas.map((etapa, index) => (
                <div key={etapa.numero} className="flex items-center">
                  <div className="flex flex-col items-center">
                    <div
                      className={cn(
                        "w-10 h-10 rounded-full flex items-center justify-center font-semibold transition-colors",
                        etapaAtual >= etapa.numero
                          ? "gradient-ocean text-primary-foreground"
                          : "bg-muted text-muted-foreground"
                      )}
                    >
                      {etapaAtual > etapa.numero ? (
                        <Check className="w-5 h-5" />
                      ) : (
                        etapa.numero
                      )}
                    </div>
                    <span
                      className={cn(
                        "text-xs mt-2 hidden md:block",
                        etapaAtual >= etapa.numero
                          ? "text-foreground"
                          : "text-muted-foreground"
                      )}
                    >
                      {etapa.titulo}
                    </span>
                  </div>
                  {index < etapas.length - 1 && (
                    <div
                      className={cn(
                        "w-12 md:w-24 h-1 mx-2 md:-mt-5",
                        etapaAtual > etapa.numero ? "bg-primary" : "bg-muted"
                      )}
                    />
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Form */}
          <div className="max-w-2xl mx-auto">
            <div className="bg-card rounded-2xl shadow-elevated p-8">
              {/* Step 1: Basic Info */}
              {etapaAtual === 1 && (
                <div className="space-y-6 animate-fade-up">
                  <div className="flex items-center gap-3 mb-6">
                    <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center">
                      <Anchor className="w-6 h-6 text-primary-foreground" />
                    </div>
                    <div>
                      <h2 className="font-display text-xl font-bold text-foreground">
                        Informações básicas
                      </h2>
                      <p className="text-sm text-muted-foreground">
                        Conte-nos sobre seu barco
                      </p>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="nome">Nome do barco</Label>
                    <Input
                      id="nome"
                      placeholder="Ex: Lancha Azimut 55"
                      value={formData.nome}
                      onChange={(e) => updateForm("nome", e.target.value)}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="tipo">Tipo de embarcação</Label>
                    <Select
                      value={formData.tipo}
                      onValueChange={(value) => updateForm("tipo", value)}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione o tipo" />
                      </SelectTrigger>
                      <SelectContent>
                        {tiposBarco.map((tipo) => (
                          <SelectItem key={tipo} value={tipo}>
                            {tipo}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="descricao">Descrição</Label>
                    <Textarea
                      id="descricao"
                      placeholder="Descreva sua embarcação, pontos fortes, experiência que oferece..."
                      rows={5}
                      value={formData.descricao}
                      onChange={(e) => updateForm("descricao", e.target.value)}
                    />
                  </div>
                </div>
              )}

              {/* Step 2: Location */}
              {etapaAtual === 2 && (
                <div className="space-y-6 animate-fade-up">
                  <div className="flex items-center gap-3 mb-6">
                    <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center">
                      <MapPin className="w-6 h-6 text-primary-foreground" />
                    </div>
                    <div>
                      <h2 className="font-display text-xl font-bold text-foreground">
                        Localização
                      </h2>
                      <p className="text-sm text-muted-foreground">
                        Onde seu barco está atracado?
                      </p>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label>Estado</Label>
                    <Select
                      value={formData.estado}
                      onValueChange={(value) => {
                        updateForm("estado", value);
                        updateForm("cidade", "");
                      }}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione o estado" />
                      </SelectTrigger>
                      <SelectContent>
                        {estados.map((estado) => (
                          <SelectItem key={estado} value={estado}>
                            {estado}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label>Cidade</Label>
                    <Select
                      value={formData.cidade}
                      onValueChange={(value) => updateForm("cidade", value)}
                      disabled={!formData.estado}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione a cidade" />
                      </SelectTrigger>
                      <SelectContent>
                        {formData.estado &&
                          cidades[formData.estado]?.map((cidade) => (
                            <SelectItem key={cidade} value={cidade}>
                              {cidade}
                            </SelectItem>
                          ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="marina">Marina / Porto</Label>
                    <Input
                      id="marina"
                      placeholder="Nome da marina ou porto"
                      value={formData.marina}
                      onChange={(e) => updateForm("marina", e.target.value)}
                    />
                  </div>
                </div>
              )}

              {/* Step 3: Characteristics */}
              {etapaAtual === 3 && (
                <div className="space-y-6 animate-fade-up">
                  <div className="flex items-center gap-3 mb-6">
                    <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center">
                      <Anchor className="w-6 h-6 text-primary-foreground" />
                    </div>
                    <div>
                      <h2 className="font-display text-xl font-bold text-foreground">
                        Características
                      </h2>
                      <p className="text-sm text-muted-foreground">
                        Detalhes técnicos e comodidades
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="comprimento">Comprimento (metros)</Label>
                      <Input
                        id="comprimento"
                        type="number"
                        placeholder="Ex: 17"
                        value={formData.comprimento}
                        onChange={(e) =>
                          updateForm("comprimento", e.target.value)
                        }
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="capacidade">Capacidade (pessoas)</Label>
                      <Input
                        id="capacidade"
                        type="number"
                        placeholder="Ex: 12"
                        value={formData.capacidade}
                        onChange={(e) =>
                          updateForm("capacidade", e.target.value)
                        }
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="velocidade">Velocidade máx. (nós)</Label>
                      <Input
                        id="velocidade"
                        type="number"
                        placeholder="Ex: 35"
                        value={formData.velocidade}
                        onChange={(e) =>
                          updateForm("velocidade", e.target.value)
                        }
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="ano">Ano de fabricação</Label>
                      <Input
                        id="ano"
                        type="number"
                        placeholder="Ex: 2022"
                        value={formData.ano}
                        onChange={(e) => updateForm("ano", e.target.value)}
                      />
                    </div>
                  </div>

                  <div className="space-y-3">
                    <Label>Comodidades</Label>
                    <div className="grid grid-cols-2 gap-3">
                      {comodidades.map((com) => (
                        <div
                          key={com.id}
                          className="flex items-center space-x-3"
                        >
                          <Checkbox
                            id={com.id}
                            checked={formData.comodidades.includes(com.id)}
                            onCheckedChange={() => toggleComodidade(com.id)}
                          />
                          <label
                            htmlFor={com.id}
                            className="text-sm text-foreground cursor-pointer"
                          >
                            {com.label}
                          </label>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* Step 4: Photos */}
              {etapaAtual === 4 && (
                <div className="space-y-6 animate-fade-up">
                  <div className="flex items-center gap-3 mb-6">
                    <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center">
                      <ImageIcon className="w-6 h-6 text-primary-foreground" />
                    </div>
                    <div>
                      <h2 className="font-display text-xl font-bold text-foreground">
                        Fotos
                      </h2>
                      <p className="text-sm text-muted-foreground">
                        Adicione fotos de alta qualidade
                      </p>
                    </div>
                  </div>

                  <div className="border-2 border-dashed border-border rounded-xl p-12 text-center hover:border-primary/50 transition-colors cursor-pointer">
                    <Upload className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                    <p className="text-foreground font-medium mb-2">
                      Arraste fotos aqui ou clique para selecionar
                    </p>
                    <p className="text-sm text-muted-foreground">
                      PNG, JPG até 10MB. Mínimo 5 fotos recomendado.
                    </p>
                  </div>

                  <div className="grid grid-cols-4 gap-3">
                    {[1, 2, 3, 4].map((i) => (
                      <div
                        key={i}
                        className="aspect-square bg-muted rounded-lg flex items-center justify-center"
                      >
                        <ImageIcon className="w-8 h-8 text-muted-foreground" />
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Step 5: Price */}
              {etapaAtual === 5 && (
                <div className="space-y-6 animate-fade-up">
                  <div className="flex items-center gap-3 mb-6">
                    <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center">
                      <DollarSign className="w-6 h-6 text-primary-foreground" />
                    </div>
                    <div>
                      <h2 className="font-display text-xl font-bold text-foreground">
                        Preço
                      </h2>
                      <p className="text-sm text-muted-foreground">
                        Defina suas tarifas
                      </p>
                    </div>
                  </div>

                  <div className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="precoDiaria">
                        Diária (segunda a quinta)
                      </Label>
                      <div className="relative">
                        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
                          R$
                        </span>
                        <Input
                          id="precoDiaria"
                          type="number"
                          placeholder="0,00"
                          className="pl-10"
                          value={formData.precoDiaria}
                          onChange={(e) =>
                            updateForm("precoDiaria", e.target.value)
                          }
                        />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="precoFimDeSemana">
                        Diária (sexta a domingo)
                      </Label>
                      <div className="relative">
                        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
                          R$
                        </span>
                        <Input
                          id="precoFimDeSemana"
                          type="number"
                          placeholder="0,00"
                          className="pl-10"
                          value={formData.precoFimDeSemana}
                          onChange={(e) =>
                            updateForm("precoFimDeSemana", e.target.value)
                          }
                        />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="precoSemana">
                        Pacote semanal (opcional)
                      </Label>
                      <div className="relative">
                        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
                          R$
                        </span>
                        <Input
                          id="precoSemana"
                          type="number"
                          placeholder="0,00"
                          className="pl-10"
                          value={formData.precoSemana}
                          onChange={(e) =>
                            updateForm("precoSemana", e.target.value)
                          }
                        />
                      </div>
                    </div>
                  </div>

                  <div className="bg-muted/50 rounded-xl p-4">
                    <p className="text-sm text-muted-foreground">
                      💡 <strong>Dica:</strong> Pesquise barcos similares na sua
                      região para definir um preço competitivo. A NavegarBem
                      cobra uma taxa de serviço de 10% sobre cada reserva.
                    </p>
                  </div>
                </div>
              )}

              {/* Navigation */}
              <div className="flex justify-between mt-8 pt-6 border-t border-border">
                <Button
                  variant="outline"
                  onClick={etapaAnterior}
                  disabled={etapaAtual === 1}
                >
                  Voltar
                </Button>
                {etapaAtual < 5 ? (
                  <Button variant="ocean" onClick={proximaEtapa}>
                    Continuar
                  </Button>
                ) : (
                  <Button variant="ocean" onClick={handleSubmit}>
                    Publicar anúncio
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
};

export default RegisterBoat;
