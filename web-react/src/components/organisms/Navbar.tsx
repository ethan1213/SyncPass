import { useEffect, useState } from 'react';
import { Link, useLocation } from 'wouter';
import { useSession } from '../../auth/SessionProvider';
import { useUserRoles, useHasRole } from '../../hooks/useRoles';
import { ROLES } from '../../config/auth';
import { formatRoleList } from '../../utils/roles';
import NavLinkItem from '../molecules/NavLinkItem';

interface EnlaceNav {
  href: string;
  label: string;
  roles?: string[];
  badge?: boolean;
}

const ENLACES: EnlaceNav[] = [
  { href: '/', label: 'Inicio' },
  { href: '/mi-panel', label: 'Mi panel' },
  { href: '/proyectos', label: 'Proyectos', roles: [ROLES.admin, ROLES.coordinator] },
  { href: '/talleres', label: 'Gestionar talleres', roles: [ROLES.admin, ROLES.coordinator] },
  { href: '/acreditar', label: 'Acreditación', roles: [ROLES.admin, ROLES.coordinator, ROLES.volunteer] },
  { href: '/mi-qr', label: 'Mi QR', roles: [ROLES.attendee] },
  { href: '/mis-talleres', label: 'Mis talleres pendientes', roles: [ROLES.attendee] },
  { href: '/encuesta', label: 'Encuestas' },
  { href: '/sincronizacion', label: 'Sincronización', badge: true },
  { href: '/panel', label: 'Panel', roles: [ROLES.coordinator, ROLES.admin] },
];

export interface NavbarProps {
  offlineCount?: number;
  isOfflineMode?: boolean;
}

/**
 * Organismo: cabecera completa de SyncPass. Se compone de la molécula
 * `NavLinkItem` (que a su vez usa el átomo `Badge`) para cada enlace visible.
 */
export default function Navbar({ offlineCount = 0, isOfflineMode }: NavbarProps) {
  const [location] = useLocation();
  const [abierto, setAbierto] = useState(false);
  const { isAuthenticated, isLoading, user, loginWithRedirect, logout } = useSession();
  const roles = useUserRoles();
  const esGestor = useHasRole(ROLES.admin, ROLES.coordinator);

  // Cerrar el menu al navegar: en movil el panel desplegado tapa la pagina completa.
  useEffect(() => {
    setAbierto(false);
  }, [location]);

  const visibles = ENLACES.filter((e) => !e.roles || e.roles.some((r) => roles.includes(r)));

  return (
    <header>
      <div className="contenedor cabecera">
          <Link href="/" className="marca">
          <img src="/img/logo-header.svg" alt="SyncPass" className="marca-logo logo-claro" />
          <img src="/img/logo-header-dark.svg" alt="" aria-hidden="true" className="marca-logo logo-oscuro" />
          <span className="marca-producto">SyncPass</span>
          <span style={{ marginLeft: '0.5rem', fontSize: '0.75rem', opacity: 0.7 }}>· Ethan Astorga</span>
        </Link>

        <div className="cabecera-acciones">
          {isOfflineMode && (
            <span className="pill pill-alerta" title="Trabajando sin conexión">
              Sin conexión
            </span>
          )}
          {esGestor && (
            <span className="pill pill-ok" title="Tu cuenta tiene permisos de administrador o coordinador">
              Gestión
            </span>
          )}

          <button
            className="boton-menu"
            aria-expanded={abierto}
            aria-controls="menu-principal"
            onClick={() => setAbierto((v) => !v)}
          >
            <span className="visualmente-oculto">{abierto ? 'Cerrar menú' : 'Abrir menú'}</span>
            <span aria-hidden="true">{abierto ? '✕' : '☰'}</span>
            {!abierto && offlineCount > 0 && <span className="punto-aviso" aria-hidden="true" />}
          </button>
        </div>

        <nav
          id="menu-principal"
          className={`menu-principal ${abierto ? 'abierto' : ''}`}
          aria-label="Navegación principal"
        >
          {visibles.map((e) => (
            <NavLinkItem
              key={e.href}
              href={e.href}
              label={e.label}
              active={location === e.href}
              badgeCount={e.badge ? offlineCount : 0}
            />
          ))}

          {isLoading ? (
            <span className="nav-link nav-link-estatico">Cargando…</span>
          ) : isAuthenticated ? (
            <>
              <Link
                href="/mi-panel"
                className="nav-link nav-usuario"
                title={formatRoleList(roles)}
              >
                {(user?.name as string | undefined) ?? user?.email}
              </Link>
              <button
                className="nav-link"
                onClick={() => logout()}
              >
                Salir
              </button>
            </>
          ) : (
            <button className="nav-link" onClick={() => loginWithRedirect()}>
              Iniciar sesión
            </button>
          )}
        </nav>
      </div>
    </header>
  );
}
