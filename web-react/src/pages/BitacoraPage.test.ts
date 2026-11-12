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
const originalCreateObjectURL = URL.createObjectURL;
const originalRevokeObjectURL = URL.revokeObjectURL;
beforeEach(() => api.mockReset());
afterEach(() => { cleanup(); vi.unstubAllGlobals(); URL.createObjectURL = originalCreateObjectURL; URL.revokeObjectURL = originalRevokeObjectURL; });

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

it('exige consentimiento antes de activar el micrófono', async () => {
  api.mockResolvedValue({ contenido: 'Notas.', resumen: 'Notas.', fragmentos: ['Notas.'] });
  render(React.createElement(BitacoraPage, { tallerId: '42' }));
  fireEvent.click(screen.getByRole('button', { name: 'Grabar audio local' }));
  expect(await screen.findByText('Confirma el consentimiento de las personas antes de grabar.')).toBeTruthy();
});

it('permite descargar una grabación local sin enviarla a la API', async () => {
  api.mockResolvedValue({ contenido: 'Notas.', resumen: 'Notas.', fragmentos: ['Notas.'] });
  const stopTrack = vi.fn();
  Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: { getUserMedia: vi.fn(async () => ({ getTracks: () => [{ stop: stopTrack }] })) } });
  class Recorder {
    static isTypeSupported() { return true; }
    state = 'inactive';
    mimeType = 'audio/webm';
    stream;
    ondataavailable = (_event: { data: Blob }) => {};
    onstop = () => {};
    constructor(stream: unknown) { this.stream = stream; }
    start() { this.state = 'recording'; }
    stop() { this.state = 'inactive'; this.ondataavailable({ data: new Blob(['demo'], { type: 'audio/webm' }) }); this.onstop(); }
  }
  vi.stubGlobal('MediaRecorder', Recorder);
  const create = vi.fn(() => 'blob:demo');
  const revoke = vi.fn();
  URL.createObjectURL = create;
  URL.revokeObjectURL = revoke;
  render(React.createElement(BitacoraPage, { tallerId: '42' }));
  fireEvent.click(screen.getByLabelText(/Confirmo que las personas presentes/));
  fireEvent.click(screen.getByRole('button', { name: 'Grabar audio local' }));
  fireEvent.click(await screen.findByRole('button', { name: 'Detener grabación' }));
  expect(await screen.findByRole('link', { name: 'Descargar grabación' })).toHaveProperty('href', 'blob:demo');
  expect(stopTrack).toHaveBeenCalled();
  expect(api.mock.calls.some(([, options]) => options?.method === 'POST' || options?.method === 'PUT')).toBe(false);
});
