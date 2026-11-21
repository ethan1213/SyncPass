import { useState } from 'react';
import type { QueueRecord } from '../types';

export interface SyncPageProps {
  unsyncedItems: QueueRecord[];
  onPerformSync: () => Promise<number>;
  isOfflineMode: boolean;
}

export default function SyncPage({ unsyncedItems, onPerformSync, isOfflineMode }: SyncPageProps) {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  async function sync() {
    setBusy(true);
    setError('');
    setMessage('');
    try {
      const n = await onPerformSync();
      setMessage(n + ' registros confirmados por el servidor.');
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return <div className="contenedor" style={{ paddingBlock: 30 }}><h1>Sincronización</h1><p>{isOfflineMode ? 'Sin conexión. Tus registros permanecen guardados.' : 'Conexión disponible.'}</p>
    {error && <p role="alert" className="alert alert-warning">{error}</p>}{message && <p role="status">{message}</p>}
    <div className="panel"><h2>{unsyncedItems.length} registros pendientes</h2>{unsyncedItems.map((r) => <p key={r.id}>{r.tipo === 'encuesta' ? 'Encuesta' : 'Asistencia'} · Taller {r.tallerId} · {new Date(r.timestamp).toLocaleString()}</p>)}
      <button className="btn btn-primary" disabled={busy || isOfflineMode || !unsyncedItems.length} onClick={sync}>{busy ? 'Enviando…' : 'Sincronizar pendientes'}</button></div></div>;
}
