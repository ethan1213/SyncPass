import {openDB} from 'idb';
const db=openDB('syncpass',1,{upgrade(db){db.createObjectStore('drafts',{keyPath:'id'});db.createObjectStore('queue',{keyPath:'id'});db.createObjectStore('cache');}});
export const listLocal=async(store,owner)=>(await (await db).getAll(store)).filter(row=>row.owner===owner);
export const saveLocal=async(store,row)=>{await (await db).put(store,row);return row;};
export const removeLocal=async(store,id,owner)=>{const database=await db;const row=await database.get(store,id);if(row?.owner===owner)await database.delete(store,id);};
export const cacheCatalog=async(rows)=>(await db).put('cache',rows,'talleres');
export const cachedCatalog=async()=>(await db).get('cache','talleres');
