import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import BitacoraPage from './BitacoraPage';
import { request } from '../clients/api';

const token = async () => 'token-demo';
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ getAccessTokenSilently: token }) }));
vi.mock('../hooks/useRoles', () => ({ useUserRoles: () => ['admin'] }));
vi.mock('../clients/api', () => ({ request: vi.fn() }));
vi.mock('wouter', () => ({ Link: ({ children, href }: { children: React.ReactNode; href: string }) => React.createElement('a', { href }, children) }));
const api = vi.mocked(request);
beforeEach(() => api.mockReset());
afterEach(() => cleanup());

it('publica una bitácora y consulta una respuesta con fuente por el mismo tallerId', async () => {
  api.mockImplementation(async (path, options) => {
    if (String(path).endsWith('/preguntas')) return { respuesta: 'Lavamos los envases.', fragmento: 2, cita: 'Lavamos los envases.' };
    if (options?.method === 'PUT') return { contenido: 'Separamos papel. Lavamos los envases.', resumen: 'Separamos papel.', fragmentos: ['Separamos papel.', 'Lavamos los envases.'] };
    return { contenido: 'Separamos papel.', resumen: 'Separamos papel.', fragmentos: ['Separamos papel.'] };
  });
  render(React.createElement(BitacoraPage, { tallerId: '42' }));
  await waitFor(() => expect(screen.getByLabelText('Transcripción o apuntes')).toHaveProperty('value', 'Separamos papel.'));
  fireEvent.change(screen.getByLabelText('Transcripción o apuntes'), { target: { value: 'Separamos papel. Lavamos los envases.' } });
  fireEvent.click(screen.getByRole('button', { name: 'Guardar y resumir' }));
  await waitFor(() => expect(api).toHaveBeenCalledWith('/api/eventos/42/bitacora', expect.objectContaining({ method: 'PUT', token: 'token-demo' })));
  fireEvent.change(screen.getByLabelText('Tu pregunta'), { target: { value: '¿Qué hacemos con los envases?' } });
  fireEvent.click(screen.getByRole('button', { name: 'Buscar respuesta' }));
  await waitFor(() => expect(api).toHaveBeenCalledWith('/api/eventos/42/bitacora/preguntas', expect.objectContaining({ method: 'POST', body: { pregunta: '¿Qué hacemos con los envases?' } })));
  expect(await screen.findByText('Fuente: fragmento 2 del contenido guardado.')).toBeTruthy();
});
