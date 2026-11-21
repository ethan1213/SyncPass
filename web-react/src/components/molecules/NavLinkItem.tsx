import { Link } from 'wouter';
import Badge from '../atoms/Badge';

export interface NavLinkItemProps {
  href: string;
  label: string;
  active: boolean;
  /** Pendientes a mostrar en un Badge junto a la etiqueta (p. ej. cola de sincronización). */
  badgeCount?: number;
}

/**
 * Molécula: combina el átomo `Badge` con un `Link` de Wouter para formar un
 * ítem de navegación completo (enlace + estado activo + contador opcional).
 * Usada por el organismo `Navbar`.
 */
export default function NavLinkItem({ href, label, active, badgeCount = 0 }: NavLinkItemProps) {
  return (
    <Link href={href} className={`nav-link ${active ? 'active' : ''}`} aria-current={active ? 'page' : undefined}>
      {label}
      <Badge count={badgeCount} />
    </Link>
  );
}
