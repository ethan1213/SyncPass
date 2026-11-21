import type { ReactNode } from 'react';
import { useSession } from '../auth/SessionProvider';
import { useUserRoles } from '../hooks/useRoles';
import { formatRoleList } from '../utils/roles';
import Button from './atoms/Button';

export interface RequireAuthProps {
  roles?: string[];
  children: ReactNode;
}

/**
 * Envuelve rutas que exigen sesion y, opcionalmente, alguno de los roles.
 *
 * OJO: el frontend solo oculta UI. La autorizacion real la valida la API con el
 * access token.
 */
export default function RequireAuth({ roles, children }: RequireAuthProps) {
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
        <Button onClick={() => loginWithRedirect()}>Iniciar sesión</Button>
      </div>
    );
  }

  if (roles && !roles.some((r) => userRoles.includes(r))) {
    return (
      <div className="contenedor estado-bloque">
        <h2>Acceso denegado</h2>
        <p>
          Esta sección requiere el perfil{' '}
          <strong>{formatRoleList(roles, ' o ')}</strong>.
        </p>
        <p className="texto-apoyo">
          Tu sesión tiene:{' '}
          {userRoles.length ? formatRoleList(userRoles, ', ') : 'ningún rol asignado'}.
        </p>
      </div>
    );
  }

  return <>{children}</>;
}
