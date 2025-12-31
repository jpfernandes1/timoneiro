import { Anchor, Facebook, Instagram, Youtube, Mail, Phone, MapPin } from "lucide-react";

const Footer = () => {
  const footerLinks = {
    platform: [
      { label: "Como Funciona", href: "#" },
      { label: "Buscar Barcos", href: "#" },
      { label: "Anunciar Barco", href: "#" },
      { label: "Preços", href: "#" },
    ],
    support: [
      { label: "Central de Ajuda", href: "#" },
      { label: "Segurança", href: "#" },
      { label: "Política de Cancelamento", href: "#" },
      { label: "FAQ", href: "#" },
    ],
    company: [
      { label: "Sobre Nós", href: "#" },
      { label: "Carreiras", href: "#" },
      { label: "Blog", href: "#" },
      { label: "Imprensa", href: "#" },
    ],
  };

  return (
    <footer className="bg-ocean-deep text-primary-foreground">
      <div className="container mx-auto px-4 py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-10 lg:gap-8">
          {/* Brand */}
          <div className="lg:col-span-2">
            <a href="/" className="flex items-center gap-2 mb-4">
              <div className="w-10 h-10 rounded-full bg-primary-foreground/10 border border-primary-foreground/20 flex items-center justify-center">
                <Anchor className="w-5 h-5 text-primary-foreground" />
              </div>
              <span className="font-display text-xl font-bold">Timoneiro</span>
            </a>
            <p className="text-primary-foreground/70 mb-6 max-w-sm">
              A plataforma de aluguel de barcos mais confiável do Brasil. Conectando proprietários e navegantes desde 2025.
            </p>
            
            {/* Contact */}
            <div className="space-y-2 text-sm text-primary-foreground/70">
              <p className="flex items-center gap-2">
                <Mail className="w-4 h-4" />
                contato@timoneiro.com.br
              </p>
              <p className="flex items-center gap-2">
                <Phone className="w-4 h-4" />
                (99) 99999-9999
              </p>
              <p className="flex items-center gap-2">
                <MapPin className="w-4 h-4" />
                Sede em Maranhão, Brasil
              </p>
            </div>
          </div>

          {/* Platform Links */}
          <div>
            <h4 className="font-semibold mb-4">Plataforma</h4>
            <ul className="space-y-3">
              {footerLinks.platform.map((link) => (
                <li key={link.label}>
                  <a href={link.href} className="text-primary-foreground/70 hover:text-primary-foreground transition-colors text-sm">
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          {/* Support Links */}
          <div>
            <h4 className="font-semibold mb-4">Suporte</h4>
            <ul className="space-y-3">
              {footerLinks.support.map((link) => (
                <li key={link.label}>
                  <a href={link.href} className="text-primary-foreground/70 hover:text-primary-foreground transition-colors text-sm">
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          {/* Company Links */}
          <div>
            <h4 className="font-semibold mb-4">Empresa</h4>
            <ul className="space-y-3">
              {footerLinks.company.map((link) => (
                <li key={link.label}>
                  <a href={link.href} className="text-primary-foreground/70 hover:text-primary-foreground transition-colors text-sm">
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* Bottom */}
        <div className="flex flex-col md:flex-row items-center justify-between gap-4 pt-10 mt-10 border-t border-primary-foreground/10">
          <p className="text-primary-foreground/50 text-sm">
            © 2025 Timoneiro. Todos os direitos reservados.
          </p>
          
          {/* Social Links */}
          <div className="flex items-center gap-4">
            <a href="#" className="w-10 h-10 rounded-full bg-primary-foreground/10 flex items-center justify-center hover:bg-primary-foreground/20 transition-colors">
              <Facebook className="w-5 h-5" />
            </a>
            <a href="#" className="w-10 h-10 rounded-full bg-primary-foreground/10 flex items-center justify-center hover:bg-primary-foreground/20 transition-colors">
              <Instagram className="w-5 h-5" />
            </a>
            <a href="#" className="w-10 h-10 rounded-full bg-primary-foreground/10 flex items-center justify-center hover:bg-primary-foreground/20 transition-colors">
              <Youtube className="w-5 h-5" />
            </a>
          </div>

          {/* Legal Links */}
          <div className="flex items-center gap-4 text-sm text-primary-foreground/50">
            <a href="#" className="hover:text-primary-foreground transition-colors">Termos</a>
            <a href="#" className="hover:text-primary-foreground transition-colors">Privacidade</a>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
