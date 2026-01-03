// app/boat/[id]/owner/page.tsx
"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Navbar from "@/src/components/Navbar";
import Footer from "@/src/components/Footer";
import { Button } from "@/src/components/ui/button";
import { Input } from "@/src/components/ui/input";
import { Label } from "@/src/components/ui/label";
import { Textarea } from "@/src/components/ui/textarea";
import { Badge } from "@/src/components/ui/badge";
import { Checkbox } from "@/src/components/ui/checkbox";
import {
  Calendar,
  MapPin,
  Users,
  Anchor,
  Star,
  Clock,
  Ruler,
  Zap,
  CalendarDays,
  Edit,
  Save,
  X,
  Plus,
  Trash2,
  ArrowLeft,
  Loader2,
  AlertCircle,
  Image as ImageIcon,
  ChevronLeft,
  ChevronRight,
  Wifi,
  Wind,
  Music,
  Flame,
  LifeBuoy,
  FireExtinguisher,
  Navigation,
  Radio,
  Sailboat,
  Fish,
  Waves,
  ChefHat,
  Refrigerator,
  Microwave,
  Utensils,
  Bed,
  Bath,
  Droplets,
} from "lucide-react";
import { cn } from "@/lib/utils";
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

interface BoatAvailability {
  id: number;
  boatId: number;
  startDate: string;
  endDate: string;
  pricePerHour: number;
}

// Amenities organizadas por categoria (igual ao modelo)
const amenitiesByCategory = {
  "Conforto": [
    { id: "ar-condicionado", label: "Ar condicionado", icon: Wind },
    { id: "churrasqueira", label: "Churrasqueira", icon: Flame },
    { id: "som-ambiente", label: "Som ambiente", icon: Music },
    { id: "wifi", label: "Wi-Fi", icon: Wifi },
  ],
  "Segurança": [
    { id: "coletes-salva-vidas", label: "Coletes salva-vidas", icon: LifeBuoy },
    { id: "extintor-incendio", label: "Extintor de incêndio", icon: FireExtinguisher },
    { id: "gps", label: "GPS", icon: Navigation },
    { id: "radio-vhf", label: "Rádio VHF", icon: Radio },
  ],
  "Lazer": [
    { id: "equipamentos-mergulho", label: "Equipamentos de mergulho", icon: Waves },
    { id: "stand-up-paddle", label: "Stand Up Paddle", icon: Sailboat },
    { id: "pesca-esportiva", label: "Pesca esportiva", icon: Fish },
    { id: "snorkel", label: "Tábua de snorkel", icon: Waves },
  ],
  "Cozinha": [
    { id: "fogao", label: "Fogão", icon: ChefHat },
    { id: "geladeira", label: "Geladeira", icon: Refrigerator },
    { id: "microondas", label: "Micro-ondas", icon: Microwave },
    { id: "utensilios-cozinha", label: "Utensílios de cozinha", icon: Utensils },
  ],
  "Cabines": [
    { id: "cabine-principal", label: "Cabine principal", icon: Bed },
    { id: "cabine-hospedes", label: "Cabine de hóspedes", icon: Bed },
    { id: "banheiro", label: "Banheiro", icon: Bath },
    { id: "chuveiro", label: "Chuveiro", icon: Droplets },
  ],
};

const BoatOwnerPage = () => {
  const params = useParams();
  const router = useRouter();
  const boatId = params.id as string;

  // States
  const [boat, setBoat] = useState<Boat | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  
  // Edit mode states
  const [isEditing, setIsEditing] = useState(false);
  const [editedBoat, setEditedBoat] = useState<Partial<Boat>>({});
  const [saving, setSaving] = useState(false);
  
  // Availability states
  const [availabilities, setAvailabilities] = useState<BoatAvailability[]>([]);
  const [showAvailabilityForm, setShowAvailabilityForm] = useState(false);
  const [newAvailability, setNewAvailability] = useState({
    startDate: "",
    endDate: "",
    pricePerHour: 0,
  });
  const [availabilityLoading, setAvailabilityLoading] = useState(false);

  // Check if boat has a specific amenity
  const hasAmenity = (amenityLabel: string) => {
    if (!boat) return false;
    return boat.amenities?.some(amenity => 
      amenity.toLowerCase().includes(amenityLabel.toLowerCase())
    ) || false;
  };

  // Handle amenity checkbox change
  const handleAmenityChange = (amenityLabel: string, isChecked: boolean) => {
    if (!editedBoat) return;

    const currentAmenities = editedBoat.amenities || [];
    
    if (isChecked) {
      // Add amenity if not already present
      if (!currentAmenities.includes(amenityLabel)) {
        setEditedBoat({
          ...editedBoat,
          amenities: [...currentAmenities, amenityLabel]
        });
      }
    } else {
      // Remove amenity
      setEditedBoat({
        ...editedBoat,
        amenities: currentAmenities.filter(amenity => amenity !== amenityLabel)
      });
    }
  };

  // Fetch boat details
  const fetchBoatDetails = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetch(buildUrl(`/boats/${boatId}`));
      
      if (!response.ok) {
        if (response.status === 404) {
          throw new Error("Barco não encontrado");
        }
        throw new Error(`Erro ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      setBoat(data);
      setEditedBoat(data);
      setNewAvailability(prev => ({ ...prev, pricePerHour: data.pricePerHour }));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro desconhecido");
    } finally {
      setLoading(false);
    }
  }, [boatId]);

  // Fetch availabilities
  const fetchAvailabilities = useCallback(async () => {
    try {
      const response = await fetch(buildUrl(`/boats/${boatId}/availability`));
      if (response.ok) {
        const data = await response.json();
        setAvailabilities(data);
      }
    } catch (err) {
      console.error("Erro ao buscar disponibilidades:", err);
    }
  }, [boatId]);

  // Initial data fetch
  useEffect(() => {
    if (boatId) {
      fetchBoatDetails();
      fetchAvailabilities();
    }
  }, [boatId, fetchBoatDetails, fetchAvailabilities]);

  // Handle save boat
  const handleSaveBoat = async () => {
    if (!boat) return;
    
    setSaving(true);
    try {
      const response = await fetch(buildUrl(`/boats/${boat.id}`), {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(editedBoat),
      });

      if (!response.ok) {
        throw new Error("Falha ao atualizar barco");
      }

      const updatedBoat = await response.json();
      setBoat(updatedBoat);
      setEditedBoat(updatedBoat);
      setIsEditing(false);
      
      // Update price in availability form
      setNewAvailability(prev => ({ ...prev, pricePerHour: updatedBoat.pricePerHour }));
    } catch (err) {
      console.error("Erro ao atualizar barco:", err);
      alert("Erro ao atualizar o barco. Tente novamente.");
    } finally {
      setSaving(false);
    }
  };

  // Handle create availability
  const handleCreateAvailability = async () => {
    if (!boat) return;
    
    setAvailabilityLoading(true);
    try {
      // Convert dates to LocalDateTime format
      const availabilityToSend = {
        ...newAvailability,
        startDate: `${newAvailability.startDate}:00`,
        endDate: `${newAvailability.endDate}:00`,
      };

      const response = await fetch(buildUrl(`/boats/${boatId}/availability`), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(availabilityToSend),
      });

      if (!response.ok) {
        throw new Error("Falha ao criar janela de disponibilidade");
      }

      const createdAvailability = await response.json();
      setAvailabilities([...availabilities, createdAvailability]);
      setShowAvailabilityForm(false);
      setNewAvailability({
        startDate: "",
        endDate: "",
        pricePerHour: boat.pricePerHour,
      });
    } catch (err) {
      console.error("Erro ao criar disponibilidade:", err);
      alert("Erro ao criar janela de disponibilidade.");
    } finally {
      setAvailabilityLoading(false);
    }
  };

  // Handle delete availability
  const handleDeleteAvailability = async (id: number) => {
    if (!confirm("Tem certeza que deseja excluir esta janela de disponibilidade?")) return;

    try {
      const response = await fetch(buildUrl(`/boats/${boatId}/availability/${id}`), {
        method: "DELETE",
      });

      if (!response.ok) {
        throw new Error("Falha ao excluir disponibilidade");
      }

      setAvailabilities(availabilities.filter((av) => av.id !== id));
    } catch (err) {
      console.error("Erro ao excluir disponibilidade:", err);
      alert("Erro ao excluir janela de disponibilidade.");
    }
  };

  // Format date for display
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Images navigation
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

  // Enter edit mode
  const handleEditClick = () => {
    setIsEditing(true);
  };

  // Cancel edit
  const handleCancelEdit = () => {
    setIsEditing(false);
    setEditedBoat(boat || {});
  };

  // Loading state
  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="pt-24 container mx-auto px-4">
          <div className="flex flex-col items-center justify-center py-12">
            <Loader2 className="w-12 h-12 text-primary animate-spin mb-4" />
            <p className="text-muted-foreground">Carregando detalhes do barco...</p>
          </div>
        </div>
        <Footer />
      </div>
    );
  }

  // Error state
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
              O barco que você está procurando não está disponível.
            </p>
            <div className="flex gap-4 justify-center">
              <Button variant="outline" onClick={() => router.back()}>
                <ArrowLeft className="w-4 h-4 mr-2" />
                Voltar
              </Button>
              <Link href="/dashboard#meus-barcos">
                <Button variant="ocean">Meus Barcos</Button>
              </Link>
            </div>
          </div>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      {/* Header with action buttons */}
      <div className="container mx-auto px-4 pt-24">
        <div className="flex items-center justify-between mb-6">
          <div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground mb-2">
              <Link href="/dashboard#meus-barcos" className="hover:text-foreground transition-colors">
                Meus Barcos
              </Link>
              <span>/</span>
              <span className="text-foreground font-medium">{boat.name}</span>
            </div>
            <h1 className="font-display text-3xl font-bold text-foreground">
              {isEditing ? (
                <Input
                  value={editedBoat.name || ""}
                  onChange={(e) => setEditedBoat({ ...editedBoat, name: e.target.value })}
                  className="text-3xl font-bold"
                />
              ) : (
                boat.name
              )}
            </h1>
          </div>

          <div className="flex gap-3">
            <Button
              variant={isEditing ? "outline" : "default"}
              onClick={isEditing ? handleCancelEdit : handleEditClick}
              disabled={saving}
            >
              {isEditing ? (
                <>
                  <X className="w-4 h-4 mr-2" />
                  Cancelar
                </>
              ) : (
                <>
                  <Edit className="w-4 h-4 mr-2" />
                  Editar Barco
                </>
              )}
            </Button>

            <Button
              variant="ocean"
              onClick={isEditing ? handleSaveBoat : () => setShowAvailabilityForm(true)}
              disabled={isEditing ? false : false}
            >
              {isEditing ? (
                saving ? (
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                ) : (
                  <Save className="w-4 h-4 mr-2" />
                )
              ) : (
                <Calendar className="w-4 h-4 mr-2" />
              )}
              {isEditing ? (saving ? "Salvando..." : "Salvar") : "Criar Janela de Disponibilidade"}
            </Button>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container mx-auto px-4 pb-16">
        <div className="grid lg:grid-cols-3 gap-8">
          {/* Left column - Boat details */}
          <div className="lg:col-span-2 space-y-8">
            {/* Images Section */}
            <div className="space-y-4">
              <div className="relative rounded-2xl overflow-hidden bg-linear-to-br from-gray-100 to-gray-200 aspect-4/3">
                {boat.photos && boat.photos.length > 0 ? (
                  <>
                    <img
                      src={boat.photos[selectedImageIndex]}
                      alt={`${boat.name} - Foto ${selectedImageIndex + 1}`}
                      className="w-full h-full object-cover"
                    />
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
            </div>

            {/* Specifications */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h3 className="font-semibold text-foreground mb-6">Especificações técnicas</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="text-center">
                  <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center mx-auto mb-3">
                    <Users className="w-6 h-6 text-primary-foreground" />
                  </div>
                  {isEditing ? (
                    <Input
                      type="number"
                      value={editedBoat.capacity || ""}
                      onChange={(e) => setEditedBoat({ ...editedBoat, capacity: parseInt(e.target.value) })}
                      className="text-center"
                    />
                  ) : (
                    <div className="text-foreground font-semibold">{boat.capacity}</div>
                  )}
                  <div className="text-sm text-muted-foreground">Pessoas</div>
                </div>
                <div className="text-center">
                  <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center mx-auto mb-3">
                    <Ruler className="w-6 h-6 text-primary-foreground" />
                  </div>
                  {isEditing ? (
                    <Input
                      type="number"
                      step="0.1"
                      value={editedBoat.length || ""}
                      onChange={(e) => setEditedBoat({ ...editedBoat, length: parseFloat(e.target.value) })}
                      className="text-center"
                    />
                  ) : (
                    <div className="text-foreground font-semibold">{boat.length}m</div>
                  )}
                  <div className="text-sm text-muted-foreground">Comprimento</div>
                </div>
                <div className="text-center">
                  <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center mx-auto mb-3">
                    <Zap className="w-6 h-6 text-primary-foreground" />
                  </div>
                  {isEditing ? (
                    <Input
                      type="number"
                      step="0.1"
                      value={editedBoat.speed || ""}
                      onChange={(e) => setEditedBoat({ ...editedBoat, speed: parseFloat(e.target.value) })}
                      className="text-center"
                    />
                  ) : (
                    <div className="text-foreground font-semibold">{boat.speed}</div>
                  )}
                  <div className="text-sm text-muted-foreground">Nós</div>
                </div>
                <div className="text-center">
                  <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center mx-auto mb-3">
                    <CalendarDays className="w-6 h-6 text-primary-foreground" />
                  </div>
                  {isEditing ? (
                    <Input
                      type="number"
                      value={editedBoat.fabrication || ""}
                      onChange={(e) => setEditedBoat({ ...editedBoat, fabrication: parseInt(e.target.value) })}
                      className="text-center"
                    />
                  ) : (
                    <div className="text-foreground font-semibold">{boat.fabrication}</div>
                  )}
                  <div className="text-sm text-muted-foreground">Ano</div>
                </div>
              </div>
            </div>

            {/* Description */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h3 className="font-semibold text-foreground mb-4">Descrição</h3>
              {isEditing ? (
                <Textarea
                  value={editedBoat.description || ""}
                  onChange={(e) => setEditedBoat({ ...editedBoat, description: e.target.value })}
                  rows={6}
                  className="w-full"
                />
              ) : (
                <p className="text-foreground/80 leading-relaxed whitespace-pre-line">
                  {boat.description}
                </p>
              )}
            </div>

            {/* Amenities - Nova Versão com Ícones Verdes */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h3 className="font-semibold text-foreground mb-6">Comodidades e equipamentos</h3>
              
              {isEditing ? (
                // Modo edição - Checkboxes com ícones
                <div className="space-y-8">
                  {Object.entries(amenitiesByCategory).map(([category, amenities]) => (
                    <div key={category}>
                      <h4 className="font-medium text-foreground mb-4">{category}</h4>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {amenities.map(({ id, label, icon: Icon }) => {
                          const isChecked = editedBoat.amenities?.some(amenity => 
                            amenity.toLowerCase().includes(label.toLowerCase())
                          ) || false;
                          
                          return (
                            <div key={id} className="flex items-center space-x-3">
                              <Checkbox
                                id={`amenity-${id}`}
                                checked={isChecked}
                                onCheckedChange={(checked) => 
                                  handleAmenityChange(label, checked as boolean)
                                }
                                className="h-5 w-5"
                              />
                              <label
                                htmlFor={`amenity-${id}`}
                                className="flex items-center gap-3 text-sm cursor-pointer flex-1"
                              >
                                <Icon className={cn(
                                  "w-5 h-5",
                                  isChecked ? "text-green-500" : "text-gray-400"
                                )} />
                                <span className="text-foreground">{label}</span>
                              </label>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                  
                  {/* Outras comodidades - campo de texto livre */}
                  <div className="pt-6 border-t border-border">
                    <h4 className="font-medium text-foreground mb-4">Outras comodidades</h4>
                    <Textarea
                      value={editedBoat.amenities?.filter(amenity => {
                        // Filtrar amenities que não estão na lista fixa
                        const allFixedAmenities = Object.values(amenitiesByCategory)
                          .flat()
                          .map(a => a.label);
                        return !allFixedAmenities.some(fixed => 
                          amenity.toLowerCase().includes(fixed.toLowerCase())
                        );
                      }).join(", ") || ""}
                      onChange={(e) => {
                        const otherAmenities = e.target.value
                          .split(",")
                          .map(item => item.trim())
                          .filter(item => item);
                        
                        // Combinar amenities fixas (que estão nos checkboxes) com as outras
                        const fixedAmenities = editedBoat.amenities?.filter(amenity => {
                          const allFixedAmenities = Object.values(amenitiesByCategory)
                            .flat()
                            .map(a => a.label);
                          return allFixedAmenities.some(fixed => 
                            amenity.toLowerCase().includes(fixed.toLowerCase())
                          );
                        }) || [];
                        
                        setEditedBoat({
                          ...editedBoat,
                          amenities: [...fixedAmenities, ...otherAmenities]
                        });
                      }}
                      placeholder="Digite outras comodidades separadas por vírgula"
                      rows={3}
                      className="w-full"
                    />
                    <p className="text-sm text-muted-foreground mt-2">
                      Separe cada comodidade por vírgula
                    </p>
                  </div>
                </div>
              ) : (
                // Modo visualização - Ícones coloridos (verde se tem, cinza se não tem)
                <div className="space-y-8">
                  {Object.entries(amenitiesByCategory).map(([category, amenities]) => (
                    <div key={category}>
                      <h4 className="font-medium text-foreground mb-4">{category}</h4>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                        {amenities.map(({ id, label, icon: Icon }) => {
                          const hasThisAmenity = hasAmenity(label);
                          return (
                            <div key={id} className="flex items-center gap-3">
                              <Icon className={cn(
                                "w-5 h-5",
                                hasThisAmenity ? "text-green-500" : "text-gray-300"
                              )} />
                              <span className={cn(
                                "text-sm",
                                hasThisAmenity ? "text-foreground" : "text-muted-foreground"
                              )}>
                                {label}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                  
                  {/* Outras comodidades não categorizadas */}
                  {boat.amenities && boat.amenities.length > 0 && (
                    <div className="pt-8 border-t border-border">
                      <h4 className="font-medium text-foreground mb-4">Outras comodidades</h4>
                      <div className="flex flex-wrap gap-2">
                        {boat.amenities
                          .filter(amenity => {
                            // Filtrar amenities que não estão na lista fixa
                            const allFixedAmenities = Object.values(amenitiesByCategory)
                              .flat()
                              .map(a => a.label);
                            return !allFixedAmenities.some(fixed => 
                              amenity.toLowerCase().includes(fixed.toLowerCase())
                            );
                          })
                          .map((amenity, index) => (
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
              )}
            </div>
          </div>

          {/* Right column - Price and Availability */}
          <div className="space-y-6">
            {/* Price Section */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h3 className="font-semibold text-foreground mb-4">Preço por hora</h3>
              {isEditing ? (
                <div className="space-y-2">
                  <Input
                    type="number"
                    step="0.01"
                    value={editedBoat.pricePerHour || ""}
                    onChange={(e) => setEditedBoat({ ...editedBoat, pricePerHour: parseFloat(e.target.value) })}
                    className="text-2xl font-bold"
                  />
                  <p className="text-sm text-muted-foreground">
                    Este será o preço padrão para novas janelas de disponibilidade
                  </p>
                </div>
              ) : (
                <div className="text-3xl font-bold text-primary">
                  R$ {boat.pricePerHour.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
                </div>
              )}
            </div>

            {/* Location */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <h3 className="font-semibold text-foreground mb-4">Localização</h3>
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-muted-foreground">
                  <MapPin className="w-4 h-4" />
                  <span>
                    {boat.city}, {boat.state}
                  </span>
                </div>
                {boat.marina && (
                  <div className="text-sm text-foreground/80">
                    <span className="font-medium">Marina:</span> {boat.marina}
                  </div>
                )}
              </div>
            </div>

            {/* Availability Windows */}
            <div className="bg-card rounded-2xl p-6 shadow-soft">
              <div className="flex items-center justify-between mb-6">
                <h3 className="font-semibold text-foreground">Janelas de Disponibilidade</h3>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowAvailabilityForm(true)}
                  disabled={isEditing}
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Nova Janela
                </Button>
              </div>

              {availabilities.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">
                  <Calendar className="w-12 h-12 mx-auto mb-4 opacity-50" />
                  <p>Nenhuma janela de disponibilidade cadastrada</p>
                  <p className="text-sm mt-2">
                    Clique em "Nova Janela" para criar períodos disponíveis para reserva
                  </p>
                </div>
              ) : (
                <div className="space-y-3 max-h-[400px] overflow-y-auto">
                  {availabilities.map((av) => (
                    <div
                      key={av.id}
                      className="p-4 border border-border rounded-lg hover:bg-muted/50 transition-colors"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="font-medium text-foreground">
                            {formatDate(av.startDate)}
                          </div>
                          <div className="text-sm text-muted-foreground">
                            até {formatDate(av.endDate)}
                          </div>
                          <div className="mt-2 text-lg font-bold text-primary">
                            R$ {av.pricePerHour.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}/hora
                          </div>
                        </div>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeleteAvailability(av.id)}
                          className="text-destructive hover:text-destructive"
                          disabled={isEditing}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </main>

      {/* Availability Form Modal */}
      {showAvailabilityForm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-card rounded-2xl p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="font-display text-xl font-bold text-foreground">
                Criar Janela de Disponibilidade
              </h3>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setShowAvailabilityForm(false)}
              >
                <X className="w-4 h-4" />
              </Button>
            </div>

            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="startDate">Data e Hora Inicial</Label>
                <Input
                  type="datetime-local"
                  id="startDate"
                  value={newAvailability.startDate}
                  onChange={(e) =>
                    setNewAvailability({ ...newAvailability, startDate: e.target.value })
                  }
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="endDate">Data e Hora Final</Label>
                <Input
                  type="datetime-local"
                  id="endDate"
                  value={newAvailability.endDate}
                  onChange={(e) =>
                    setNewAvailability({ ...newAvailability, endDate: e.target.value })
                  }
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="pricePerHour">Preço por Hora (R$)</Label>
                <Input
                  type="number"
                  id="pricePerHour"
                  step="0.01"
                  value={newAvailability.pricePerHour}
                  onChange={(e) =>
                    setNewAvailability({
                      ...newAvailability,
                      pricePerHour: parseFloat(e.target.value),
                    })
                  }
                />
                <p className="text-sm text-muted-foreground">
                  Preço padrão do barco: R$ {boat.pricePerHour.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
                </p>
              </div>

              <div className="flex gap-3 pt-4">
                <Button
                  variant="outline"
                  className="flex-1"
                  onClick={() => setShowAvailabilityForm(false)}
                  disabled={availabilityLoading}
                >
                  Cancelar
                </Button>
                <Button
                  className="flex-1"
                  onClick={handleCreateAvailability}
                  disabled={availabilityLoading || !newAvailability.startDate || !newAvailability.endDate}
                >
                  {availabilityLoading ? (
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  ) : (
                    <Calendar className="w-4 h-4 mr-2" />
                  )}
                  {availabilityLoading ? "Criando..." : "Criar Janela"}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
};

export default BoatOwnerPage;