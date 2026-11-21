import type { HTMLAttributes } from 'react';

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  /** Número a mostrar dentro del badge. Si es 0 o menor, el átomo no se renderiza. */
  count: number;
  tone?: 'alerta';
}

/**
 * Átomo: contador pequeño usado sobre enlaces (p. ej. pendientes de
 * sincronización en la Navbar). Reemplaza el `<span className="badge-cart ...">`
 * repetido manualmente.
 */
export default function Badge({ count, tone = 'alerta', className = '', ...rest }: BadgeProps) {
  if (count <= 0) return null;
  return (
    <span className={`badge-cart badge-${tone} ${className}`.trim()} {...rest}>
      {count}
    </span>
  );
}
