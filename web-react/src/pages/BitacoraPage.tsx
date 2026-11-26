import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from 'wouter';
import { request } from '../clients/api';
import { useSession } from '../auth/SessionProvider';
import { useUserRoles } from '../hooks/useRoles';

interface Respuesta {
  respuesta: string;
  fragmento: number;
  cita?: string;
}

/** La Web Speech API no forma parte del DOM estándar de TypeScript; se tipa mínimamente aquí. */
interface SpeechRecognitionResultLike {
  0: { transcript: string };
}
interface SpeechRecognitionEventLike {
  resultIndex: number;
  results: ArrayLike<SpeechRecognitionResultLike>;
}
interface SpeechRecognitionLike {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onerror: (() => void) | null;
  onend: (() => void) | null;
  start(): void;
  stop(): void;
}
type SpeechRecognitionCtor = new () => SpeechRecognitionLike;

declare global {
  interface Window {
    SpeechRecognition?: SpeechRecognitionCtor;
    webkitSpeechRecognition?: SpeechRecognitionCtor;
  }
}

export interface BitacoraPageProps {
  tallerId: string;
}

export default function BitacoraPage({ tallerId }: BitacoraPageProps) {
  const { getAccessTokenSilently } = useSession();
  const roles = useUserRoles();
  const canEdit = roles.some((role) => ['admin', 'coordinador'].includes(role));
  const [contenido, setContenido] = useState('');
  const [resumen, setResumen] = useState('');
  const [fragmentos, setFragmentos] = useState<string[]>([]);
  const [pregunta, setPregunta] = useState('');
  const [respuesta, setRespuesta] = useState<Respuesta | null>(null);
  const [mensaje, setMensaje] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const [dictando, setDictando] = useState(false);
  const [grabando, setGrabando] = useState(false);
  const [audioUrl, setAudioUrl] = useState('');
  const [audio, setAudio] = useState<Blob | null>(null);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [autorizaProveedor, setAutorizaProveedor] = useState(false);
  const [borrador, setBorrador] = useState('');
  const [consentimiento, setConsentimiento] = useState(false);
  const [grabado, setGrabado] = useState<boolean | null>(null);
  const recognition = useRef<SpeechRecognitionLike | null>(null);
  const recorder = useRef<MediaRecorder | null>(null);
  const audioUrlRef = useRef('');

  useEffect(() => {
    let active = true;
    getAccessTokenSilently().then((token) => request(`/api/eventos/${tallerId}/bitacora`, { token }))
      .then((data) => { if (active) { setContenido(data.contenido); setResumen(data.resumen); setFragmentos(data.fragmentos); } })
      .catch((error) => { if (active) setMensaje(error.message.includes('404') ? 'Este taller aún no tiene bitácora.' : error.message); });
    request(`/api/eventos/${tallerId}/grabacion`)
      .then((state) => { if (active) setGrabado(state.grabado); })
      .catch(() => {});
    return () => {
      active = false;
      recognition.current?.stop();
      if (recorder.current?.state === 'recording') recorder.current.stop();
      recorder.current?.stream?.getTracks().forEach((track) => track.stop());
      if (audioUrlRef.current) URL.revokeObjectURL(audioUrlRef.current);
    };
  }, [tallerId, getAccessTokenSilently]);

  async function guardar() {
    if (!contenido.trim()) return setMensaje('Escribe el contenido antes de guardar.');
    setOcupado(true);
    setMensaje('');
    try {
      const token = await getAccessTokenSilently();
      const data = await request(`/api/eventos/${tallerId}/bitacora`, {
        method: 'PUT', body: { contenido, resumenRevisado: borrador || undefined }, token,
      });
      setResumen(data.resumen);
      setFragmentos(data.fragmentos);
      setRespuesta(null);
      setMensaje('Bitácora guardada en el servidor. Ya se puede consultar desde Android.');
    } catch (error) {
      setMensaje((error as Error).message);
    } finally {
      setOcupado(false);
    }
  }

  async function prepararBorrador() {
    setOcupado(true);
    try {
      const token = await getAccessTokenSilently();
      const resultado = await request(`/api/eventos/${tallerId}/bitacora/borrador`, {
        method: 'POST', token, body: { contenido, autorizaProveedor },
      });
      setBorrador(resultado.resumen);
      setMensaje(resultado.origen === 'gemini'
        ? 'Gemini preparó el borrador. Revísalo antes de publicar.'
        : 'Se preparó un borrador extractivo con los apuntes. Revísalo antes de publicar.');
    } catch (error) {
      setMensaje((error as Error).message);
    } finally {
      setOcupado(false);
    }
  }

  async function transcribir(entrada: Blob) {
    if (entrada.size > 4 * 1024 * 1024) return setMensaje('Elige un audio de hasta 4 MB.');
    const mime = entrada.type.split(';')[0];
    const form = new FormData();
    form.set('audio', entrada.slice(0, entrada.size, mime), 'taller');
    form.set('autorizaProveedor', String(autorizaProveedor));
    setOcupado(true);
    try {
      const token = await getAccessTokenSilently();
      const resultado = await request(`/api/eventos/${tallerId}/bitacora/transcripcion`, {
        method: 'POST', token, body: form,
      });
      setContenido(resultado.contenido);
      setBorrador('');
      setMensaje('Transcripción lista para revisar. Prepara el resumen y publica cuando termines.');
    } catch (error) {
      const detalle = (error as Error).message;
      setMensaje(detalle.includes('503') ? 'Gemini no está disponible. Puedes pegar los apuntes o usar el dictado.' : detalle);
    } finally {
      setOcupado(false);
    }
  }

  async function definirGrabacion(value: boolean) {
    setOcupado(true);
    setMensaje('');
    try {
      const token = await getAccessTokenSilently();
      const state = await request(`/api/eventos/${tallerId}/grabacion`, {
        method: 'PUT', body: { grabado: value }, token,
      });
      setGrabado(state.grabado);
      setMensaje(state.mensaje);
    } catch (error) {
      setMensaje((error as Error).message);
    } finally {
      setOcupado(false);
    }
  }

  async function consultar(event: FormEvent) {
    event.preventDefault();
    if (!pregunta.trim()) return;
    setOcupado(true);
    setMensaje('');
    setRespuesta(null);
    try {
      const token = await getAccessTokenSilently();
      setRespuesta(await request(`/api/eventos/${tallerId}/bitacora/preguntas`, { method: 'POST', body: { pregunta }, token }));
    } catch (error) {
      setMensaje((error as Error).message);
    } finally {
      setOcupado(false);
    }
  }

  function dictar() {
    if (!consentimiento) return setMensaje('Confirma el consentimiento de las personas antes de activar el micrófono.');
    const SpeechRecognitionCtor = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognitionCtor) return setMensaje('Este navegador no admite dictado. Puedes escribir o pegar la transcripción.');
    if (dictando) { recognition.current?.stop(); return; }
    const instance = new SpeechRecognitionCtor();
    instance.lang = 'es-CL';
    instance.continuous = true;
    instance.interimResults = false;
    instance.onresult = (event) => {
      const text = Array.from(event.results).slice(event.resultIndex).map((result) => result[0].transcript).join(' ');
      setContenido((current) => `${current}${current ? ' ' : ''}${text}`);
      setBorrador('');
    };
    instance.onerror = () => setMensaje('El dictado se interrumpió. Conservamos el texto recibido.');
    instance.onend = () => setDictando(false);
    recognition.current = instance;
    instance.start();
    setDictando(true);
  }

  async function grabar() {
    if (grabando) { recorder.current?.stop(); setGrabando(false); return; }
    if (!consentimiento) return setMensaje('Confirma el consentimiento de las personas antes de grabar.');
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) return setMensaje('Este navegador no admite grabación de audio.');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mimeType = MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : '';
      const instance = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
      const chunks: Blob[] = [];
      instance.ondataavailable = (event) => { if (event.data.size) chunks.push(event.data); };
      instance.onstop = () => {
        stream.getTracks().forEach((track) => track.stop());
        if (audioUrlRef.current) URL.revokeObjectURL(audioUrlRef.current);
        const captura = new Blob(chunks, { type: instance.mimeType || 'audio/webm' });
        setAudio(captura);
        audioUrlRef.current = URL.createObjectURL(captura);
        setAudioUrl(audioUrlRef.current);
        setGrabando(false);
      };
      recorder.current = instance;
      setAudio(null);
      setAudioUrl('');
      setMensaje('Grabando solo en este navegador. Detén la grabación y descarga el archivo antes de salir.');
      instance.start();
      setGrabando(true);
    } catch {
      setMensaje('No se pudo iniciar el micrófono. Revisa el permiso del navegador.');
    }
  }

  return <main className="contenedor" style={{ padding: '28px 0 64px', maxWidth: 900 }}>
    <Link href={`/taller/${tallerId}`} className="enlace-volver">← Volver al taller</Link>
    <p className="eyebrow">Bitácora del taller</p>
    <h1>Lo aprendido, en un solo lugar</h1>
    <p className="texto-apoyo">El resumen y las respuestas se basan exclusivamente en el texto guardado para este taller. La herramienta muestra el fragmento de origen y avisa cuando no encuentra una respuesta.</p>
    {mensaje && <p className="alert alert-info" role="status">{mensaje}</p>}
    {canEdit && <section className="seccion-bloque">
      <h2>Contenido de la sesión</h2>
      <fieldset disabled={ocupado} style={{ marginBottom: 16 }}>
        <legend>¿Se grabó este taller?</legend>
        <label><input type="radio" name="grabado" checked={grabado === true} onChange={() => definirGrabacion(true)} /> Sí</label>{' '}
        <label><input type="radio" name="grabado" checked={grabado === false} onChange={() => definirGrabacion(false)} /> No</label>
      </fieldset>
      <p className="texto-apoyo">Puedes dictar, grabar o seleccionar un archivo. Revisa la transcripción y publica la bitácora para compartirla con Android.</p>
      <label style={{ display: 'block', marginBottom: 12 }}><input type="checkbox" checked={consentimiento} onChange={(event) => setConsentimiento(event.target.checked)} /> Confirmo que las personas presentes autorizaron el uso del micrófono para esta bitácora.</label>
      <label htmlFor="contenido-bitacora">Transcripción o apuntes</label>
      <textarea id="contenido-bitacora" rows={10} maxLength={20000} disabled={ocupado} value={contenido} onChange={(e) => { setContenido(e.target.value); setBorrador(''); }} style={{ width: '100%', padding: 12 }} />
      <div className="acciones-fila">
        <button type="button" className="btn btn-secondary" onClick={dictar} disabled={grabando}>{dictando ? 'Detener dictado' : 'Dictar en Chrome'}</button>
        <button type="button" className="btn btn-secondary" onClick={grabar} disabled={dictando}>{grabando ? 'Detener grabación' : 'Grabar audio local'}</button>
        <button type="button" className="btn btn-primary" disabled={ocupado || dictando || grabando} onClick={guardar}>{borrador.trim() ? 'Publicar bitácora' : 'Guardar y resumir'}</button>
      </div>
      {audioUrl && <p><a href={audioUrl} download={`syncpass-taller-${tallerId}.webm`} className="btn btn-secondary">Descargar grabación</a></p>}
      <fieldset disabled={ocupado || dictando || grabando}>
        <legend>Asistente de transcripción y resumen</legend>
        <label><input type="checkbox" checked={autorizaProveedor} onChange={(e) => setAutorizaProveedor(e.target.checked)} /> Autorizo enviar el audio o texto de este taller a Gemini.</label>
        <div className="acciones-fila">
          <label>Archivo de audio (hasta 4 MB) <input type="file" accept="audio/*" onChange={(e) => setArchivo(e.target.files?.[0] ?? null)} /></label>
          {archivo && <button type="button" className="btn btn-secondary" disabled={!autorizaProveedor} onClick={() => transcribir(archivo)}>Transcribir archivo</button>}
          {audio && <button type="button" className="btn btn-secondary" disabled={!autorizaProveedor} onClick={() => transcribir(audio)}>Transcribir grabación</button>}
          <button type="button" className="btn btn-secondary" disabled={!contenido.trim()} onClick={prepararBorrador}>Preparar borrador</button>
        </div>
        {borrador && <label>Resumen para revisar <textarea rows={5} maxLength={3000} value={borrador} onChange={(e) => setBorrador(e.target.value)} style={{ width: '100%', padding: 12 }} /></label>}
      </fieldset>
    </section>}
    <section className="seccion-bloque"><h2>Resumen de la sesión</h2>
      <p>{resumen || 'Cuando el equipo publique la bitácora, el resumen aparecerá aquí.'}</p>
    </section>
    <section className="seccion-bloque"><h2>Preguntar sobre el taller</h2>
      <form onSubmit={consultar}><label htmlFor="pregunta-bitacora">Tu pregunta</label>
        <input id="pregunta-bitacora" value={pregunta} onChange={(e) => setPregunta(e.target.value)} maxLength={300} style={{ width: '100%', padding: 12 }} placeholder="¿Qué se explicó sobre…?" />
        <button className="btn btn-primary" disabled={ocupado || !fragmentos.length} type="submit">Buscar respuesta</button></form>
      {respuesta && <div className="alert alert-info" role="status"><strong>Respuesta basada en la bitácora:</strong> {respuesta.respuesta}
        {respuesta.fragmento > 0 && <p>Fuente: fragmento {respuesta.fragmento} del contenido guardado.</p>}
        {respuesta.cita && <blockquote>{respuesta.cita}</blockquote>}</div>}
    </section>
  </main>;
}
