import { useEffect, useState } from 'react';
import { useSession } from '../auth/SessionProvider';
import { apiGetEventos, request } from '../clients/api';
import type { Inscripcion, TallerRaw } from '../types';

/**
 * RF12: talleres que el participante marcó "voy a asistir" (preinscripción en
 * el servidor, tabla `inscripcion`) y aún no acredita. La acreditación real
 * (QR o `/ingreso/cuenta`) es la que pasa el estado a "asistio" en el backend.
 */
export default function MisTalleresPendientesPage() {
  const { getAccessTokenSilently } = useSession();
  const [talleres, setTalleres] = useState<TallerRaw[]>([]);
  const [inscripciones, setInscripciones] = useState<Inscripcion[]>([]);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  async function cargar() {
    setLoading(true);
    setError('');
    try {
      const token = await getAccessTokenSilently();
      const [catalogo, mias] = await Promise.all([apiGetEventos(), request('/api/mis-inscripciones', { token })]);
      setTalleres(catalogo as TallerRaw[]);
      setInscripciones(mias as Inscripcion[]);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => { cargar(); }, []);

  async function marcar(tallerId: number) {
    setBusyId(tallerId);
    setError('');
    try {
      const token = await getAccessTokenSilently();
      await request('/api/eventos/' + tallerId + '/inscripcion', { method: 'POST', token });
      await cargar();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusyId(null);
    }
  }

  const idsPendientes = new Set(inscripciones.filter((i) => i.estado === 'pendiente').map((i) => i.tallerId));
  const idsAsistidos = new Set(inscripciones.filter((i) => i.estado === 'asistio').map((i) => i.tallerId));

  return (
    <div className="contenedor" style={{ paddingBlock: 30 }}>
      <h1>Mis talleres pendientes</h1>
      <p>Marca los talleres a los que piensas asistir. Quedan "pendientes" hasta que te acredites con QR o código.</p>
      {error && <p role="alert" className="alert alert-warning">{error}</p>}
      {loading ? <p role="status">Cargando…</p> : !talleres.length ? <p>No hay talleres publicados todavía.</p> : (
        <div className="proyectos-grid">
          {talleres.map((t) => {
            const pendiente = idsPendientes.has(t.id);
            const asistido = idsAsistidos.has(t.id);
            const restringido = (t.rolesPermitidos && t.rolesPermitidos.trim()) || (t.zonaRestringida && t.zonaRestringida.trim());
            return (
              <article key={t.id} className="tarjeta-proyecto">
                <div className="tarjeta-body">
                  <h3>{t.title}</h3>
                  <p className="tarjeta-desc">{t.comuna} · {t.fechaHora}</p>
                  {restringido && (
                    <p className="tarjeta-desc" title="Este taller restringe quién puede unirse">
                      🔒 {t.rolesPermitidos ? 'Solo: ' + t.rolesPermitidos : ''}
                      {t.rolesPermitidos && t.zonaRestringida ? ' · ' : ''}
                      {t.zonaRestringida ? 'Zona: ' + t.zonaRestringida : ''}
                    </p>
                  )}
                  {asistido ? (
                    <span className="pill pill-ok">✔ Asististe</span>
                  ) : pendiente ? (
                    <span className="pill pill-pendiente">Pendiente de asistir</span>
                  ) : (
                    <button className="btn btn-secondary btn-sm" disabled={busyId === t.id} onClick={() => marcar(t.id)}>
                      {busyId === t.id ? 'Marcando…' : 'Marcar que voy'}
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
