'use client';
import { useState } from "react";
import { Menu, X, Anchor } from "lucide-react";
import { Button } from '@/src/components/ui/button'
import Link from 'next/link';


const Navbar = () => {
    const [isOpen, setIsOpen] = useState(false);

    const navLinks = [
        {label: "Fazer uma Reserva", href: "/search" },
        {label: "Como funciona", href: "/#how-it-works" },
        {label: "Sobre", href: "/#about" },
    ];

    return(
        <nav className="fixed top-0 left-0 right-0 z-50 bg-background/80 backdrop-blur-md border-b border-border/50">
            <div className="container mx-auto px-4">
                <div className="flex items-center justify-between h-16 lg:h-20">
                    {/*Logo*/}
                    <a href="/" className="flex items-center gap-2">
                    <div className="w-10 h-10 rounded-full gradient-ocean flex items-center justify-center">
                        <Anchor className="w-5 h-5 text-primary-foreground" />
                    </div>
                    <span className="font-display text-x1 font-bold text-foreground"> Timoneiro </span>
                    </a>
                    { /* Desktop Navigation */}
                    <div className="hidden md:flex item-center gap-8">
                        {navLinks.map((link) => (
                            <a
                            key={link.label}
                            href={link.href}
                            className="text-muted-foreground hover:text-foreground transition-colors font-medium"
                            >
                                {link.label}
                            </a>
                        ))}
                    </div>
                    { /* Desktop CTA */ }
                    <div className="hidden md:flex items-center gat-4">
                        <Link href="/auth">
                        <Button variant="ghost">Entrar</Button>
                        </Link>
                        <Link href='/register-boat'>
                        <Button variant="ocean" >Anunciar Barco</Button>
                        </Link>
                    </div>

                    { /* Mobile Menu Button */ }
                    <button
                        className="md:hidden p-2"
                        onClick={() => setIsOpen(!isOpen)}
                        aria-label="Toogle menu"
                    >
                        {isOpen? (
                            <X className="w-6 h-6 text-foreground" />
                        ) : (
                            <Menu className="w-6 h-6 text-foreground" />
                        )}
                    </button>
                </div>

                { /* Mobile Navigation */}
                {isOpen && (
                     <div className="md:hidden py-4 border-t border-border/50">
            <div className="flex flex-col gap-4">
              {navLinks.map((link) => (
                <a
                  key={link.label}
                  href={link.href}
                  className="text-muted-foreground hover:text-foreground transition-colors font-medium py-2"
                  onClick={() => setIsOpen(false)}
                >
                  {link.label}
                </a>
              ))}
              <div className="flex flex-col gap-2 pt-4 border-t border-border/50">
                <Button variant="ghost" className="justify-start">Entrar</Button>
                <Button variant="ocean">Anunciar Barco</Button>
              </div>
            </div>
          </div>
        )}
            </div>
        </nav>
  );
};

export default Navbar;