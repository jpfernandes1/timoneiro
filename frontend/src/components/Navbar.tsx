'use client';
import { useState, useEffect } from "react";
import { Menu, X, Anchor, User, LogOut, AlertCircle } from "lucide-react";
import { Button } from '@/src/components/ui/button'
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/src/contexts/AuthContext';

const Navbar = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [mounted, setMounted] = useState(false);
    const router = useRouter();
    
    const { user, isAuthenticated, logout } = useAuth();

    const navLinks = [
        {label: "Fazer uma Reserva", href: "/search" },
        {label: "Como funciona", href: "/#how-it-works" },
        {label: "Sobre", href: "/#about" },
    ];

    useEffect(() => {
        setMounted(true);
    }, []);

    const handleLogout = async () => {
        try {
            logout();
            setIsOpen(false);
        } catch (error) {
            console.error('Erro ao fazer logout:', error);
        }
    };

    const handleAnnounceBoatClick = (e: React.MouseEvent) => {
        if (!isAuthenticated) {
            e.preventDefault();
            setShowLoginModal(true);
            setIsOpen(false);
        } else {
            // If you are already logged in, go directly to the page.
            router.push('/register-boat');
        }
    };

    const handleNavigateToRegisterBoat = () => {
        // Stores the redirection intent.
        localStorage.setItem('redirectAfterLogin', '/register-boat');
        setShowLoginModal(false);
        router.push('/auth');
    };

    return(
        <>
            <nav className="fixed top-0 left-0 right-0 z-50 bg-background/80 backdrop-blur-md border-b border-border/50">
                <div className="container mx-auto px-4">
                    <div className="flex items-center justify-between h-16 lg:h-20">
                        {/* Logo */}
                        <Link href="/" className="flex items-center gap-2">
                            <div className="w-10 h-10 rounded-full gradient-ocean flex items-center justify-center">
                                <Anchor className="w-5 h-5 text-primary-foreground" />
                            </div>
                            <span className="font-display text-xl font-bold text-foreground">Timoneiro</span>
                        </Link>
                        
                        {/* Desktop Navigation */}
                        <div className="hidden md:flex items-center gap-8">
                            {navLinks.map((link) => (
                                <Link
                                    key={link.label}
                                    href={link.href}
                                    className="text-muted-foreground hover:text-foreground transition-colors font-medium"
                                >
                                    {link.label}
                                </Link>
                            ))}
                        </div>
                        
                        {/* Desktop CTA */}
                        <div className="hidden md:flex items-center gap-4">
                            {mounted && isAuthenticated ? (
                                <div className="flex items-center gap-3">
                                    <span className="text-sm text-muted-foreground">
                                        Olá, {user?.name?.split(' ')[0] || "Navegante"}
                                    </span>
                                    <Link href="/dashboard">
                                        <Button variant="outline" className="gap-2">
                                            <User className="w-4 h-4" />
                                            Minha Área
                                        </Button>
                                    </Link>
                                    <Button 
                                        variant="ghost" 
                                        onClick={handleLogout}
                                        className="gap-2 text-destructive hover:text-destructive hover:bg-destructive/10"
                                    >
                                        <LogOut className="w-4 h-4" />
                                        Sair
                                    </Button>
                                </div>
                            ) : (
                                <>
                                    <Link href="/auth">
                                        <Button variant="ghost">Entrar</Button>
                                    </Link>
                                    <Button 
                                        variant="ocean"
                                        onClick={handleAnnounceBoatClick}
                                    >
                                        Anunciar Barco
                                    </Button>
                                </>
                            )}
                        </div>

                        {/* Mobile Menu Button */}
                        <button
                            className="md:hidden p-2"
                            onClick={() => setIsOpen(!isOpen)}
                            aria-label="Toggle menu"
                        >
                            {isOpen ? (
                                <X className="w-6 h-6 text-foreground" />
                            ) : (
                                <Menu className="w-6 h-6 text-foreground" />
                            )}
                        </button>
                    </div>

                    {/* Mobile Navigation */}
                    {isOpen && (
                        <div className="md:hidden py-4 border-t border-border/50">
                            <div className="flex flex-col gap-4">
                                {navLinks.map((link) => (
                                    <Link
                                        key={link.label}
                                        href={link.href}
                                        className="text-muted-foreground hover:text-foreground transition-colors font-medium py-2"
                                        onClick={() => setIsOpen(false)}
                                    >
                                        {link.label}
                                    </Link>
                                ))}
                                
                                <div className="flex flex-col gap-2 pt-4 border-t border-border/50">
                                    {mounted && isAuthenticated ? (
                                        <>
                                            <div className="px-2 py-3 text-sm text-muted-foreground">
                                                Olá, {user?.name?.split(' ')[0] || "Navegante"}
                                            </div>
                                            <Link href="/dashboard" onClick={() => setIsOpen(false)}>
                                                <Button variant="outline" className="w-full justify-start gap-2">
                                                    <User className="w-4 h-4" />
                                                    Minha Área
                                                </Button>
                                            </Link>
                                            <Button 
                                                variant="ghost" 
                                                onClick={() => {
                                                    handleLogout();
                                                    setIsOpen(false);
                                                }}
                                                className="w-full justify-start gap-2 text-destructive hover:text-destructive"
                                            >
                                                <LogOut className="w-4 h-4" />
                                                Sair
                                            </Button>
                                        </>
                                    ) : (
                                        <>
                                            <Link href="/auth" onClick={() => setIsOpen(false)}>
                                                <Button variant="ghost" className="w-full justify-start">Entrar</Button>
                                            </Link>
                                            <Button 
                                                variant="ocean"
                                                onClick={handleAnnounceBoatClick}
                                                className="w-full"
                                            >
                                                Anunciar Barco
                                            </Button>
                                        </>
                                    )}
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </nav>

            {/* Required Login Method */}
            {showLoginModal && (
                <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                    <div className="bg-card rounded-2xl shadow-elevated p-6 max-w-md w-full animate-fade-up">
                        <div className="flex items-center gap-3 mb-4">
                            <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center">
                                <AlertCircle className="w-6 h-6 text-primary" />
                            </div>
                            <div>
                                <h3 className="font-display text-xl font-bold text-foreground">
                                    Login Necessário
                                </h3>
                                <p className="text-muted-foreground text-sm">
                                    Para anunciar seu barco, faça login ou cadastre-se
                                </p>
                            </div>
                        </div>
                        
                        <p className="text-foreground mb-6">
                            Você precisa estar logado para anunciar um barco no Timoneiro. 
                            Crie sua conta em menos de 2 minutos e comece a alugar seu barco!
                        </p>
                        
                        <div className="flex flex-col gap-3">
                            <Button 
                                variant="ocean" 
                                size="lg"
                                onClick={handleNavigateToRegisterBoat}
                                className="w-full"
                            >
                                Fazer Login / Cadastrar
                            </Button>
                            <Button 
                                variant="outline" 
                                onClick={() => setShowLoginModal(false)}
                                className="w-full"
                            >
                                Cancelar
                            </Button>
                        </div>
                        
                        <p className="text-xs text-muted-foreground text-center mt-6">
                            Ao fazer login, você será redirecionado para a página de cadastro do barco
                        </p>
                    </div>
                </div>
            )}
        </>
    );
};

export default Navbar;