import React from 'react';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import HomePage from './HomePage';
import type { Taller } from '../types';

vi.mock('wouter', () => ({
  Link: ({ children, href, className }: { children: React.ReactNode; href: string; className?: string }) =>
    React.createElement('a', { href, className }, children),
}));

afterEach(() => cleanup());

const taller: Taller = {
  id: 1,
  title: 'Reciclaje',
  descripcion: 'Taller de reciclaje comunitario',
  area: 'Medio ambiente',
  comuna: 'La Florida',
  unidadVecinal: 'UV 12',
  direccion: 'Calle Falsa 123',
  fechaHora: '2026-12-01T10:00:00',
  docenteLider: 'Ana',
  cuposMaximos: 20,
  inscritos: 10,
  quorumPct: 50,
  image: '/img/taller.jpg',
  asistentes: [],
};

it('lista los talleres recibidos y permite seleccionar uno', () => {
  const onSelectTaller = vi.fn();
  render(React.createElement(HomePage, { talleres: [taller], onSelectTaller }));
  expect(screen.getByText('Reciclaje')).toBeTruthy();
  expect(screen.getByText('10 / 20')).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: 'Ver taller' }));
  expect(onSelectTaller).toHaveBeenCalledWith(taller);
});

it('no muestra tarjetas cuando no hay talleres', () => {
  render(React.createElement(HomePage, { talleres: [], onSelectTaller: vi.fn() }));
  expect(screen.queryByText('Reciclaje')).toBeNull();
  expect(screen.getByText('¿Por qué SyncPass?')).toBeTruthy();
});
