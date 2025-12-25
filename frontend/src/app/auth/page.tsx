"use client";

import { useState, useEffect, useRef } from "react";
import { useRouter } from 'next/navigation';
import { useAuth } from '@/src/contexts/AuthContext'
import { Button } from "@/src/components/ui/button";
import { Input } from "@/src/components/ui/input";
import { Label } from "@/src/components/ui/label";
import { Anchor, Mail, Lock, User, ArrowLeft, Eye, EyeOff, Phone } from "lucide-react";
import Link from 'next/link';

const Auth = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [cpf, setCpf] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  
  const { login, register } = useAuth();
  const router = useRouter();
  const cpfInputRef = useRef<HTMLInputElement>(null);
  const phoneInputRef = useRef<HTMLInputElement>(null);

  // Função para formatar CPF
  const formatCPF = (value: string): string => {
    const numbers = value.replace(/\D/g, '');
    const limited = numbers.substring(0, 11);
    
    if (limited.length <= 3) {
      return limited;
    } else if (limited.length <= 6) {
      return `${limited.substring(0, 3)}.${limited.substring(3)}`;
    } else if (limited.length <= 9) {
      return `${limited.substring(0, 3)}.${limited.substring(3, 6)}.${limited.substring(6)}`;
    } else {
      return `${limited.substring(0, 3)}.${limited.substring(3, 6)}.${limited.substring(6, 9)}-${limited.substring(9, 11)}`;
    }
  };

  // Função para formatar Telefone
  const formatPhone = (value: string): string => {
    // Remove tudo que não é número
    const numbers = value.replace(/\D/g, '');
    
    // Limita a 11 dígitos (DDD + 9 dígitos)
    const limited = numbers.substring(0, 11);
    
    // Aplica a máscara (XX) X XXXX XXXX
    if (limited.length <= 2) {
      return limited;
    } else if (limited.length <= 3) {
      return `(${limited.substring(0, 2)}) ${limited.substring(2)}`;
    } else if (limited.length <= 7) {
      return `(${limited.substring(0, 2)}) ${limited.substring(2, 3)} ${limited.substring(3)}`;
    } else if (limited.length <= 11) {
      return `(${limited.substring(0, 2)}) ${limited.substring(2, 3)} ${limited.substring(3, 7)} ${limited.substring(7)}`;
    }
    return `(${limited.substring(0, 2)}) ${limited.substring(2, 3)} ${limited.substring(3, 7)} ${limited.substring(7, 11)}`;
  };

  // Função para lidar com a mudança do CPF
  const handleCpfChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawValue = e.target.value;
    const formattedValue = formatCPF(rawValue);
    setCpf(formattedValue);
  };

  // Função para lidar com a mudança do Telefone
  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawValue = e.target.value;
    const formattedValue = formatPhone(rawValue);
    setPhone(formattedValue);
  };

  // Funções para lidar com teclas pressionadas
  const handleCpfKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    const allowedKeys = [
      'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
      'Delete', 'Backspace', 'Tab', 'Home', 'End'
    ];
    
    if (!/^\d$/.test(e.key) && !allowedKeys.includes(e.key) && !e.ctrlKey && !e.metaKey) {
      e.preventDefault();
    }
  };

  const handlePhoneKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    const allowedKeys = [
      'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
      'Delete', 'Backspace', 'Tab', 'Home', 'End'
    ];
    
    if (!/^\d$/.test(e.key) && !allowedKeys.includes(e.key) && !e.ctrlKey && !e.metaKey) {
      e.preventDefault();
    }
  };

  // Funções para lidar com colar
  const handleCpfPaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const pastedText = e.clipboardData.getData('text');
    const numbers = pastedText.replace(/\D/g, '');
    const formatted = formatCPF(numbers);
    setCpf(formatted);
  };

  const handlePhonePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const pastedText = e.clipboardData.getData('text');
    const numbers = pastedText.replace(/\D/g, '');
    const formatted = formatPhone(numbers);
    setPhone(formatted);
  };

  // Focar automaticamente no próximo campo
  useEffect(() => {
    if (cpf.length === 14 && cpfInputRef.current) {
      const nextInput = cpfInputRef.current.closest('div')?.nextElementSibling?.querySelector('input');
      if (nextInput) {
        (nextInput as HTMLInputElement).focus();
      }
    }
  }, [cpf]);

  useEffect(() => {
    if (phone.length === 16 && phoneInputRef.current) { // 16 = (XX) X XXXX XXXX
      // Aqui você pode focar no próximo campo se quiser
    }
  }, [phone]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      // Remove formatação antes de enviar
      const cleanCpf = cpf.replace(/\D/g, '');
      const cleanPhone = phone.replace(/\D/g, '');
      
      if (isLogin) {
        await login(email, password);
      } else {
        await register({
          name,
          email,
          password,
          cpf: cleanCpf,
          phone: cleanPhone
        });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ocorreu um erro. Tente novamente.");
    } finally {
      setIsLoading(false);
    }
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  return (
    <div className="min-h-screen gradient-sky flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <Link
          href="/"
          className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors mb-8"
        >
          <ArrowLeft className="w-4 h-4" />
          Voltar para Home
        </Link>

        <div className="bg-card rounded-2xl shadow-elevated p-8 animate-fade-up">
          <div className="flex items-center justify-center gap-2 mb-8">
            <div className="w-12 h-12 rounded-full gradient-ocean flex items-center justify-center">
              <Anchor className="w-6 h-6 text-primary-foreground" />
            </div>
            <span className="font-display text-2xl font-bold text-foreground">
              Timoneiro
            </span>
          </div>

          <h1 className="font-display text-2xl font-bold text-center text-foreground mb-2">
            {isLogin ? "Bem-vindo de volta!" : "Crie sua conta"}
          </h1>
          <p className="text-muted-foreground text-center mb-8">
            {isLogin
              ? "Entre para continuar sua jornada"
              : "Cadastre-se para começar a navegar"}
          </p>

          {error && (
            <div className="mb-4 p-3 bg-red-50 text-red-600 rounded-lg text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {!isLogin && (
              <>
                <div className="space-y-2">
                  <Label htmlFor="name">Nome completo</Label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                    <Input
                      id="name"
                      type="text"
                      placeholder="Seu nome"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      className="pl-10"
                      required={!isLogin}
                    />
                  </div>
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="cpf">CPF</Label>
                  <Input
                    ref={cpfInputRef}
                    id="cpf"
                    type="text"
                    placeholder="000.000.000-00"
                    value={cpf}
                    onChange={handleCpfChange}
                    onKeyDown={handleCpfKeyDown}
                    onPaste={handleCpfPaste}
                    inputMode="numeric"
                    pattern="\d{3}\.\d{3}\.\d{3}-\d{2}"
                    title="Formato: 000.000.000-00"
                    required={!isLogin}
                    className="tracking-wide"
                  />
                  {cpf && cpf.length > 0 && cpf.length < 14 && !isLogin && (
                    <p className="text-xs text-muted-foreground mt-1">
                      {14 - cpf.length} dígito(s) restante(s)
                    </p>
                  )}
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="phone">Telefone</Label>
                  <div className="relative">
                    <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                    <Input
                      ref={phoneInputRef}
                      id="phone"
                      type="text"
                      placeholder="(XX) X XXXX XXXX"
                      value={phone}
                      onChange={handlePhoneChange}
                      onKeyDown={handlePhoneKeyDown}
                      onPaste={handlePhonePaste}
                      inputMode="tel"
                      pattern="\(\d{2}\) \d \d{4} \d{4}"
                      title="Formato: (XX) X XXXX XXXX"
                      required={!isLogin}
                      className="pl-10 tracking-wide"
                    />
                  </div>
                  {phone && phone.length > 0 && phone.length < 16 && !isLogin && (
                    <p className="text-xs text-muted-foreground mt-1">
                      {16 - phone.length} dígito(s) restante(s)
                    </p>
                  )}
                </div>
              </>
            )}

            <div className="space-y-2">
              <Label htmlFor="email">E-mail</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input
                  id="email"
                  type="email"
                  placeholder="seu@email.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="pl-10"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Senha</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="pl-10 pr-10"
                  required
                />
                <button
                  type="button"
                  onClick={togglePasswordVisibility}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors focus:outline-none"
                  aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                >
                  {showPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
            </div>

            {isLogin && (
              <div className="text-right">
                <a
                  href="#"
                  className="text-sm text-primary hover:text-primary/80 transition-colors"
                >
                  Esqueceu a senha?
                </a>
              </div>
            )}

            <Button 
              type="submit" 
              variant="ocean" 
              className="w-full" 
              size="lg"
              disabled={isLoading}
            >
              {isLoading ? "Processando..." : isLogin ? "Entrar" : "Criar conta"}
            </Button>
          </form>

          <div className="relative my-8">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-border"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="bg-card px-4 text-muted-foreground">ou</span>
            </div>
          </div>

          <p className="text-center text-muted-foreground">
            {isLogin ? "Não tem uma conta?" : "Já tem uma conta?"}{" "}
            <button
              type="button"
              onClick={() => {
                setIsLogin(!isLogin);
                setError("");
                setCpf("");
                setPhone("");
              }}
              className="text-primary font-medium hover:text-primary/80 transition-colors"
              disabled={isLoading}
            >
              {isLogin ? "Cadastre-se" : "Entrar"}
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Auth;