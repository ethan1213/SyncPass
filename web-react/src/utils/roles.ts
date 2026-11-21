import { roleLabel } from '../config/auth';

/**
 * Traduce una lista de roles crudos (grupos de Cognito) a etiquetas legibles
 * y las une con un separador. Reemplaza el `roles.map(roleLabel).join(...)`
 * que se repetía en Navbar, LoginPage y RequireAuth.
 */
export const formatRoleList = (roles: string[], separator = ' · '): string =>
  roles.map(roleLabel).join(separator);
