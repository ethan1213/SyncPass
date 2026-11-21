/**
 * Detecta el mensaje de conflicto de control optimista ("El registro cambió
 * o ya existe...") que devuelve la API cuando otra persona editó el mismo
 * recurso primero. Se repetía en ProyectosAdminPage y TalleresAdminPage.
 */
export const isStaleVersionConflict = (message: string): boolean => message.includes('cambió');
