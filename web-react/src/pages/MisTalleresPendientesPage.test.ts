import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import MisTalleresPendientesPage from './MisTalleresPendientesPage';
import { apiGetEventos, request } from '../clients/api';

vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ getAccessTokenSilently: async () => 'token-demo' }) }));
vi.mock('../clients/api', () => ({ apiGetEventos: vi.fn(), request: vi.fn() }));
const getEventos = vi.mocked(apiGetEventos);
const api = vi.mocked(request);

beforeEach(() => { getEventos.mockReset(); api.mockReset(); });
afterEach(() => cleanup());

const tallerAbierto = { id: 1, title: 'Huerto comunitario', comuna: 'La Florida', fechaHora: '2026-11-20T10:00' };
const tallerRestringido = { id: 2, title: 'Taller de monitores', comuna: 'Santiago', fechaHora: '2026-11-21T10:00',
  rolesPermitidos: 'monitor', zonaRestringida: null };

it('muestra los talleres del catalogo y permite marcar que voy', async () => {
  getEventos.mockResolvedValue([tallerAbierto, tallerRestringido]);
  api.mockResolvedValueOnce([]).mockResolvedValueOnce({ id: 10, tallerId: 1, estado: 'pendiente' })
    .mockResolvedValueOnce([{ id: 10, tallerId: 1, estado: 'pendiente' }]);
  render(React.createElement(MisTalleresPendientesPage));

  expect(await screen.findByText('Huerto comunitario')).toBeTruthy();
  expect(screen.getByText('Taller de monitores')).toBeTruthy();
  expect(screen.getByText(/Solo: monitor/)).toBeTruthy();

  fireEvent.click(screen.getAllByRole('button', { name: 'Marcar que voy' })[0]);
  await waitFor(() => expect(api).toHaveBeenCalledWith('/api/eventos/1/inscripcion',
    expect.objectContaining({ method: 'POST', token: 'token-demo' })));
  expect(await screen.findByText('Pendiente de asistir')).toBeTruthy();
});

it('muestra el error de la API sin romper la pagina', async () => {
  getEventos.mockRejectedValue(new Error('Tu sesión expiró. Inicia sesión.'));
  render(React.createElement(MisTalleresPendientesPage));
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'Tu sesión expiró. Inicia sesión.');
});
