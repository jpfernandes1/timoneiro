"use client";

import { useState, useEffect, useRef } from "react";
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
import { buildUrl } from '@/src/lib/api';
import { ImageCompressor, type CompressionOptions } from '@/src/lib/image-compressor';

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
    // Step 1
    nome: "",
    tipo: "",
    descricao: "",

    // Step 2
    cep: "",
    numero: "",
    rua: "",
    bairro: "",
    estado: "",
    cidade: "",
    marina: "",

    // Step 3
    comprimento: "",
    capacidade: "",
    velocidade: "",
    ano: "",
    comodidades: [] as string[],

    // Step 4
    fotos: [] as File[],
  });

  const [enviando, setEnviando] = useState(false);
  const [uploadingPhotos, setUploadingPhotos] = useState(false);
  const [photoPreviews, setPhotoPreviews] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const updateForm = (field: string, value: string | string[] | File[]) => {
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

  // Photo upload - multiple files
  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    
    setUploadingPhotos(true);
    
    try {
      // Convert FileList to array
      const originalFiles = Array.from(files);

    console.log(`📤 ${originalFiles.length} arquivo(s) selecionado(s)`);
    
    // ✅ Compression utility.
    console.log('🔄 Comprimindo imagens para otimização...');

    // Compression options
    const compressionOptions: CompressionOptions = {
      maxWidth: 1920,
      quality: 0.8,
      outputFormat: 'image/jpeg'
    };
    
    const compressionResults = await ImageCompressor.compressAll(originalFiles, compressionOptions);
    
    // Extract only the compressed files.
    const compressedFiles = compressionResults.map(result => result.file);
    
      
      // Validations
      const maxFiles = 10;
       if (formData.fotos.length + compressedFiles.length > maxFiles) {
      alert(`Máximo de ${maxFiles} fotos permitidas`);
      setUploadingPhotos(false);
      return;
    }
      
      const maxSize = 10 * 1024 * 1024; // 10MB
      const oversized = compressedFiles.filter(file => file.size > maxSize);
      if (oversized.length > 0) {
      alert(`⚠️ Após compressão, ${oversized.length} imagem(nes) ainda excede(m) 10MB. 
Tente selecionar imagens menores ou entre em contato com o suporte.`);
      setUploadingPhotos(false);
      return;
      }
      
      // Add new files
      const allFiles = [...formData.fotos, ...compressedFiles];
      updateForm("fotos", allFiles);
      
      // Create previews
       const newPreviews = compressedFiles.map(file => URL.createObjectURL(file));
    setPhotoPreviews(prev => [...prev, ...newPreviews]);
    
    console.log(`✅ ${compressedFiles.length} foto(s) adicionada(s) após compressão`);
      
    } catch (error) {
      console.error("❌ Erro no processamento das imagens:", error);
      alert("Ocorreu um erro ao processar as imagens. Tente novamente ou selecione outras fotos.");
    } finally {
      setUploadingPhotos(false);
      // Clear input to allow selecting the same files again
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  // Remove photo
  const removePhoto = (index: number) => {
    // Revoke preview URL to avoid memory leak
    URL.revokeObjectURL(photoPreviews[index]);
    
    const newFiles = formData.fotos.filter((_, i) => i !== index);
    const newPreviews = photoPreviews.filter((_, i) => i !== index);
    
    updateForm("fotos", newFiles);
    setPhotoPreviews(newPreviews);
  };

  // Form validation
  const validarFormulario = () => {
    const camposObrigatorios = [
      { campo: formData.nome, nome: 'Nome do barco' },
      { campo: formData.tipo, nome: 'Tipo de embarcação' },
      { campo: formData.descricao, nome: 'Descrição' },
      { campo: formData.capacidade, nome: 'Capacidade' },
      { campo: formData.cidade, nome: 'Cidade' },
      { campo: formData.estado, nome: 'Estado' },
    ];
    
    const faltantes = camposObrigatorios
      .filter(item => !item.campo || item.campo.toString().trim() === '')
      .map(item => item.nome);
    
    if (faltantes.length > 0) {
      alert(`Por favor, preencha os seguintes campos:\n${faltantes.join('\n')}`);
      return false;
    }
    
    // CEP validation (if filled)
    if (formData.cep && formData.cep.replace(/\D/g, '').length !== 8) {
      alert('CEP inválido. Deve conter 8 dígitos.');
      return false;
    }
    
    // Numeric validation
    const numericos = [
      { campo: formData.capacidade, nome: 'Capacidade', min: 1 },
      { campo: formData.comprimento, nome: 'Comprimento', min: 1, required: true },
      { campo: formData.velocidade, nome: 'Velocidade', min: 0, required: false },
      { campo: formData.ano, nome: 'Ano', min: 1900, max: new Date().getFullYear() + 1, required: false }
    ];
    
    for (const num of numericos) {
      const valor = num.campo ? parseFloat(num.campo) : NaN;
      if (num.required && isNaN(valor)) {
        alert(`${num.nome} deve ser um número válido`);
        return false;
      }
      if (!isNaN(valor)) {
        if (num.min !== undefined && valor < num.min) {
          alert(`${num.nome} deve ser maior ou igual a ${num.min}`);
          return false;
        }
        if (num.max !== undefined && valor > num.max) {
          alert(`${num.nome} deve ser menor ou igual a ${num.max}`);
          return false;
        }
      }
    }
    
    // Photo validation (at least 1)
    if (formData.fotos.length === 0) {
      const confirmacao = confirm("Você não selecionou nenhuma foto. Deseja continuar mesmo assim?");
      if (!confirmacao) return false;
    }
    
    return true;
  };

  // Form submit - multipart/form-data
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
    console.log("📤 Iniciando cadastro...");
    
    try {
      // Map form data to DTO format
      const boatRequestDTO = {
        // Basic data
        name: formData.nome,
        description: formData.descricao,
        type: formData.tipo,
        capacity: formData.capacidade ? parseInt(formData.capacidade) : 0,
        
        // Technical characteristics
        length: formData.comprimento ? parseFloat(formData.comprimento) : 0,
        speed: formData.velocidade ? parseFloat(formData.velocidade) : 0,
        fabrication: formData.ano ? parseInt(formData.ano) : new Date().getFullYear(),
        
        // Location
        city: formData.cidade,
        state: formData.estado,
        marina: formData.marina || "",
        cep: formData.cep || "",
        number: formData.numero || "",
        street: formData.rua || "",
        neighborhood: formData.bairro || "",
        
        // Lists
        amenities: formData.comodidades,
        
        // Required field in backend
        pricePerHour: 0.00,
      };
      
      console.log("📦 Payload JSON:", JSON.stringify(boatRequestDTO, null, 2));
      console.log("📸 Photos to upload:", formData.fotos.length);

      
      // Use buildUrl function to dynamically build the URL
      const url = buildUrl('/boats');
      
      // Create FormData for multipart submission
      const formDataToSend = new FormData();
      
      // Add JSON as string
      formDataToSend.append('boat', JSON.stringify(boatRequestDTO));
      
      // Add each photo
      formData.fotos.forEach((file, index) => {
        formDataToSend.append('images', file);
        console.log(`📎 Anexando foto ${index + 1}:`, file.name, `(${(file.size / 1024 / 1024).toFixed(2)}MB)`);
      });
      
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
        body: formDataToSend
      });
      
      console.log("📡 Status da resposta:", response.status);
      
      if (!response.ok) {
        let errorMessage = `Erro ${response.status}`;
        try {
          const errorData = await response.json();
          errorMessage += `: ${JSON.stringify(errorData)}`;
        } catch {
          const errorText = await response.text();
          errorMessage += `: ${errorText}`;
        }
        throw new Error(errorMessage);
      }
      
      const savedBoat = await response.json();
      console.log("✅ Barco cadastrado com sucesso:", savedBoat);
      
      // Clear photo previews
      photoPreviews.forEach(preview => URL.revokeObjectURL(preview));
      
      alert('Barco cadastrado com sucesso!');
      
      // Reset form
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
      setPhotoPreviews([]);
      setEtapaAtual(1);
      
    } catch (error) {
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

  // CEP lookup
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
        
        // Mapping state abbreviations to full names
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

        const cidade = data.localidade || data.city || data._originalData?.city || '';
        const estadoSigla = data.uf || data.state || data._originalData?.state || '';
        const rua = data.logradouro || data.street || data._originalData?.street || '';
        const bairro = data.bairro || data.neighborhood || data._originalData?.neighborhood || '';
        
        if (cidade) {
          const nomeEstado = estadoSigla ? 
            (siglasParaEstados[estadoSigla.toUpperCase()] || estadoSigla) : '';
          
          setFormData(prev => ({
            ...prev,
            rua: rua,
            bairro: bairro,
            cidade: cidade,
            estado: nomeEstado,
          }));
        }
      } catch (error) {
        console.error('Erro ao buscar CEP:', error);
      }
    };

    const timeoutId = setTimeout(buscarEnderecoPorCEP, 500);
    return () => clearTimeout(timeoutId);
  }, [formData.cep]);

  // Convert state abbreviations to full names
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

  // Clean previews when component unmounts
  useEffect(() => {
    return () => {
      photoPreviews.forEach(preview => URL.revokeObjectURL(preview));
    };
  }, []);

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
                        Adicione fotos de alta qualidade (máx. 10 fotos, 10MB cada)
                      </p>
                    </div>
                  </div>
                            
                  <input
                    type="file"
                    multiple
                    accept="image/*"
                    className="hidden"
                    id="photo-upload"
                    onChange={handlePhotoUpload}
                    disabled={uploadingPhotos}
                    ref={fileInputRef}
                  />

                  <label
                    htmlFor="photo-upload"
                    className="block border-2 border-dashed border-border rounded-xl p-8 text-center hover:border-primary/50 transition-colors cursor-pointer"
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
                          PNG, JPG até 10MB. Máximo 10 fotos.
                        </p>
                        <p className="text-xs text-muted-foreground mt-2">
                          {formData.fotos.length} foto(s) selecionada(s)
                        </p>
                      </>
                    )}
                  </label>
                  
                  {formData.fotos.length > 0 && (
                    <div className="space-y-3">
                      <Label>Pré-visualização das fotos:</Label>
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                        {formData.fotos.map((file, index) => (
                          <div key={index} className="relative aspect-square rounded-lg overflow-hidden border border-border">
                            <img
                              src={photoPreviews[index]}
                              alt={`Preview ${index + 1}`}
                              className="w-full h-full object-cover"
                            />
                            <div className="absolute bottom-0 left-0 right-0 bg-black/70 text-white p-1 text-xs">
                              {file.name.length > 15 
                                ? `${file.name.substring(0, 12)}...` 
                                : file.name}
                            </div>
                            <button
                              type="button"
                              onClick={() => removePhoto(index)}
                              className="absolute top-1 right-1 bg-red-500 text-white rounded-full p-1 hover:bg-red-600 transition-colors"
                            >
                              <X className="w-4 h-4" />
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
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

                  <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
                    <p className="text-sm text-blue-800 font-medium mb-1">
                      📋 Resumo do cadastro:
                    </p>
                    <ul className="text-xs text-blue-700 space-y-1">
                      <li>• Barco: {formData.nome || "Não definido"}</li>
                      <li>• Tipo: {formData.tipo || "Não definido"}</li>
                      <li>• Fotos: {formData.fotos.length} selecionada(s)</li>
                      <li>• Comodidades: {formData.comodidades.length} selecionada(s)</li>
                    </ul>
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