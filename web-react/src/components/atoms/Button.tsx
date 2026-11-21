import type { ButtonHTMLAttributes } from 'react';

type Variant = 'primary' | 'secondary';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  /** Añade el modificador `btn-block` (ancho completo) usado en los formularios. */
  block?: boolean;
  /** Añade el modificador `btn-sm` usado en tarjetas compactas. */
  small?: boolean;
}

/**
 * Átomo: botón base de SyncPass. Centraliza las clases `btn btn-primary` /
 * `btn btn-secondary` que antes se repetían como strings sueltos en cada página.
 */
export default function Button({ variant = 'primary', block, small, className = '', type = 'button', ...rest }: ButtonProps) {
  const classes = ['btn', `btn-${variant}`, block ? 'btn-block' : '', small ? 'btn-sm' : '', className]
    .filter(Boolean)
    .join(' ');
  return <button type={type} className={classes} {...rest} />;
}
