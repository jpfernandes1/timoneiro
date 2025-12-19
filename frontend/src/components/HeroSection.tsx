import heroImage from "@/src/assets/hero-boat.jpg";
import { Search, MapPin, Calendar } from "lucide-react";
import { Button } from "@/src/components/ui/button";

const HeroSection = () => {
  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden">
      {/* Background Image */}
      <div className="absolute inset-0">
        <img
          src={heroImage.src}
          alt="Iate de luxo navegando em águas cristalinas"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-b from-ocean-deep/70 via-ocean-deep/50 to-ocean-deep/80" />
      </div>

      {/* Content */}
      <div className="relative z-10 container mx-auto px-4 pt-20">
        <div className="max-w-4xl mx-auto text-center">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 bg-primary-foreground/10 backdrop-blur-sm border border-primary-foreground/20 rounded-full px-4 py-2 mb-6 animate-fade-up">
            <span className="w-2 h-2 rounded-full bg-accent animate-pulse" />
            <span className="text-primary-foreground/90 text-sm font-medium">
              +2.000 barcos disponíveis
            </span>
          </div>

          {/* Heading */}
          <h1 className="font-display text-4xl md:text-5xl lg:text-7xl font-bold text-primary-foreground mb-6 animate-fade-up" style={{ animationDelay: "0.1s" }}>
            Navegue pelos melhores
            <span className="block text-ocean-light">destinos do Brasil</span>
          </h1>

          {/* Subheading */}
          <p className="text-primary-foreground/80 text-lg md:text-xl max-w-2xl mx-auto mb-10 animate-fade-up" style={{ animationDelay: "0.2s" }}>
            Alugue barcos, lanchas e iates com proprietários verificados. 
            Experiências únicas a partir de R$ 500/dia.
          </p>

          {/* Search Box */}
          <div className="bg-primary-foreground/95 backdrop-blur-md rounded-2xl p-4 md:p-6 shadow-elevated animate-fade-up max-w-3xl mx-auto" style={{ animationDelay: "0.3s" }}>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Location */}
              <div className="flex items-center gap-3 p-3 rounded-xl bg-muted/50 hover:bg-muted transition-colors">
                <MapPin className="w-5 h-5 text-primary" />
                <div className="text-left">
                  <p className="text-xs text-muted-foreground font-medium">Destino</p>
                  <input
                    type="text"
                    placeholder="Para onde você vai?"
                    className="bg-transparent border-none outline-none text-foreground placeholder:text-muted-foreground/60 w-full text-sm"
                  />
                </div>
              </div>

              {/* Date */}
              <div className="flex items-center gap-3 p-3 rounded-xl bg-muted/50 hover:bg-muted transition-colors">
                <Calendar className="w-5 h-5 text-primary" />
                <div className="text-left">
                  <p className="text-xs text-muted-foreground font-medium">Data</p>
                  <input
                    type="text"
                    placeholder="Quando?"
                    className="bg-transparent border-none outline-none text-foreground placeholder:text-muted-foreground/60 w-full text-sm"
                  />
                </div>
              </div>

              {/* Search Button */}
              <Button variant="ocean" size="lg" className="h-full min-h-[56px]">
                <Search className="w-5 h-5" />
                Buscar Barcos
              </Button>
            </div>
          </div>

          {/* Stats */}
          <div className="flex flex-wrap justify-center gap-8 md:gap-16 mt-12 animate-fade-up" style={{ animationDelay: "0.4s" }}>
            {[
              { value: "2.500+", label: "Barcos" },
              { value: "50+", label: "Destinos" },
              { value: "15.000+", label: "Avaliações" },
            ].map((stat) => (
              <div key={stat.label} className="text-center">
                <p className="text-2xl md:text-3xl font-bold text-primary-foreground">{stat.value}</p>
                <p className="text-primary-foreground/70 text-sm">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Scroll Indicator */}
      <div className="absolute bottom-8 left-1/2 -translate-x-1/2 animate-bounce">
        <div className="w-6 h-10 rounded-full border-2 border-primary-foreground/50 flex items-start justify-center p-2">
          <div className="w-1 h-2 rounded-full bg-primary-foreground/70" />
        </div>
      </div>
    </section>
  );
};

export default HeroSection;
