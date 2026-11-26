import { useEffect, useState, type FormEvent } from 'react';
import { useSession } from '../auth/SessionProvider';
import { request } from '../clients/api';
import { isStaleVersionConflict } from '../utils/errors';
import { ROLES, roleLabel } from '../config/auth';
import type { Proyecto, Taller, TallerRaw } from '../types';

type TallerForm = Partial<Pick<TallerRaw, 'id' | 'version'>>
  & Omit<TallerRaw, 'id' | 'version' | 'rolesPermitidos' | 'zonaRestringida'>
  & { rolesPermitidos: string[]; zonaRestringida: string };
type TextField = 'title' | 'descripcion' | 'area' | 'unidadVecinal' | 'comuna' | 'direccion' | 'fechaHora' | 'docenteLider' | 'cuposMaximos';
type GeoField = 'latitud' | 'longitud' | 'radioMetros';

const DOMINIO_ROLES = Object.values(ROLES);

const blank: TallerForm = {
  title: '', descripcion: '', area: '', unidadVecinal: '', comuna: '', direccion: '', fechaHora: '',
  docenteLider: '', cuposMaximos: 20, proyectoId: '', latitud: '', longitud: '', radioMetros: '',
  rolesPermitidos: [], zonaRestringida: '',
};

const labels: Record<TextField, string> = {
  title: 'Nombre del taller', descripcion: 'Descripción', area: 'Área temática', unidadVecinal: 'Unidad vecinal',
  comuna: 'Comuna', direccion: 'Dirección', fechaHora: 'Fecha y hora', docenteLider: 'Coordinador', cuposMaximos: 'Cupos máximos',
};

const geoFields: [GeoField, string, number, number][] = [
  ['latitud', 'Latitud', -90, 90], ['longitud', 'Longitud', -180, 180], ['radioMetros', 'Radio en metros', 10, 5000],
];

const pickFormFields = (t: Taller): TallerForm => ({
  id: t.id, version: t.version, title: t.title, descripcion: t.descripcion, area: t.area,
  unidadVecinal: t.unidadVecinal, comuna: t.comuna, direccion: t.direccion, fechaHora: t.fechaHora,
  docenteLider: t.docenteLider, cuposMaximos: t.cuposMaximos, proyectoId: t.proyectoId ?? '',
  latitud: t.latitud ?? '', longitud: t.longitud ?? '', radioMetros: t.radioMetros ?? '',
  rolesPermitidos: (t.rolesPermitidos ?? '').split(',').map((r) => r.trim()).filter(Boolean),
  zonaRestringida: t.zonaRestringida ?? '',
});

export interface TalleresAdminPageProps {
  talleres: Taller[];
  onRefresh: () => Promise<void>;
}

export default function TalleresAdminPage({ talleres, onRefresh }: TalleresAdminPageProps) {
  const { getAccessTokenSilently } = useSession();
  const [form, setForm] = useState<TallerForm>(blank);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [loadingProjects, setLoadingProjects] = useState(true);
  useEffect(() => {
    getAccessTokenSilently().then((token) => request('/api/proyectos', { token })).then(setProyectos).catch((e) => setError(e.message)).finally(() => setLoadingProjects(false));
  }, []);
  async function save(e: FormEvent) {
    e.preventDefault();
    setError('');
    setMessage('');
    const location = [form.latitud, form.longitud, form.radioMetros].map((value) => value !== '' && value != null);
    if (location.some(Boolean) && !location.every(Boolean)) { setError('Completa latitud, longitud y radio, o deja los tres vacíos.'); return; }
    setBusy(true);
    try {
      await request('/api/eventos' + (form.id ? '/' + form.id : ''), {
        method: form.id ? 'PUT' : 'POST',
        body: {
          ...form,
          proyectoId: form.proyectoId ? Number(form.proyectoId) : null,
          latitud: form.latitud === '' ? null : Number(form.latitud),
          longitud: form.longitud === '' ? null : Number(form.longitud),
          radioMetros: form.radioMetros === '' ? null : Number(form.radioMetros),
          rolesPermitidos: form.rolesPermitidos.length ? form.rolesPermitidos.join(',') : null,
          zonaRestringida: form.zonaRestringida === '' ? null : form.zonaRestringida,
        },
        token: await getAccessTokenSilently(),
      });
      await onRefresh();
      setForm(blank);
      setMessage('Taller guardado.');
    } catch (e) {
      const err = e as Error;
      setError(err.message);
      if (isStaleVersionConflict(err)) onRefresh().catch(() => {});
    } finally {
      setBusy(false);
    }
  }
  async function remove(t: Taller) {
    if (!window.confirm('¿Eliminar el taller ' + t.title + '?')) return;
    setBusy(true);
    setError('');
    try {
      await request('/api/eventos/' + t.id, { method: 'DELETE', token: await getAccessTokenSilently() });
      await onRefresh();
      if (form.id === t.id) setForm(blank);
      setMessage('Taller eliminado.');
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return <div className="contenedor" style={{ paddingBlock: 30 }}><h1>Gestión de talleres</h1>
    {error && <p role="alert" className="alert alert-warning">{error}</p>}{message && <p role="status">{message}</p>}
    <div className="crud-grid"><form className="panel" onSubmit={save}><h2>{form.id ? 'Editar taller' : 'Nuevo taller'}</h2>
      <div className="form-field"><label htmlFor="t-proyecto">Proyecto comunitario</label><select id="t-proyecto" className="form-input" disabled={loadingProjects} value={form.proyectoId ?? ''} onChange={(e) => setForm({ ...form, proyectoId: e.target.value })}><option value="">Sin proyecto asociado</option>{proyectos.map((p) => <option key={p.id} value={p.id}>{p.title}</option>)}</select>{loadingProjects && <small role="status">Cargando proyectos…</small>}</div>
      {(Object.keys(labels) as TextField[]).map((key) => <div className="form-field" key={key}><label htmlFor={'t-' + key}>{labels[key]}</label><input id={'t-' + key} className="form-input" required type={key === 'cuposMaximos' ? 'number' : key === 'fechaHora' ? 'datetime-local' : 'text'} min={key === 'cuposMaximos' ? 1 : undefined} max={key === 'cuposMaximos' ? 10000 : undefined} maxLength={key === 'descripcion' ? 2000 : key === 'fechaHora' ? 50 : 100} value={form[key]} onChange={(e) => setForm({ ...form, [key]: key === 'cuposMaximos' ? Number(e.target.value) : e.target.value } as TallerForm)} /></div>)}
      <fieldset><legend>Ubicación del taller (opcional)</legend><p>Si completas estos tres datos, la asistencia exigirá GPS dentro del radio indicado.</p>
        {geoFields.map(([key, label, min, max]) => <div className="form-field" key={key}><label htmlFor={'t-' + key}>{label}</label><input id={'t-' + key} className="form-input" type="number" step={key === 'radioMetros' ? '1' : 'any'} min={min} max={max} value={form[key] ?? ''} onChange={(e) => setForm({ ...form, [key]: e.target.value })} /></div>)}</fieldset>
      <fieldset><legend>Dominio de acceso (opcional)</legend><p>Deja todo vacío para que cualquiera pueda ver y unirse al taller.</p>
        <div className="form-field"><span>Roles que pueden unirse</span>{DOMINIO_ROLES.map((rol) => <label key={rol} style={{ display: 'block' }}><input type="checkbox" checked={form.rolesPermitidos.includes(rol)} onChange={(e) => setForm({ ...form, rolesPermitidos: e.target.checked ? [...form.rolesPermitidos, rol] : form.rolesPermitidos.filter((r) => r !== rol) })} /> {roleLabel(rol)}</label>)}</div>
        <div className="form-field"><label htmlFor="t-zona">Zona / unidad vecinal exigida</label><input id="t-zona" className="form-input" maxLength={120} value={form.zonaRestringida} onChange={(e) => setForm({ ...form, zonaRestringida: e.target.value })} /></div>
      </fieldset>
      <button className="btn btn-primary" disabled={busy}>{busy ? 'Guardando…' : 'Guardar'}</button> <button type="button" className="btn btn-secondary" disabled={busy} onClick={() => setForm(blank)}>Cancelar edición</button></form>
      <div><h2>Talleres registrados</h2>{!talleres.length && <p>No hay talleres. Crea el primero.</p>}
        {talleres.map((t) => <article className="panel maintainer-card" key={t.id}><p className="eyebrow">{t.area} · {proyectos.find((p) => p.id === t.proyectoId)?.title || 'Actividad independiente'}</p><h3>{t.title}</h3><p>{t.comuna} · {t.fechaHora} · {t.cuposMaximos} cupos</p><button className="btn btn-secondary" disabled={busy} onClick={() => setForm(pickFormFields(t))}>Editar</button> <button className="btn btn-secondary" disabled={busy} onClick={() => remove(t)}>Eliminar</button></article>)}</div></div></div>;
}
