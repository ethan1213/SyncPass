import { openDB, type IDBPDatabase } from 'idb';

export type StoreName = 'drafts' | 'queue' | 'cache';

interface OwnedRecord {
  id?: string;
  owner?: string;
}

const db: Promise<IDBPDatabase> = openDB('syncpass', 1, {
  upgrade(database) {
    database.createObjectStore('drafts', { keyPath: 'id' });
    database.createObjectStore('queue', { keyPath: 'id' });
    database.createObjectStore('cache');
  },
});

export const listLocal = async <T extends OwnedRecord = OwnedRecord>(store: StoreName, owner?: string): Promise<T[]> =>
  ((await (await db).getAll(store)) as T[]).filter((row) => row.owner === owner);

export const saveLocal = async <T extends OwnedRecord = OwnedRecord>(store: StoreName, row: T): Promise<T> => {
  await (await db).put(store, row);
  return row;
};

export const removeLocal = async (store: StoreName, id: string, owner?: string): Promise<void> => {
  const database = await db;
  const row = await database.get(store, id);
  if (row?.owner === owner) await database.delete(store, id);
};

export const cacheCatalog = async <T>(rows: T[]): Promise<IDBValidKey> => (await db).put('cache', rows, 'talleres');
export const cachedCatalog = async <T>(): Promise<T | undefined> => (await db).get('cache', 'talleres');
