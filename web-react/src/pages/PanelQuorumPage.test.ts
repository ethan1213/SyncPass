import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import PanelQuorumPage from './PanelQuorumPage';
import { request } from '../clients/api';
import type { Taller } from '../types';

const token = async () => 'token-demo';
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ getAccessTokenSilently: token }) }));
vi.mock('../clients/api', () => ({ request: vi.fn() }));
const api = vi.mocked(request);

beforeEach(() => api.mockReset());
afterEach(() => cleanup());

const taller: Taller = {
  id: 1, title: 'Reciclaje', descripcion: '', area: '', comuna: '', unidadVecinal: '', direccion: '',
  fechaHora: '', docenteLider: '', cuposMaximos: 20, inscritos: 0, quorumPct: 0, image: '', asistentes: [],
};

it('carga el quórum al montar y permite actualizarlo', async () => {
  api.mockResolvedValue([{ id: 1, asistentes: 7 }]);
  render(React.createElement(PanelQuorumPage, { talleres: [taller] }));
  await waitFor(() => expect(screen.getByText(/7 asistencias confirmadas/)).toBeTruthy());
  fireEvent.click(screen.getByRole('button', { name: 'Actualizar' }));
  await waitFor(() => expect(api).toHaveBeenCalledTimes(2));
  expect(api).toHaveBeenCalledWith('/api/quorum', { token: 'token-demo' });
});

it('muestra error cuando la consulta falla', async () => {
  api.mockResolvedValueOnce([]);
  render(React.createElement(PanelQuorumPage, { talleres: [taller] }));
  await waitFor(() => expect(api).toHaveBeenCalledTimes(1));
  api.mockImplementationOnce(() => Promise.reject(new Error('No se pudo completar la operación (500).')));
  fireEvent.click(screen.getByRole('button', { name: 'Actualizar' }));
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'No se pudo completar la operación (500).');
});
