import { Star, Quote } from "lucide-react";

const testimonials = [
  {
    id: 1,
    name: "Carlos Mendes",
    role: "Empresário",
    avatar: "CM",
    rating: 5,
    text: "Experiência incrível! O processo de reserva foi super simples e o barco estava impecável. Já estou planejando minha próxima viagem.",
  },
  {
    id: 2,
    name: "Ana Paula Santos",
    role: "Arquiteta",
    avatar: "AS",
    rating: 4,
    text: "Aluguei um veleiro para comemorar meu aniversário. O proprietário foi muito atencioso e a plataforma transmite muita segurança.",
  },
  {
    id: 3,
    name: "Ricardo Oliveira",
    role: "Médico",
    avatar: "RO",
    rating: 5,
    text: "Família inteira adorou! As crianças se divertiram muito e pudemos conhecer praias que só são acessíveis de barco.",
  },
];

const TestimonialsSection = () => {
  return (
    <section id="about" className="py-20 lg:py-28 bg-background">
      <div className="container mx-auto px-4">
        {/* Header */}
        <div className="text-center max-w-2xl mx-auto mb-16">
          <p className="text-primary font-medium mb-2">Depoimentos</p>
          <h2 className="font-display text-3xl md:text-4xl lg:text-5xl font-bold text-foreground mb-4">
            O que dizem nossos clientes
          </h2>
          <p className="text-muted-foreground text-lg">
            Milhares de experiências inesquecíveis em alto mar
          </p>
        </div>

        {/* Testimonials Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 lg:gap-8">
          {testimonials.map((testimonial, index) => (
            <div
              key={testimonial.id}
              className="relative bg-card rounded-2xl p-6 lg:p-8 shadow-card hover:shadow-elevated transition-all duration-300"
              style={{ animationDelay: `${index * 0.1}s` }}
            >
              {/* Quote Icon */}
              <Quote className="absolute top-6 right-6 w-8 h-8 text-primary/20" />

              {/* Rating */}
              <div className="flex gap-1 mb-4">
                {[...Array(testimonial.rating)].map((_, i) => (
                  <Star key={i} className="w-5 h-5 text-coral fill-current" />
                ))}
              </div>

              {/* Text */}
              <p className="text-card-foreground mb-6 leading-relaxed">
                "{testimonial.text}"
              </p>

              {/* Author */}
              <div className="flex items-center gap-3 pt-4 border-t border-border">
                <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center text-primary-foreground font-semibold">
                  {testimonial.avatar}
                </div>
                <div>
                  <p className="font-semibold text-card-foreground">{testimonial.name}</p>
                  <p className="text-sm text-muted-foreground">{testimonial.role}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default TestimonialsSection;
