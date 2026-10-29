import { afterEach, describe, expect, it, vi } from 'vitest';
import { request, apiGetEventos, apiSyncBatch } from './api';

afterEach(() => vi.unstubAllGlobals());
describe('cliente HTTP', () => {
  it('envía token y lote en JSON al Gateway', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ acceptedIds: ['uno'] }) });
    vi.stubGlobal('fetch', fetchMock);
    const result = await apiSyncBatch([{ id: 'uno' }], 'token-prueba');
    expect(result).toEqual({ acceptedIds: ['uno'] });
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/api/sync/batch'),
      expect.objectContaining({ method: 'POST', body: JSON.stringify({ registros: [{ id: 'uno' }] }),
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer token-prueba' } }));
  });
  it('consulta el catálogo sin token', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => [] });
    vi.stubGlobal('fetch', fetchMock);
    expect(await apiGetEventos()).toEqual([]);
    expect(fetchMock.mock.calls[0][1].headers).toEqual({});
  });
  it('acepta una respuesta 204 sin cuerpo', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 204 }));
    expect(await request('/api/auth/confirm', { method: 'POST' })).toBeNull();
  });
  it('muestra el error informado por la API', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 403, json: async () => ({ message: 'Permiso denegado' }) }));
    await expect(request('/api/proyectos')).rejects.toThrow('Permiso denegado');
  });
  it.each([401, 403, 409, 500])('muestra un error útil para HTTP %i sin JSON', async status => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status, json: async () => { throw Error('sin cuerpo'); } }));
    await expect(request('/api/proyectos')).rejects.toThrow();
  });
});
