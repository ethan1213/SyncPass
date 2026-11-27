import React from 'react';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import TallerDetailPage from './TallerDetailPage';
import { request } from '../clients/api';
import type { Taller } from '../types';

vi.mock('../clients/api', () => ({ request: vi.fn() }));
vi.mock('wouter', () => ({
  Link: ({ children, href, className }: { children: React.ReactNode; href: string; className?: string }) =>
    React.createElement('a', { href, className }, children),
}));
const api = vi.mocked(request);

beforeEach(() => api.mockReset());
afterEach(() => cleanup());

const taller: Taller = {
  id: 7,
  title: 'Huerto comunitario',
  descripcion: 'Aprendemos a cultivar',
  area: 'Medio ambiente',
  comuna: 'La Florida',
  unidadVecinal: 'UV 3',
  direccion: 'Av. Siempre Viva 742',
  fechaHora: '2026-12-10T15:00:00',
  docenteLider: 'Marta',
  cuposMaximos: 30,
  inscritos: 12,
  quorumPct: 40,
  image: '/img/huerto.jpg',
  asistentes: [
    { id: 1, nombre: 'Juan', hora: '10:00', estado: 'Presente', synced: true },
    { id: 2, nombre: 'Pedro', hora: '10:05', estado: 'Presente', synced: false },
  ],
};

it('muestra un mensaje cuando no se encuentra el taller', () => {
  render(React.createElement(TallerDetailPage, { taller: undefined }));
  expect(screen.getByText('No encontramos ese taller')).toBeTruthy();
});

it('muestra la ficha completa del taller y el resumen de asistencia', async () => {
  api.mockResolvedValue({ grabado: true, resumenDisponible: true, mensaje: 'Grabación disponible.' });
  render(React.createElement(TallerDetailPage, { taller }));
  expect(screen.getByText('Huerto comunitario')).toBeTruthy();
  expect(screen.getByText('12 / 30')).toBeTruthy();
  expect(screen.getByText('Juan')).toBeTruthy();
  expect(screen.getByText('Sincronizado')).toBeTruthy();
  expect(screen.getByText('Pendiente')).toBeTruthy();
  expect(screen.getByText(/1 sincronizado\(s\)/)).toBeTruthy();
  await waitFor(() => expect(screen.getByText('Grabación disponible.')).toBeTruthy());
  expect(screen.getByRole('link', { name: 'Resumen y preguntas del taller' })).toBeTruthy();
});

it('deja disponible la encuesta y reserva el acceso de edición cuando solo existe la grabación', async () => {
  api.mockResolvedValue({ grabado: true, resumenDisponible: false, mensaje: 'Transcripción pendiente.' });
  const view = render(React.createElement(TallerDetailPage, { taller }));
  expect(await screen.findByText('Transcripción pendiente.')).toBeTruthy();
  expect(screen.getByRole('link', { name: 'Aplicar encuesta en terreno' })).toBeTruthy();
  expect(screen.queryByRole('link', { name: 'Resumen y preguntas del taller' })).toBeNull();
  expect(screen.queryByRole('link', { name: 'Gestionar bitácora' })).toBeNull();
  view.rerender(React.createElement(TallerDetailPage, { taller, puedeGestionarBitacora: true }));
  expect(screen.getByRole('link', { name: 'Gestionar bitácora' })).toBeTruthy();
});

it('muestra aviso cuando no hay marcaciones registradas', () => {
  api.mockResolvedValue({ grabado: false, mensaje: '' });
  render(React.createElement(TallerDetailPage, { taller: { ...taller, asistentes: [] } }));
  expect(screen.getByText('Todavía no hay marcaciones registradas para este taller.')).toBeTruthy();
});
