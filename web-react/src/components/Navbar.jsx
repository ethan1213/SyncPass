import React, { useEffect, useState } from 'react';
import { Link, useLocation } from 'wouter';
import { useSession } from '../auth/SessionProvider';
import { useUserRoles } from '../hooks/useRoles.js';
import { ROLES, roleLabel } from '../config/auth.js';

const ENLACES = [
  { href: '/', label: 'Inicio' },
  { href: '/mi-panel', label: 'Mi panel' },
  { href: '/proyectos', label: 'Proyectos', roles: [ROLES.admin, ROLES.coordinator] },
  { href: '/talleres', label: 'Gestionar talleres', roles: [ROLES.admin, ROLES.coordinator] },
  { href: '/acreditar', label: 'Acreditación', roles: [ROLES.admin, ROLES.coordinator, ROLES.volunteer] },
  { href: '/mi-qr', label: 'Mi QR', roles: [ROLES.attendee] },
  { href: '/encuesta', label: 'Encuestas' },
  { href: '/sincronizacion', label: 'Sincronización', badge: true },
  { href: '/panel', label: 'Panel', roles: [ROLES.coordinator, ROLES.admin] },
];

export default function Navbar({ offlineCount = 0, isOfflineMode }) {
  const [location] = useLocation();
  const [abierto, setAbierto] = useState(false);
  const { isAuthenticated, isLoading, user, loginWithRedirect, logout } = useSession();
  const roles = useUserRoles();

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
        </Link>

        <div className="cabecera-acciones">
          {isOfflineMode && (
            <span className="pill pill-alerta" title="Trabajando sin conexión">
              Sin conexión
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
            <Link
              key={e.href}
              href={e.href}
              className={`nav-link ${location === e.href ? 'active' : ''}`}
              aria-current={location === e.href ? 'page' : undefined}
            >
              {e.label}
              {e.badge && offlineCount > 0 && (
                <span className="badge-cart badge-alerta">{offlineCount}</span>
              )}
            </Link>
          ))}

          {isLoading ? (
            <span className="nav-link nav-link-estatico">Cargando…</span>
          ) : isAuthenticated ? (
            <>
              <Link
                href="/mi-panel"
                className="nav-link nav-usuario"
                title={roles.map(roleLabel).join(' · ')}
              >
                {user?.name ?? user?.email}
              </Link>
              <button
                className="nav-link"
                onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
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
