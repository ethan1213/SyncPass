import { useCallback, useEffect, useState, useRef } from 'react';
import { Link, Route, Switch, useLocation } from 'wouter';
import Navbar from './components/organisms/Navbar';
import Footer from './components/organisms/Footer';
import RequireAuth from './components/RequireAuth';
import HomePage from './pages/HomePage';
import ScannerPage from './pages/ScannerPage';
import SyncPage from './pages/SyncPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import TallerDetailPage from './pages/TallerDetailPage';
import PanelQuorumPage from './pages/PanelQuorumPage';
import TalleresAdminPage from './pages/TalleresAdminPage';
import GestionTallerPage from './pages/GestionTallerPage';
import ProyectosAdminPage from './pages/ProyectosAdminPage';
import DraftsPage from './pages/DraftsPage';
import RoleDashboard from './pages/RoleDashboard';
import MyQrPage from './pages/MyQrPage';
import BitacoraPage from './pages/BitacoraPage';
import MisTalleresPendientesPage from './pages/MisTalleresPendientesPage';
import { ROLES } from './config/auth';
import { useSession } from './auth/SessionProvider';
import { useUserRoles } from './hooks/useRoles';
import { apiGetEventos, apiSyncBatch } from './clients/api';
import { listLocal, saveLocal, removeLocal, cachedCatalog, cacheCatalog } from './data/localDb';
import type { ReactNode } from 'react';
import type { Taller, TallerRaw, QueueRecord } from './types';

const staff = [ROLES.admin, ROLES.coordinator, ROLES.volunteer];

const normalize = (rows: TallerRaw[]): Taller[] =>
  rows.map((t) => ({
    ...t,
    image: '/img/taller-placeholder.svg',
    asistentes: [],
    inscritos: Number(t.inscritos || 0),
    quorumPct: t.cuposMaximos ? Math.min(100, Math.round((Number(t.inscritos || 0) / t.cuposMaximos) * 100)) : 0,
  }));

export default function App() {
  const [, navigate] = useLocation();
  const { user, isLoading, getAccessTokenSilently } = useSession();
  const roles = useUserRoles();
  const owner = user?.sub;
  const [talleres, setTalleres] = useState<Taller[]>([]);
  const [catalogoCargado, setCatalogoCargado] = useState(false);
  const [unsyncedItems, setQueue] = useState<QueueRecord[]>([]);
  const [error, setError] = useState('');
  const [isOfflineMode, setOffline] = useState(!navigator.onLine);
  const syncing = useRef(false);
  const catalogRequest = useRef(0);
  const backgroundRefresh = useRef(false);

  const refresh = useCallback(async () => {
    const numero = ++catalogRequest.current;
    const rows = await apiGetEventos();
    if (numero !== catalogRequest.current) return;
    setTalleres(normalize(rows));
    setCatalogoCargado(true);
    setError('');
    await cacheCatalog(rows).catch(() => {});
  }, []);

  useEffect(() => {
    let activo = true;
    async function actualizar() {
      if (backgroundRefresh.current || document.hidden) return;
      backgroundRefresh.current = true;
      const numero = catalogRequest.current + 1;
      try {
        await refresh();
      } catch {
        const rows = await cachedCatalog<TallerRaw[]>().catch(() => null);
        if (!activo || numero !== catalogRequest.current) return;
        if (rows) setTalleres(normalize(rows));
        setError(rows ? 'Mostrando el catálogo guardado; no se pudo actualizar.' : 'La API no está disponible. No se pudieron cargar los talleres.');
        setCatalogoCargado(true);
      } finally {
        backgroundRefresh.current = false;
      }
    }
    void actualizar();
    const timer = window.setInterval(actualizar, 20000);
    window.addEventListener('focus', actualizar);
    window.addEventListener('online', actualizar);
    document.addEventListener('visibilitychange', actualizar);
    return () => {
      activo = false;
      window.clearInterval(timer);
      window.removeEventListener('focus', actualizar);
      window.removeEventListener('online', actualizar);
      document.removeEventListener('visibilitychange', actualizar);
    };
  }, [refresh]);

  useEffect(() => {
    setQueue([]);
    if (owner) listLocal<QueueRecord>('queue', owner).then(setQueue).catch((e) => setError(e.message));
  }, [owner]);

  useEffect(() => {
    const online = () => setOffline(false);
    const offline = () => setOffline(true);
    window.addEventListener('online', online);
    window.addEventListener('offline', offline);
    return () => {
      window.removeEventListener('online', online);
      window.removeEventListener('offline', offline);
    };
  }, []);

  async function queue(record: QueueRecord) {
    if (!owner) throw new Error('Inicia sesión antes de guardar.');
    await saveLocal<QueueRecord>('queue', { ...record, owner });
    setQueue(await listLocal<QueueRecord>('queue', owner));
  }

  async function sync() {
    if (syncing.current) throw new Error('Ya hay una sincronización en curso.');
    if (!navigator.onLine) throw new Error('No hay conexión. Se conservaron los pendientes.');
    syncing.current = true;
    let count = 0;
    try {
      const rows = await listLocal<QueueRecord>('queue', owner);
      const token = await getAccessTokenSilently();
      for (let offset = 0; offset < rows.length; offset += 100) {
        const batch = rows.slice(offset, offset + 100);
        const result = await apiSyncBatch(batch.map(({ owner, ...row }) => row), token);
        const ids = new Set(batch.map((r) => r.id));
        for (const id of result.acceptedIds || []) {
          if (ids.has(id)) {
            await removeLocal('queue', id, owner);
            count++;
          }
        }
      }
      return count;
    } finally {
      syncing.current = false;
      setQueue(await listLocal<QueueRecord>('queue', owner));
    }
  }

  const protect = (children: ReactNode, roles?: string[]) => <RequireAuth roles={roles}>{children}</RequireAuth>;

  return (
    <div className="layout-app">
      <a className="salto-contenido" href="#contenido">Saltar al contenido principal</a>
      <Navbar offlineCount={unsyncedItems.length} isOfflineMode={isOfflineMode} />
      <div id="contenido" className="layout-cuerpo">
        {error && !['/login', '/registro'].includes(window.location.pathname) && (
          <p className="contenedor alert alert-warning" role="status">{error}</p>
        )}
        {isLoading ? (
          <p className="contenedor">Verificando sesión…</p>
        ) : (
          <Switch>
            <Route path="/"><HomePage talleres={talleres} onSelectTaller={(t) => navigate('/taller/' + t.id)} /></Route>
            <Route path="/taller/:id">{(params) => catalogoCargado
              ? <TallerDetailPage taller={talleres.find((t) => String(t.id) === params.id)}
                  puedeGestionarBitacora={roles.includes(ROLES.admin) || roles.includes(ROLES.coordinator)} />
              : <p className="contenedor" role="status">Cargando taller…</p>}</Route>
            <Route path="/bitacora/:id">{(params) => protect(<BitacoraPage key={params.id} tallerId={params.id} />)}</Route>
            <Route path="/mi-panel">{protect(<RoleDashboard />)}</Route>
            <Route path="/mi-qr">{protect(<MyQrPage />)}</Route>
            <Route path="/mis-talleres">{protect(<MisTalleresPendientesPage />)}</Route>
            <Route path="/talleres/:id/gestion">{({ id }) => {
              const taller = talleres.find((registro) => String(registro.id) === id);
              return protect(taller ? <GestionTallerPage key={id} taller={taller} /> :
                <p className="contenedor" role="status">{catalogoCargado ? 'No encontramos ese taller.' : 'Cargando taller…'}</p>,
              [ROLES.admin, ROLES.coordinator]);
            }}</Route>
            <Route path="/talleres">{protect(<TalleresAdminPage talleres={talleres} onRefresh={refresh} />, [ROLES.admin, ROLES.coordinator])}</Route>
            <Route path="/proyectos">{protect(<ProyectosAdminPage />, [ROLES.admin, ROLES.coordinator])}</Route>
            <Route path="/acreditar">
              {protect(
                <ScannerPage
                  talleres={talleres}
                  isOfflineMode={isOfflineMode}
                  onAddAttendance={(tallerId, a) =>
                    queue({
                      id: crypto.randomUUID(),
                      tipo: 'asistencia',
                      tallerId,
                      participante: a.participante,
                      latitud: a.latitud,
                      longitud: a.longitud,
                      precisionMetros: a.precisionMetros,
                      timestamp: new Date().toISOString(),
                    })
                  }
                />,
                staff,
              )}
            </Route>
            <Route path="/encuesta">{protect(<DraftsPage talleres={talleres} onQueue={queue} />)}</Route>
            <Route path="/borradores">{protect(<DraftsPage talleres={talleres} onQueue={queue} />)}</Route>
            <Route path="/sincronizacion">{protect(<SyncPage unsyncedItems={unsyncedItems} isOfflineMode={isOfflineMode} onPerformSync={sync} />)}</Route>
            <Route path="/panel">{protect(<PanelQuorumPage talleres={talleres} />, staff)}</Route>
            <Route path="/login"><LoginPage /></Route>
            <Route path="/registro"><RegisterPage /></Route>
            <Route>
              <div className="contenedor estado-bloque">
                <h1>Página no encontrada</h1>
                <Link href="/">Volver al inicio</Link>
              </div>
            </Route>
          </Switch>
        )}
      </div>
      <Footer />
    </div>
  );
}
