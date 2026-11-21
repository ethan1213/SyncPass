import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import DraftsPage from './DraftsPage';
import { listLocal, saveLocal, removeLocal } from '../data/localDb';
import type { Taller, DraftRecord } from '../types';

vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ user: { sub: 'owner-1' } }) }));
vi.mock('../data/localDb', () => ({ listLocal: vi.fn(), saveLocal: vi.fn(), removeLocal: vi.fn() }));
const list = vi.mocked(listLocal);
const save = vi.mocked(saveLocal);
const remove = vi.mocked(removeLocal);

const taller: Taller = {
  id: 1, title: 'Reciclaje', descripcion: '', area: '', comuna: '', unidadVecinal: '', direccion: '',
  fechaHora: '', docenteLider: '', cuposMaximos: 20, inscritos: 0, quorumPct: 0, image: '', asistentes: [],
};

beforeEach(() => {
  list.mockReset();
  save.mockReset();
  remove.mockReset();
  list.mockResolvedValue([]);
});
afterEach(() => cleanup());

it('muestra que no hay borradores guardados', async () => {
  render(React.createElement(DraftsPage, { talleres: [taller], onQueue: vi.fn() }));
  expect(await screen.findByText('No hay borradores guardados.')).toBeTruthy();
});

it('guarda un nuevo borrador', async () => {
  save.mockResolvedValue(undefined as unknown as DraftRecord);
  render(React.createElement(DraftsPage, { talleres: [taller], onQueue: vi.fn() }));
  await screen.findByText('No hay borradores guardados.');
  fireEvent.change(screen.getByLabelText('Taller'), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText('Comentario'), { target: { value: 'Muy buen taller' } });
  fireEvent.click(screen.getByRole('button', { name: 'Guardar borrador' }));
  await waitFor(() => expect(save).toHaveBeenCalled());
  expect(save.mock.calls[0][1]).toEqual(expect.objectContaining({ owner: 'owner-1', tallerId: 1, comentario: 'Muy buen taller' }));
});

it('envía un borrador existente a pendientes', async () => {
  const draft = { id: 'd1', tallerId: 1, rating: 4, utilidad: 'muy_util', comentario: 'Listo' };
  list.mockResolvedValue([draft]);
  remove.mockResolvedValue(undefined);
  const onQueue = vi.fn().mockResolvedValue(undefined);
  render(React.createElement(DraftsPage, { talleres: [taller], onQueue }));
  fireEvent.click(await screen.findByRole('button', { name: 'Enviar a pendientes' }));
  await waitFor(() => expect(onQueue).toHaveBeenCalledWith(expect.objectContaining({ id: 'd1', tipo: 'encuesta', tallerId: 1 })));
  expect(remove).toHaveBeenCalledWith('drafts', 'd1', 'owner-1');
});
