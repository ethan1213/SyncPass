import React from 'react';
import { useSession } from '../auth/SessionProvider';
import { useUserRoles } from '../hooks/useRoles.js';
import { roleLabel } from '../config/auth.js';

/**
 * Envuelve rutas que exigen sesion y, opcionalmente, alguno de los roles.
 *
 * OJO: el frontend solo oculta UI. La autorizacion real la valida la API con el
 * access token.
 */
export default function RequireAuth({ roles, children }) {
  const { isLoading, isAuthenticated, loginWithRedirect } = useSession();
  const userRoles = useUserRoles();

  if (isLoading) {
    return (
      <div className="contenedor estado-bloque" role="status" aria-live="polite">
        <p>Verificando tu sesión…</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="contenedor estado-bloque">
        <h2>Necesitas iniciar sesión</h2>
        <p>Esta sección está reservada para el equipo acreditado de SyncPass.</p>
        <button className="btn btn-primary" onClick={() => loginWithRedirect()}>
          Iniciar sesión con Amazon Cognito
        </button>
      </div>
    );
  }

  if (roles && !roles.some((r) => userRoles.includes(r))) {
    return (
      <div className="contenedor estado-bloque">
        <h2>Acceso denegado</h2>
        <p>
          Esta sección requiere el perfil{' '}
          <strong>{roles.map(roleLabel).join(' o ')}</strong>.
        </p>
        <p className="texto-apoyo">
          Tu sesión tiene:{' '}
          {userRoles.length ? userRoles.map(roleLabel).join(', ') : 'ningún rol asignado'}.
        </p>
      </div>
    );
  }

  return <>{children}</>;
}
