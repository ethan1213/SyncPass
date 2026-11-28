import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'wouter';
import { useSession } from '../auth/SessionProvider';
import { request } from '../clients/api';
import { isStaleVersionConflict } from '../utils/errors';
import type { Proyecto } from '../types';

type ProyectoForm = Partial<Pick<Proyecto, 'id' | 'version'>> & Omit<Proyecto, 'id' | 'version'>;
type TextField = 'title' | 'descripcion' | 'area' | 'unidadVecinal' | 'fechaInicio' | 'fechaTermino';

const empty: ProyectoForm = { title: '', descripcion: '', area: '', unidadVecinal: '', fechaInicio: '', fechaTermino: '', estado: 'planificado' };
const fields: [TextField, string][] = [
  ['title', 'Nombre del proyecto'], ['descripcion', 'Descripción'], ['area', 'Área temática'],
  ['unidadVecinal', 'Unidad vecinal'], ['fechaInicio', 'Inicio'], ['fechaTermino', 'Término'],
];

export default function ProyectosAdminPage() {
  const { getAccessTokenSilently } = useSession();
  const [rows, setRows] = useState<Proyecto[]>([]);
  const [form, setForm] = useState<ProyectoForm>(empty);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  async function refresh() {
    const token = await getAccessTokenSilently();
    setRows((await request<Proyecto[]>('/api/proyectos', { token })) ?? []);
  }
  useEffect(() => { refresh().catch((e) => setError(e.message)).finally(() => setLoading(false)); }, []);
  async function save(e: FormEvent) {
    e.preventDefault();
    setError('');
    setMessage('');
    if (form.fechaTermino < form.fechaInicio) { setError('La fecha de término debe ser posterior al inicio.'); return; }
    setBusy(true);
    try {
      await request('/api/proyectos' + (form.id ? '/' + form.id : ''), { method: form.id ? 'PUT' : 'POST', body: form, token: await getAccessTokenSilently() });
      await refresh();
      setForm(empty);
      setMessage('Proyecto guardado.');
    } catch (e) {
      const err = e as Error;
      setError(err.message);
      if (isStaleVersionConflict(err)) refresh().catch(() => {});
    } finally {
      setBusy(false);
    }
  }
  async function remove(row: Proyecto) {
    if (!window.confirm('¿Eliminar el proyecto ' + row.title + '?')) return;
    setBusy(true);
    setError('');
    setMessage('');
    try {
      await request('/api/proyectos/' + row.id, { method: 'DELETE', token: await getAccessTokenSilently() });
      await refresh();
      if (form.id === row.id) setForm(empty);
      setMessage('Proyecto eliminado.');
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return <div className="contenedor maintainer" style={{ paddingBlock: 30 }}><div className="maintainer-heading"><p className="eyebrow">Vinculación comunitaria · Mantenedor 01</p><h1>Proyectos comunitarios</h1><p>Planifica iniciativas por territorio. Los talleres pueden asociarse a un proyecto activo.</p></div>
    {error && <p role="alert" className="alert alert-warning">{error}</p>}{message && <p role="status" className="alert alert-success">{message}</p>}
    <div className="crud-grid"><form className="panel" onSubmit={save}><h2>{form.id ? 'Editar proyecto' : 'Nuevo proyecto'}</h2>
      {fields.map(([key, label]) => <div className="form-field" key={key}><label htmlFor={'p-' + key}>{label}</label><input id={'p-' + key} className="form-input" required type={key.startsWith('fecha') ? 'date' : 'text'} maxLength={key === 'descripcion' ? 2000 : 160} value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })} /></div>)}
      <div className="form-field"><label htmlFor="p-estado">Estado</label><select id="p-estado" className="form-input" value={form.estado} onChange={(e) => setForm({ ...form, estado: e.target.value })}><option value="planificado">Planificado</option><option value="activo">Activo</option><option value="finalizado">Finalizado</option></select></div>
      <button className="btn btn-primary" disabled={busy}>{busy ? 'Guardando…' : 'Guardar proyecto'}</button> <button type="button" className="btn btn-secondary" disabled={busy} onClick={() => setForm(empty)}>Cancelar edición</button></form>
      <div><h2>Proyectos registrados</h2><button type="button" className="btn btn-secondary" disabled={busy || loading} onClick={() => { setLoading(true); refresh().catch((e) => setError(e.message)).finally(() => setLoading(false)); }}>Actualizar lista</button>{loading ? <p role="status">Cargando proyectos…</p> : !rows.length && <p>No hay proyectos registrados.</p>}{rows.map((row) => <article className="panel maintainer-card" key={row.id}><p className="eyebrow">{row.area} · {row.estado}</p><h3>{row.title}</h3><p>{row.descripcion}</p><p>{row.unidadVecinal} · {row.fechaInicio} a {row.fechaTermino}</p>
        <div className="acciones-fila">
          <Link className="btn btn-primary" href={`/proyectos/${row.id}/talleres`}>Ver talleres y QR</Link>
          <button className="btn btn-secondary" disabled={busy} onClick={() => { setForm({ ...row }); setError(''); setMessage(''); }}>Editar</button>
          <button className="btn btn-secondary" disabled={busy} onClick={() => remove(row)}>Eliminar</button>
        </div>
      </article>)}</div></div></div>;
}
