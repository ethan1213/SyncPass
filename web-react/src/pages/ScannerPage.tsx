import { useState, useRef, useEffect, type FormEvent } from 'react';
import { BrowserQRCodeReader, type IScannerControls } from '@zxing/browser';
import { parseParticipantCode } from '../data/participantQr';
import GeofenceNote from '../components/GeofenceNote';
import type { Taller } from '../types';

export interface AttendancePayload {
  participante: string;
  latitud?: number;
  longitud?: number;
  precisionMetros?: number;
}

export interface ScannerPageProps {
  talleres: Taller[];
  onAddAttendance: (tallerId: number, attendance: AttendancePayload) => Promise<void>;
  isOfflineMode: boolean;
}

interface GeoError {
  code: number;
  message: string;
}

export default function ScannerPage({ talleres, onAddAttendance, isOfflineMode }: ScannerPageProps) {
  const [tallerId, setTallerId] = useState('');
  const [code, setCode] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const video = useRef<HTMLVideoElement>(null);
  const controls = useRef<IScannerControls | null>(null);
  const cancelled = useRef(false);
  const [camera, setCamera] = useState(false);
  const stop = () => { cancelled.current = true; controls.current?.stop(); controls.current = null; setCamera(false); };
  useEffect(() => () => { cancelled.current = true; controls.current?.stop(); }, []);
  async function scan() {
    setError('');
    cancelled.current = false;
    setCamera(true);
    try {
      const reader = new BrowserQRCodeReader();
      const active = await reader.decodeFromVideoDevice(undefined, video.current!, (result, err, ctl) => {
        if (result) {
          try {
            setCode(parseParticipantCode(result.getText()));
            setMessage('Código leído. Revisa el taller y guarda la marcación.');
            ctl.stop();
            stop();
          } catch (e) {
            setError((e as Error).message);
          }
        }
      });
      if (cancelled.current) active.stop();
      else controls.current = active;
    } catch {
      setError('No se pudo abrir la cámara. Revisa el permiso o ingresa el código manualmente.');
      setCamera(false);
    }
  }
  async function record(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const taller = talleres.find((t) => t.id === Number(tallerId));
      let location: Partial<AttendancePayload> = {};
      if (taller?.latitud != null) {
        if (!navigator.geolocation) throw new Error('Este dispositivo no ofrece ubicación GPS.');
        const position = await new Promise<GeolocationPosition>((resolve, reject) =>
          navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }));
        location = { latitud: position.coords.latitude, longitud: position.coords.longitude, precisionMetros: position.coords.accuracy };
      }
      await onAddAttendance(Number(tallerId), { participante: parseParticipantCode(code), ...location });
      setMessage('Marcación guardada en el dispositivo. Pendiente de sincronización.');
      setCode('');
    } catch (e) {
      const err = e as Error & Partial<GeoError>;
      setError(err.code === 1 ? 'Permite el acceso a la ubicación para acreditar en este taller.' : err.message);
    } finally {
      setBusy(false);
    }
  }
  return <div className="contenedor" style={{ paddingBlock: 30 }}><h1>Acreditación</h1><p className="text-sm">Lee el QR o ingresa el identificador ficticio del participante.</p>
    <video ref={video} muted playsInline style={{ width: '100%', maxWidth: 420, display: camera ? 'block' : 'none' }} />
    <button type="button" className="btn btn-secondary" onClick={camera ? stop : scan}>{camera ? 'Detener cámara' : 'Leer código QR con cámara'}</button>
    <p>{isOfflineMode ? 'Sin conexión' : 'Con conexión'}</p><GeofenceNote required={talleres.find((t) => t.id === Number(tallerId))?.latitud != null} />{error && <p role="alert">{error}</p>}{message && <p role="status">{message}</p>}
    <form className="form-panel" onSubmit={record}><div className="form-field"><label htmlFor="scan-event">Taller</label><select id="scan-event" className="form-input" required value={tallerId} onChange={(e) => setTallerId(e.target.value)}><option value="">Selecciona un taller</option>{talleres.map((t) => <option key={t.id} value={t.id}>{t.title}</option>)}</select></div>
      <div className="form-field"><label htmlFor="scan-code">Código del participante</label><input id="scan-code" className="form-input" maxLength={100} required value={code} onChange={(e) => setCode(e.target.value)} placeholder="SP1:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" /></div>
      <button className="btn btn-primary" disabled={busy}>Guardar marcación</button></form></div>;
}
