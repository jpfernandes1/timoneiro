"use client";

import { useState, useEffect } from "react";
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
    // step 1
    nome: "",
    tipo: "",
    descricao: "",

    // step 2
    cep: "",
    numero: "",
    rua: "",
    bairro: "",
    estado: "",
    cidade: "",
    marina: "",

    // step 3
    comprimento: "",
    capacidade: "",
    velocidade: "",
    ano: "",
    comodidades: [] as string[],

    // step 4
    fotos: [] as string[],
  });

  const [enviando, setEnviando] = useState(false);
  const [uploadingPhotos, setUploadingPhotos] = useState(false);

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

  const formatCEP = (value: string) => {
    const cep = value.replace(/\D/g, '');
    if (cep.length > 5) {
      return cep.slice(0, 5) + '-' + cep.slice(5, 8);
    }
    return cep;
  };

  // ✅ VALIDAÇÃO DO FORMULÁRIO
  const validarFormulario = () => {
    const camposObrigatorios = [
      { campo: formData.nome, nome: 'Nome do barco' },
      { campo: formData.tipo, nome: 'Tipo de embarcação' },
      { campo: formData.descricao, nome: 'Descrição' },
      { campo: formData.capacidade, nome: 'Capacidade' },
      { campo: formData.cep, nome: 'CEP' },
      { campo: formData.cidade, nome: 'Cidade' },
      { campo: formData.estado, nome: 'Estado' },
      { campo: formData.comprimento, nome: 'Comprimento' },
    ];
    
    const faltantes = camposObrigatorios
      .filter(item => !item.campo || item.campo.toString().trim() === '')
      .map(item => item.nome);
    
    if (faltantes.length > 0) {
      alert(`Por favor, preencha os seguintes campos:\n${faltantes.join('\n')}`);
      return false;
    }
    
    // Validação do CEP
    if (formData.cep.replace(/\D/g, '').length !== 8) {
      alert('CEP inválido. Deve conter 8 dígitos.');
      return false;
    }
    
    // Validação numérica
    const numericos = [
      { campo: formData.capacidade, nome: 'Capacidade', min: 1 },
      { campo: formData.comprimento, nome: 'Comprimento', min: 1 },
      { campo: formData.velocidade, nome: 'Velocidade', min: 0 },
      { campo: formData.ano, nome: 'Ano', min: 1900, max: new Date().getFullYear() + 1 }
    ];
    
    for (const num of numericos) {
      const valor = parseFloat(num.campo);
      if (isNaN(valor)) {
        alert(`${num.nome} deve ser um número válido`);
        return false;
      }
      if (num.min !== undefined && valor < num.min) {
        alert(`${num.nome} deve ser maior ou igual a ${num.min}`);
        return false;
      }
      if (num.max !== undefined && valor > num.max) {
        alert(`${num.nome} deve ser menor ou igual a ${num.max}`);
        return false;
      }
    }
    
    return true;
  };

  // Form Submit
  const handleSubmit = async () => {

      // Login Verification
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');

    if (!token) {
      alert('🔒 Faça login antes de cadastrar embarcações');
      return;
    }
    
    if (!validarFormulario()) {
      return;
    }

    if (enviando) return;
    
    setEnviando(true);
    console.log("Cadastro:", formData);
    
    try {
      // Mapear os dados do formulário para o formato do DTO
      const boatRequestDTO = {
        // Dados básicos
        name: formData.nome,
        description: formData.descricao,
        type: formData.tipo,
        capacity: formData.capacidade ? parseInt(formData.capacidade) : 0,
        
        // Características técnicas
        length: formData.comprimento ? parseFloat(formData.comprimento) : 0,
        speed: formData.velocidade ? parseFloat(formData.velocidade) : 0,
        fabrication: formData.ano ? parseInt(formData.ano) : new Date().getFullYear(),
        
        // Localização
        city: formData.cidade,
        state: formData.estado,
        marina: formData.marina,
        cep: formData.cep,
        number: formData.numero,
        street: formData.rua,
        neighborhood: formData.bairro,
        
        // Listas
        amenities: formData.comodidades,
        photos: [], // ⚠️ Array vazio por enquanto - quando implementar upload, substitua
        
        // Campo obrigatório no backend
        pricePerHour: 0.00,
      };
      
      console.log("📤 Enviando para:", 'http://localhost:8080/api/boats');
      console.log("📦 Payload:", JSON.stringify(boatRequestDTO, null, 2));
      
      const response = await fetch('http://localhost:8080/api/boats', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(boatRequestDTO)
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Erro ${response.status}: ${errorText}`);
      }
      
      const savedBoat = await response.json();
      console.log("✅ Resposta completa:", {
        status: response.status,
        data: savedBoat
      });
      
      alert('Barco cadastrado com sucesso!');
      
      // Opcional: limpar formulário após sucesso
      setFormData({
        nome: "",
        tipo: "",
        descricao: "",
        cep: "",
        numero: "",
        rua: "",
        bairro: "",
        estado: "",
        cidade: "",
        marina: "",
        comprimento: "",
        capacidade: "",
        velocidade: "",
        ano: "",
        comodidades: [],
        fotos: [],
      });
      setEtapaAtual(1);
      
    } catch (error) {
    // ✅ CORREÇÃO DO ERRO DO TYPESCRIPT
    console.error("❌ Erro ao cadastrar barco:", error);
    
    if (error instanceof Error) {
      alert(`Falha no cadastro: ${error.message}`);
    } else {
      alert('Falha no cadastro: Erro desconhecido');
    }
  } finally {
    setEnviando(false);
  }
};

  // ✅ UPLOAD DE FOTOS (SIMULADO - COMENTADO PARA FUTURA IMPLEMENTAÇÃO)
  /*
  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    
    setUploadingPhotos(true);
    
    try {
      const mockUrls = Array.from(files).map((file, i) => 
        URL.createObjectURL(file)
      );
      
      updateForm("fotos", [...formData.fotos, ...mockUrls]);
      
      console.log(`${mockUrls.length} fotos adicionadas`);
      
    } catch (error) {
      console.error("Erro no upload:", error);
      alert("Erro ao processar fotos");
    } finally {
      setUploadingPhotos(false);
    }
  };
  */

  // ✅ BUSCA DE CEP
  useEffect(() => {
    const buscarEnderecoPorCEP = async () => {
      const cepLimpo = formData.cep.replace(/\D/g, '');
      
      if (cepLimpo.length !== 8) return;

      console.log('🌐 Buscando CEP:', cepLimpo);

      try {
        const response = await fetch(`/api/cep?cep=${cepLimpo}`);
        console.log('📡 Status da Resposta:', response.status, response.ok);
        
        if (!response.ok) {
          throw new Error('CEP não encontrado');
        }

        const data = await response.json();
        console.log('📦 Dados COMPLETOS da API:', JSON.stringify(data, null, 2));
        
        // Mapeamento de siglas para estados
        const siglasParaEstados: Record<string, string> = {
          'AC': 'Acre', 'AL': 'Alagoas', 'AP': 'Amapá', 'AM': 'Amazonas',
          'BA': 'Bahia', 'CE': 'Ceará', 'DF': 'Distrito Federal', 'ES': 'Espírito Santo',
          'GO': 'Goiás', 'MA': 'Maranhão', 'MT': 'Mato Grosso', 'MS': 'Mato Grosso do Sul',
          'MG': 'Minas Gerais', 'PA': 'Pará', 'PB': 'Paraíba', 'PR': 'Paraná',
          'PE': 'Pernambuco', 'PI': 'Piauí', 'RJ': 'Rio de Janeiro',
          'RN': 'Rio Grande do Norte', 'RS': 'Rio Grande do Sul', 'RO': 'Rondônia',
          'RR': 'Roraima', 'SC': 'Santa Catarina', 'SP': 'São Paulo',
          'SE': 'Sergipe', 'TO': 'Tocantins'
        };

        // Extrair dados de múltiplas fontes possíveis
        const cidade = data.localidade || data.city || data._originalData?.city || '';
        const estadoSigla = data.uf || data.state || data._originalData?.state || '';
        const rua = data.logradouro || data.street || data._originalData?.street || '';
        const bairro = data.bairro || data.neighborhood || data._originalData?.neighborhood || '';
        
        if (cidade) {
          const nomeEstado = estadoSigla ? 
            (siglasParaEstados[estadoSigla.toUpperCase()] || estadoSigla) : '';
          
          console.log('🔄 Valores que serão setados:', { 
            rua, bairro, cidade, estado: nomeEstado 
          });
          
          setFormData(prev => ({
            ...prev,
            rua: rua,
            bairro: bairro,
            cidade: cidade,
            estado: nomeEstado,
          }));
          
          console.log('✅ Formulário preenchido com:', {
            cidade,
            estado: nomeEstado,
            rua,
            bairro
          });
        } else {
          console.log('CEP não encontrado ou dados incompletos');
        }
      } catch (error) {
        console.error('Erro ao buscar CEP:', error);
      }
    };

    const timeoutId = setTimeout(buscarEnderecoPorCEP, 500);
    return () => clearTimeout(timeoutId);
  }, [formData.cep]);

  // ✅ CONVERTER SIGLAS DE ESTADOS PARA NOMES COMPLETOS
  useEffect(() => {
    if (formData.estado && formData.estado.length === 2) {
      const siglasParaEstados: Record<string, string> = {
        'MA': 'Maranhão', 'SP': 'São Paulo', 'RJ': 'Rio de Janeiro',
        'BA': 'Bahia', 'SC': 'Santa Catarina', 'CE': 'Ceará', 
        'PE': 'Pernambuco', 'AC': 'Acre', 'AL': 'Alagoas',
        'AP': 'Amapá', 'AM': 'Amazonas', 'DF': 'Distrito Federal',
        'ES': 'Espírito Santo', 'GO': 'Goiás', 'MT': 'Mato Grosso',
        'MS': 'Mato Grosso do Sul', 'MG': 'Minas Gerais', 'PA': 'Pará',
        'PB': 'Paraíba', 'PR': 'Paraná', 'PI': 'Piauí',
        'RN': 'Rio Grande do Norte', 'RS': 'Rio Grande do Sul',
        'RO': 'Rondônia', 'RR': 'Roraima', 'SE': 'Sergipe',
        'TO': 'Tocantins'
      };
      
      const nomeCompleto = siglasParaEstados[formData.estado.toUpperCase()];
      if (nomeCompleto && nomeCompleto !== formData.estado) {
        updateForm("estado", nomeCompleto);
      }
    }
  }, [formData.estado]);

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

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="cep">CEP</Label>
                      <Input
                        id="cep"
                        placeholder="Ex: 12345-678"
                        value={formData.cep}
                        onChange={(e) => updateForm("cep", formatCEP(e.target.value))}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="numero">Número</Label>
                      <Input
                        id="numero"
                        placeholder="Ex: 123"
                        value={formData.numero}
                        onChange={(e) => updateForm("numero", e.target.value)}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="rua">Rua</Label>
                      <Input
                        id="rua"
                        placeholder="Nome da rua"
                        value={formData.rua}
                        onChange={(e) => updateForm("rua", e.target.value)}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="bairro">Bairro</Label>
                      <Input
                        id="bairro"
                        placeholder="Nome do bairro"
                        value={formData.bairro}
                        onChange={(e) => updateForm("bairro", e.target.value)}
                      />
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
                            
                  {/* ⚠️ COMENTADO PARA FUTURA IMPLEMENTAÇÃO */}
                  {/* 
                  <input
                    type="file"
                    multiple
                    accept="image/*"
                    className="hidden"
                    id="photo-upload"
                    onChange={handlePhotoUpload}
                    disabled={uploadingPhotos}
                  />

                  <label
                    htmlFor="photo-upload"
                    className="block border-2 border-dashed border-border rounded-xl p-12 text-center hover:border-primary/50 transition-colors cursor-pointer"
                  >
                    {uploadingPhotos ? (
                      <div className="flex flex-col items-center">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mb-4"></div>
                        <p>Processando fotos...</p>
                      </div>
                    ) : (
                      <>
                        <Upload className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                        <p className="text-foreground font-medium mb-2">
                          Clique para selecionar fotos
                        </p>
                        <p className="text-sm text-muted-foreground">
                          PNG, JPG até 10MB. Mínimo 1 foto recomendado.
                        </p>
                      </>
                    )}
                  </label>
                  
                  <div className="grid grid-cols-4 gap-3">
                    {formData.fotos.length > 0 ? (
                      formData.fotos.map((photoUrl, index) => (
                        <div key={index} className="relative aspect-square rounded-lg overflow-hidden">
                          <img
                            src={photoUrl}
                            alt={`Foto ${index + 1}`}
                            className="w-full h-full object-cover"
                          />
                          <button
                            type="button"
                            onClick={() => {
                              const novasFotos = formData.fotos.filter((_, i) => i !== index);
                              updateForm("fotos", novasFotos);
                            }}
                            className="absolute top-1 right-1 bg-red-500 text-white rounded-full p-1 hover:bg-red-600"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        </div>
                      ))
                    ) : (
                      [1, 2, 3, 4].map((i) => (
                        <div
                          key={i}
                          className="aspect-square bg-muted rounded-lg flex items-center justify-center"
                        >
                          <ImageIcon className="w-8 h-8 text-muted-foreground" />
                        </div>
                      ))
                    )}
                  </div>
                  */}
                  
                  <div className="border-2 border-dashed border-border rounded-xl p-12 text-center">
                    <ImageIcon className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                    <p className="text-foreground font-medium mb-2">
                      Upload de fotos em desenvolvimento
                    </p>
                    <p className="text-sm text-muted-foreground">
                      Esta funcionalidade será implementada em breve.
                    </p>
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
                        Os preços serão definidos nas janelas de locação
                      </p>
                    </div>
                  </div>

                  <div className="bg-muted/50 rounded-xl p-4">
                    <p className="text-sm text-muted-foreground">
                      Os preços serão definidos no momento de criar as janelas de locação. <br/>
                      (verifique a seção "Meus Barcos").
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
                  <Button 
                    variant="ocean" 
                    onClick={handleSubmit}
                    disabled={enviando}
                    className="min-w-[150px]"
                  >
                    {enviando ? (
                      <span className="flex items-center justify-center">
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                        Enviando...
                      </span>
                    ) : (
                      'Publicar anúncio'
                    )}
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