import { Button } from "@/src/components/ui/button";
import { Star, Users, Anchor, ArrowRight } from "lucide-react";
import boat1 from "@/src/assets/boat-1.jpg";
import boat2 from "@/src/assets/boat-2.jpg";
import boat3 from "@/src/assets/boat-3.jpg";

const boats = [
    {
    id: 1,
    name: "Azimut 55",
    type: "Iate de Luxo",
    location: "Angra dos Reis, RJ",
    price: 3500,
    rating: 4.9,
    reviews: 127,
    guests: 12,
    image: boat1,
  },
  {
    id: 2,
    name: "Catamaran Lagoon 42",
    type: "Veleiro",
    location: "Ilhabela, SP",
    price: 2200,
    rating: 4.8,
    reviews: 89,
    guests: 8,
    image: boat2,
  },
  {
    id: 3,
    name: "Chris-Craft Heritage",
    type: "Lancha Clássica",
    location: "Florianópolis, SC",
    price: 1800,
    rating: 5.0,
    reviews: 54,
    guests: 6,
    image: boat3,
  },
];

const BoatsSection = () => {
  return (
    <section id="boats" className="py-20 lg:py-28 bg-background">
      <div className="container mx-auto px-4">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-12">
          <div>
            <p className="text-primary font-medium mb-2">Destaques</p>
            <h2 className="font-display text-3xl md:text-4xl lg:text-5xl font-bold text-foreground">
              Barcos em Alta
            </h2>
          </div>
          <Button variant="ghost" className="self-start md:self-auto group">
            Ver todos os barcos
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </Button>
        </div>

        {/* Boats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8">
          {boats.map((boat, index) => (
            <div
              key={boat.id}
              className="group bg-card rounded-2xl overflow-hidden shadow-card hover:shadow-elevated transition-all duration-500 hover:-translate-y-2"
              style={{ animationDelay: `${index * 0.1}s` }}
            >
              {/* Image */}
              <div className="relative h-56 overflow-hidden">
                <img
                  src={boat.image.src}
                  alt={boat.name}
                  className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700"
                />
                <div className="absolute top-4 left-4">
                  <span className="bg-primary-foreground/90 backdrop-blur-sm text-foreground text-xs font-medium px-3 py-1.5 rounded-full">
                    {boat.type}
                  </span>
                </div>
                <div className="absolute top-4 right-4 flex items-center gap-1 bg-foreground/80 backdrop-blur-sm text-primary-foreground text-sm font-medium px-2.5 py-1 rounded-full">
                  <Star className="w-3.5 h-3.5 fill-current" />
                  {boat.rating}
                </div>
              </div>

              {/* Content */}
              <div className="p-5">
                <div className="flex items-start justify-between gap-2 mb-2">
                  <h3 className="font-display text-xl font-semibold text-card-foreground group-hover:text-primary transition-colors">
                    {boat.name}
                  </h3>
                </div>
                
                <p className="text-muted-foreground text-sm mb-4 flex items-center gap-1">
                  <Anchor className="w-3.5 h-3.5" />
                  {boat.location}
                </p>

                <div className="flex items-center justify-between pt-4 border-t border-border">
                  <div className="flex items-center gap-3 text-sm text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <Users className="w-4 h-4" />
                      {boat.guests}
                    </span>
                    <span>•</span>
                    <span>{boat.reviews} avaliações</span>
                  </div>
                  <div className="text-right">
                    <span className="text-lg font-bold text-foreground">
                      R$ {boat.price.toLocaleString("pt-BR")}
                    </span>
                    <span className="text-muted-foreground text-sm">/dia</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default BoatsSection;