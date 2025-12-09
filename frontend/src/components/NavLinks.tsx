"use client";

import { ReactNode } from "react";
import Link, { LinkProps } from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/src/lib/utils"; // Ajuste o caminho conforme seu projeto

interface CustomNavLinkProps extends Omit<LinkProps, "href"> {
  href: string;
  children: ReactNode;
  className?: string;
  activeClassName?: string;
  inactiveClassName?: string;
  exact?: boolean;
  // Suporta os parâmetros específicos do seu design
  variant?: "default" | "ocean" | "ghost";
  size?: "sm" | "md" | "lg";
}

export default function NavLink({
  href,
  children,
  className = "",
  activeClassName = "text-primary font-semibold border-b-2 border-primary",
  inactiveClassName = "text-muted-foreground hover:text-foreground",
  exact = false,
  variant = "default",
  size = "md",
  ...props
}: CustomNavLinkProps) {
  const pathname = usePathname();
  
  // Lógica para determinar se o link está ativo
  const isActive = exact 
    ? pathname === href
    : pathname?.startsWith(href) || false;

  // Classes base por variante
  const variantClasses = {
    default: "px-4 py-2 rounded-lg",
    ocean: "px-4 py-2 rounded-lg bg-gradient-ocean text-white shadow-soft",
    ghost: "px-4 py-2 rounded-lg hover:bg-accent/50"
  };

  // Classes por tamanho
  const sizeClasses = {
    sm: "text-sm",
    md: "text-base",
    lg: "text-lg font-medium"
  };

  return (
    <Link
      href={href}
      className={cn(
        "transition-all duration-300 inline-flex items-center",
        variantClasses[variant],
        sizeClasses[size],
        isActive ? activeClassName : inactiveClassName,
        className
      )}
      aria-current={isActive ? "page" : undefined}
      {...props}
    >
      {children}
    </Link>
  );
}

// Componente de grupo de navegação
export function NavLinksGroup({ children, className }: { 
  children: ReactNode; 
  className?: string;
}) {
  return (
    <nav className={cn("flex items-center space-x-6", className)}>
      {children}
    </nav>
  );
}