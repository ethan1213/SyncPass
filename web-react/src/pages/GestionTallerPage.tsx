import { useEffect, useState } from 'react';
import { Link } from 'wouter';
import { useSession } from '../auth/SessionProvider';
import { request } from '../clients/api';
import type { Taller } from '../types';
import { TallerJoinQr } from './TallerDetailPage';

type Marcacion = { id: string; fecha: string; invitado: string | null };
type Frecuencia = { valor: string; total: number };
type Resultados = { total: number; calificaciones: Frecuencia[]; utilidad: Frecuencia[]; comentarios: string[] };

export default function GestionTallerPage({ taller }: { taller: Taller }) {
  const { getAccessTokenSilently } = useSession();
  const [datos, setDatos] = useState<{ marcaciones: Marcacion[]; encuestas: Resultados }>();
  const [error, setError] = useState('');
  const [actualizacion, setActualizacion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    const endpoint = `/api/eventos/${taller.id}`;
    setError('');
    getAccessTokenSilently().then((token) => Promise.all([
      request<Marcacion[]>(`${endpoint}/marcaciones`, { token, signal: controller.signal }),
      request<Resultados>(`${endpoint}/encuestas/resultados`, { token, signal: controller.signal }),
    ])).then(([marcaciones, encuestas]) => {
      if (!controller.signal.aborted && marcaciones && encuestas) setDatos({ marcaciones, encuestas });
    }).catch((err: Error) => {
      if (!controller.signal.aborted) setError(err.message);
    });
    return () => controller.abort();
  }, [taller.id, actualizacion, getAccessTokenSilently]);

  return <main className="contenedor" style={{ paddingBlock: 30 }}>
    <Link href="/talleres" className="enlace-volver">Volver a gestionar talleres</Link>
    <h1>{taller.title}</h1>
    <TallerJoinQr id={taller.id} />
    <section className="seccion-bloque">
      <h2>Marcaciones</h2>
      <button className="btn btn-secondary" onClick={() => setActualizacion((valor) => valor + 1)}>
        Actualizar marcaciones y encuestas
      </button>
      {error && <p role="alert">{error}</p>}
      {!datos && !error && <p role="status">Consultando los registros guardados…</p>}
      {datos && <>
        <p role="status">{datos.marcaciones.length} {datos.marcaciones.length === 1
          ? 'asistencia confirmada en la nube' : 'asistencias confirmadas en la nube'}</p>
        {datos.marcaciones.length > 0 && <ul>{datos.marcaciones.map((fila) => <li key={fila.id}>
          {fila.invitado ? `Invitado · ${fila.invitado}` : 'Cuenta SyncPass'} · {new Date(fila.fecha).toLocaleString('es-CL')}
        </li>)}</ul>}
      </>}
    </section>
    <section className="seccion-bloque">
      <h2>Encuestas anónimas</h2>
      {datos && <>
        <p>{datos.encuestas.total} {datos.encuestas.total === 1 ? 'respuesta' : 'respuestas'}</p>
        {datos.encuestas.total > 0 && <>
          <h3>Calificación</h3>
          <ul>{datos.encuestas.calificaciones.map(({ valor, total }) => <li key={valor}>{valor}/5 · {total}</li>)}</ul>
          {datos.encuestas.utilidad.length > 0 && <>
            <h3>Utilidad</h3>
            <ul>{datos.encuestas.utilidad.map(({ valor, total }) => <li key={valor}>{valor} · {total}</li>)}</ul>
          </>}
          {datos.encuestas.comentarios.length > 0 && <>
            <h3>Comentarios</h3>
            <ul>{datos.encuestas.comentarios.map((comentario, index) => <li key={index}>{comentario}</li>)}</ul>
          </>}
        </>}
      </>}
    </section>
    <section className="seccion-bloque">
      <h2>Material y resumen</h2>
      <Link className="btn btn-primary" href={`/bitacora/${taller.id}`}>Gestionar grabación, transcripción y resumen</Link>
    </section>
  </main>;
}
