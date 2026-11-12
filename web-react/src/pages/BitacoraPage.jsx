import React, { useEffect, useRef, useState } from 'react';
import { Link } from 'wouter';
import { request } from '../clients/api';
import { useSession } from '../auth/SessionProvider';
import { useUserRoles } from '../hooks/useRoles';

export default function BitacoraPage({ tallerId }) {
  const { getAccessTokenSilently } = useSession();
  const roles = useUserRoles();
  const canEdit = roles.some((role) => ['admin', 'coordinador'].includes(role));
  const [contenido, setContenido] = useState('');
  const [resumen, setResumen] = useState('');
  const [fragmentos, setFragmentos] = useState([]);
  const [pregunta, setPregunta] = useState('');
  const [respuesta, setRespuesta] = useState(null);
  const [mensaje, setMensaje] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const [dictando, setDictando] = useState(false);
  const [consentimiento, setConsentimiento] = useState(false);
  const recognition = useRef(null);

  useEffect(() => {
    let active = true;
    getAccessTokenSilently().then((token) => request(`/api/eventos/${tallerId}/bitacora`, { token }))
      .then((data) => { if (active) { setContenido(data.contenido); setResumen(data.resumen); setFragmentos(data.fragmentos); } })
      .catch((error) => { if (active) setMensaje(error.message.includes('404') ? 'Este taller aún no tiene bitácora.' : error.message); });
    return () => { active = false; recognition.current?.stop(); };
  }, [tallerId, getAccessTokenSilently]);

  async function guardar() {
    if (!contenido.trim()) return setMensaje('Escribe el contenido antes de guardar.');
    setOcupado(true); setMensaje('');
    try {
      const token = await getAccessTokenSilently();
      const data = await request(`/api/eventos/${tallerId}/bitacora`, { method: 'PUT', body: { contenido }, token });
      setResumen(data.resumen); setFragmentos(data.fragmentos); setRespuesta(null);
      setMensaje('Bitácora guardada en el servidor. Ya se puede consultar desde Android.');
    } catch (error) { setMensaje(error.message); }
    finally { setOcupado(false); }
  }

  async function consultar(event) {
    event.preventDefault();
    if (!pregunta.trim()) return;
    setOcupado(true); setMensaje(''); setRespuesta(null);
    try {
      const token = await getAccessTokenSilently();
      setRespuesta(await request(`/api/eventos/${tallerId}/bitacora/preguntas`, { method: 'POST', body: { pregunta }, token }));
    } catch (error) { setMensaje(error.message); }
    finally { setOcupado(false); }
  }

  function dictar() {
    if (!consentimiento) return setMensaje('Confirma el consentimiento de las personas antes de activar el micrófono.');
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) return setMensaje('Este navegador no admite dictado. Puedes escribir o pegar la transcripción.');
    if (dictando) { recognition.current?.stop(); return; }
    const instance = new SpeechRecognition();
    instance.lang = 'es-CL'; instance.continuous = true; instance.interimResults = false;
    instance.onresult = (event) => {
      const text = Array.from(event.results).slice(event.resultIndex).map((result) => result[0].transcript).join(' ');
      setContenido((current) => `${current}${current ? ' ' : ''}${text}`);
    };
    instance.onerror = () => setMensaje('El dictado se interrumpió. Conservamos el texto recibido.');
    instance.onend = () => setDictando(false);
    recognition.current = instance; instance.start(); setDictando(true);
  }

  return <main className="contenedor" style={{ padding: '28px 0 64px', maxWidth: 900 }}>
    <Link href={`/taller/${tallerId}`} className="enlace-volver">← Volver al taller</Link>
    <p className="eyebrow">Bitácora del taller</p>
    <h1>Lo aprendido, en un solo lugar</h1>
    <p className="texto-apoyo">El resumen y las respuestas se basan exclusivamente en el texto guardado para este taller. La herramienta muestra el fragmento de origen y avisa cuando no encuentra una respuesta.</p>
    {mensaje && <p className="alert alert-info" role="status">{mensaje}</p>}
    {canEdit && <section className="seccion-bloque">
      <h2>Contenido de la sesión</h2>
      <p className="texto-apoyo">Pide consentimiento antes de dictar. El navegador transcribe la voz; SyncPass guarda solo el texto al pulsar Guardar.</p>
      <label style={{ display: 'block', marginBottom: 12 }}><input type="checkbox" checked={consentimiento} onChange={(event) => setConsentimiento(event.target.checked)} /> Confirmo que las personas presentes autorizaron el uso del micrófono para esta bitácora.</label>
      <label htmlFor="contenido-bitacora">Transcripción o apuntes</label>
      <textarea id="contenido-bitacora" rows="10" maxLength={20000} value={contenido} onChange={(e) => setContenido(e.target.value)} style={{ width: '100%', padding: 12 }} />
      <div className="acciones-fila">
        <button type="button" className="btn btn-secondary" onClick={dictar}>{dictando ? 'Detener dictado' : 'Dictar en Chrome'}</button>
        <button type="button" className="btn btn-primary" disabled={ocupado} onClick={guardar}>Guardar y resumir</button>
      </div>
    </section>}
    <section className="seccion-bloque"><h2>Resumen de la sesión</h2>
      <p>{resumen || 'Cuando el equipo publique la bitácora, el resumen aparecerá aquí.'}</p>
    </section>
    <section className="seccion-bloque"><h2>Preguntar sobre el taller</h2>
      <form onSubmit={consultar}><label htmlFor="pregunta-bitacora">Tu pregunta</label>
        <input id="pregunta-bitacora" value={pregunta} onChange={(e) => setPregunta(e.target.value)} maxLength={300} style={{ width: '100%', padding: 12 }} placeholder="¿Qué se explicó sobre…?" />
        <button className="btn btn-primary" disabled={ocupado || !fragmentos.length} type="submit">Buscar respuesta</button></form>
      {respuesta && <div className="alert alert-info" role="status"><strong>Respuesta basada en la bitácora:</strong> {respuesta.respuesta}
        {respuesta.fragmento > 0 && <p>Fuente: fragmento {respuesta.fragmento} del contenido guardado.</p>}</div>}
    </section>
  </main>;
}
