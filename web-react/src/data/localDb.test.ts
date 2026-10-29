import 'fake-indexeddb/auto';
import { describe, expect, it } from 'vitest';
import { listLocal, saveLocal, removeLocal, cacheCatalog, cachedCatalog } from './localDb';

describe('persistencia local por usuario', () => {
  it('crea, consulta y elimina un borrador sin tocar el de otra cuenta', async () => {
    await saveLocal('drafts', { id: 'uno', owner: 'ethan', text: 'primero' });
    await saveLocal('drafts', { id: 'dos', owner: 'jorge', text: 'segundo' });
    expect(await listLocal('drafts', 'ethan')).toEqual([{ id: 'uno', owner: 'ethan', text: 'primero' }]);
    await removeLocal('drafts', 'dos', 'ethan');
    expect(await listLocal('drafts', 'jorge')).toHaveLength(1);
    await removeLocal('drafts', 'uno', 'ethan');
    expect(await listLocal('drafts', 'ethan')).toEqual([]);
  });
  it('actualiza un registro local y conserva el catálogo', async () => {
    await saveLocal('queue', { id: 'a', owner: 'ethan', tipo: 'asistencia' });
    await saveLocal('queue', { id: 'a', owner: 'ethan', tipo: 'encuesta' });
    expect((await listLocal('queue', 'ethan'))[0].tipo).toBe('encuesta');
    await cacheCatalog([{ id: 7, title: 'Taller' }]);
    expect(await cachedCatalog()).toEqual([{ id: 7, title: 'Taller' }]);
  });
});
