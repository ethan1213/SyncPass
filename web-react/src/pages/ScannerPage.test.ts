import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import ScannerPage from './ScannerPage';
import type { Taller } from '../types';

vi.mock('@zxing/browser', () => ({
  BrowserQRCodeReader: class {
    async decodeFromVideoDevice() {
      return { stop: vi.fn() };
    }
  },
}));

afterEach(() => cleanup());

const tallerSinGeo: Taller = {
  id: 1, title: 'Reciclaje', descripcion: '', area: '', comuna: '', unidadVecinal: '', direccion: '',
  fechaHora: '', docenteLider: '', cuposMaximos: 20, inscritos: 0, quorumPct: 0, image: '', asistentes: [],
};

it('guarda una marcación válida sin geocerca', async () => {
  const onAddAttendance = vi.fn().mockResolvedValue(undefined);
  render(React.createElement(ScannerPage, { talleres: [tallerSinGeo], onAddAttendance, isOfflineMode: false }));
  fireEvent.change(screen.getByLabelText('Taller'), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText('Código del participante'), { target: { value: 'SP1:11111111-1111-1111-1111-111111111111' } });
  fireEvent.click(screen.getByRole('button', { name: 'Guardar marcación' }));
  await waitFor(() => expect(onAddAttendance).toHaveBeenCalledWith(1, { participante: 'SP1:11111111-1111-1111-1111-111111111111' }));
  expect(await screen.findByRole('status')).toHaveProperty('textContent', expect.stringContaining('Marcación guardada'));
});

it('muestra error cuando el código del participante es inválido', async () => {
  const onAddAttendance = vi.fn();
  render(React.createElement(ScannerPage, { talleres: [tallerSinGeo], onAddAttendance, isOfflineMode: false }));
  fireEvent.change(screen.getByLabelText('Taller'), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText('Código del participante'), { target: { value: 'codigo-malo' } });
  fireEvent.click(screen.getByRole('button', { name: 'Guardar marcación' }));
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', expect.stringContaining('QR de participante inválido'));
  expect(onAddAttendance).not.toHaveBeenCalled();
});

it('muestra el estado sin conexión', () => {
  render(React.createElement(ScannerPage, { talleres: [tallerSinGeo], onAddAttendance: vi.fn(), isOfflineMode: true }));
  expect(screen.getByText('Sin conexión')).toBeTruthy();
});
