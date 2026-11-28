import { createElement } from 'react';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import { request } from '../clients/api';
import type { Taller } from '../types';
import GestionTallerPage from './GestionTallerPage';

vi.mock('../clients/api', () => ({ request: vi.fn() }));
vi.mock('../auth/SessionProvider', () => {
  const getAccessTokenSilently = vi.fn().mockResolvedValue('sesion');
  return { useSession: () => ({ getAccessTokenSilently }) };
});

afterEach(cleanup);

it('actualiza la asistencia guardada y muestra las encuestas aparte', async () => {
  const api = vi.mocked(request);
  const taller = { id: 3, title: 'Bienestar digital' } as Taller;
  const resultados = {
    total: 1, calificaciones: [{ valor: '5', total: 1 }], utilidad: [], comentarios: ['Práctico'],
  };
  api.mockResolvedValueOnce([]).mockResolvedValueOnce(resultados)
    .mockResolvedValueOnce([{ id: 'registro-1', fecha: '2026-11-27T19:00:00Z', invitado: null }])
    .mockResolvedValueOnce(resultados);

  render(createElement(GestionTallerPage, { taller }));
  expect(await screen.findByText('0 asistencias confirmadas en la nube')).toBeTruthy();
  expect(await screen.findByRole('img', { name: 'QR de acceso al taller 3' })).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: 'Actualizar marcaciones y encuestas' }));
  expect(await screen.findByText('1 asistencia confirmada en la nube')).toBeTruthy();
  expect(screen.getByText('Práctico')).toBeTruthy();
  expect(screen.getByRole('link', { name: 'Gestionar grabación, transcripción y resumen' })
    .getAttribute('href')).toBe('/bitacora/3');
});
