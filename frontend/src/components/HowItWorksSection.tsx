import { Search, CalendarCheck, Anchor, Shield } from "lucide-react";

const steps = [
  {
    icon: Search,
    title: "Busque",
    description: "Encontre o barco perfeito filtrando por destino, data e tipo de embarcação.",
  },
  {
    icon: CalendarCheck,
    title: "Reserve",
    description: "Escolha suas datas, converse com o proprietário e confirme sua reserva online.",
  },
  {
    icon: Shield,
    title: "Pague com Segurança",
    description: "Pagamento protegido. Você só paga após a confirmação do proprietário.",
  },
  {
    icon: Anchor,
    title: "Navegue",
    description: "Aproveite sua experiência! Com ou sem capitão, você decide.",
  },
];

const HowItWorksSection = () => {
  return (
    <section id="how-it-works" className="py-20 lg:py-28 gradient-sky">
      <div className="container mx-auto px-4">
        {/* Header */}
        <div className="text-center max-w-2xl mx-auto mb-16">
          <p className="text-primary font-medium mb-2">Simples e Rápido</p>
          <h2 className="font-display text-3xl md:text-4xl lg:text-5xl font-bold text-foreground mb-4">
            Como Funciona
          </h2>
          <p className="text-muted-foreground text-lg">
            Em poucos passos você está navegando. Sem burocracia, sem complicação.
          </p>
        </div>

        {/* Steps */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {steps.map((step, index) => (
            <div
              key={step.title}
              className="relative text-center group"
            >
              {/* Connector Line */}
              {index < steps.length - 1 && (
                <div className="hidden lg:block absolute top-12 left-1/2 w-full h-0.5 bg-linear-to-r from-primary/30 to-primary/10" />
              )}

              {/* Icon */}
              <div className="relative inline-flex items-center justify-center w-24 h-24 rounded-2xl gradient-ocean shadow-card mb-6 group-hover:scale-110 transition-transform duration-300">
                <step.icon className="w-10 h-10 text-primary-foreground" />
                <span className="absolute -top-2 -right-2 w-8 h-8 rounded-full bg-accent text-accent-foreground text-sm font-bold flex items-center justify-center shadow-soft">
                  {index + 1}
                </span>
              </div>

              {/* Content */}
              <h3 className="font-display text-xl font-semibold text-foreground mb-3">
                {step.title}
              </h3>
              <p className="text-muted-foreground">
                {step.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default HowItWorksSection;
