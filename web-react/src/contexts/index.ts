/**
 * Capa de contextos de React. SyncPass tiene un único contexto real de sesión
 * (`SessionProvider`, en `src/auth`, junto al resto del código de autenticación).
 * Este módulo lo re-exporta bajo `src/contexts` para que el proyecto tenga una
 * capa arquitectónica llamada "contexts" claramente identificable, sin duplicar
 * su lógica.
 */
export { SessionProvider, useSession } from '../auth/SessionProvider';
