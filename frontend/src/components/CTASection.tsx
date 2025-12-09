import { Button } from "@/src/components/ui/button";
import { Anchor, ArrowRight } from "lucide-react";
import Link from 'next/link';

const CTASection = () => {
  return (
    <section className="py-20 lg:py-28 gradient-ocean relative overflow-hidden">
      {/* Decorative Elements */}
      <div className="absolute top-0 left-0 w-96 h-96 bg-primary-foreground/5 rounded-full blur-3xl -translate-x-1/2 -translate-y-1/2" />
      <div className="absolute bottom-0 right-0 w-96 h-96 bg-accent/20 rounded-full blur-3xl translate-x-1/2 translate-y-1/2" />

      <div className="container mx-auto px-4 relative z-10">
        <div className="max-w-3xl mx-auto text-center">
          {/* Icon */}
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary-foreground/10 backdrop-blur-sm border border-primary-foreground/20 mb-6">
            <Anchor className="w-8 h-8 text-primary-foreground" />
          </div>

          {/* Content */}
          <h2 className="font-display text-3xl md:text-4xl lg:text-5xl font-bold text-primary-foreground mb-6">
            Pronto para zarpar?
          </h2>
          <p className="text-primary-foreground/80 text-lg md:text-xl mb-10 max-w-xl mx-auto">
            Crie sua conta gratuitamente e comece a explorar os melhores barcos do Brasil.
          </p>

          {/* CTAs */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link href="/search">
            <Button variant="heroOutline" size="xl" className="group">
              Buscar Barcos
              <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
            </Button>
            </Link>
            <Link href="/register-boat">
            <Button 
              variant="hero" 
              size="xl" 
              className="bg-primary-foreground text-primary hover:bg-primary-foreground/90"
            >
              Anunciar Meu Barco
            </Button>
            </Link>
          </div>

          {/* Trust badges */}
          <p className="text-primary-foreground/60 text-sm mt-8">
            ✓ Cadastro gratuito • ✓ Pagamento seguro • ✓ Suporte 24/7
          </p>
        </div>
      </div>
    </section>
  );
};

export default CTASection;
