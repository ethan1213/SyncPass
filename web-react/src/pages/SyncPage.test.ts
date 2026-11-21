import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import SyncPage from './SyncPage';
import type { QueueRecord } from '../types';

afterEach(() => cleanup());

const record: QueueRecord = { id: 'q1', tipo: 'asistencia', tallerId: 1, timestamp: new Date().toISOString() };

it('sincroniza los pendientes y muestra el resultado', async () => {
  const onPerformSync = vi.fn().mockResolvedValue(2);
  render(React.createElement(SyncPage, { unsyncedItems: [record], onPerformSync, isOfflineMode: false }));
  expect(screen.getByText('1 registros pendientes')).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: 'Sincronizar pendientes' }));
  await waitFor(() => expect(screen.getByText('2 registros confirmados por el servidor.')).toBeTruthy());
  expect(onPerformSync).toHaveBeenCalled();
});

it('deshabilita el botón sin conexión o sin pendientes', () => {
  render(React.createElement(SyncPage, { unsyncedItems: [], onPerformSync: vi.fn(), isOfflineMode: true }));
  expect(screen.getByRole('button', { name: 'Sincronizar pendientes' })).toHaveProperty('disabled', true);
  expect(screen.getByText('Sin conexión. Tus registros permanecen guardados.')).toBeTruthy();
});

it('muestra error cuando la sincronización falla', async () => {
  const onPerformSync = vi.fn().mockRejectedValue(new Error('Sin respuesta del servidor.'));
  render(React.createElement(SyncPage, { unsyncedItems: [record], onPerformSync, isOfflineMode: false }));
  fireEvent.click(screen.getByRole('button', { name: 'Sincronizar pendientes' }));
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'Sin respuesta del servidor.');
});
