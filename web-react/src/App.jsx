import React,{useEffect,useState,useRef} from 'react';
import {Link,Route,Switch,useLocation} from 'wouter';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import RequireAuth from './components/RequireAuth';
import HomePage from './pages/HomePage';
import ScannerPage from './pages/ScannerPage';
import SyncPage from './pages/SyncPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import TallerDetailPage from './pages/TallerDetailPage';
import PanelQuorumPage from './pages/PanelQuorumPage';
import TalleresAdminPage from './pages/TalleresAdminPage';
import ProyectosAdminPage from './pages/ProyectosAdminPage';
import DraftsPage from './pages/DraftsPage';
import RoleDashboard from './pages/RoleDashboard';
import MyQrPage from './pages/MyQrPage';
import {ROLES} from './config/auth';
import {useSession} from './auth/SessionProvider';
import {apiGetEventos,apiSyncBatch} from './clients/api';
import {listLocal,saveLocal,removeLocal,cachedCatalog,cacheCatalog} from './data/localDb';
const staff=[ROLES.admin,ROLES.coordinator,ROLES.volunteer];
export default function App(){
 const [,navigate]=useLocation();
 const {user,isLoading,getAccessTokenSilently}=useSession();
 const owner=user?.sub;
 const [talleres,setTalleres]=useState([]),[unsyncedItems,setQueue]=useState([]),[error,setError]=useState(''),[isOfflineMode,setOffline]=useState(!navigator.onLine);
 const syncing=useRef(false);
 const normalize=rows=>rows.map(t=>({...t,image:'/img/taller-placeholder.svg',asistentes:[],inscritos:Number(t.inscritos||0),quorumPct:t.cuposMaximos?Math.min(100,Math.round(Number(t.inscritos||0)/t.cuposMaximos*100)):0}));
 async function refresh(){const rows=await apiGetEventos();setTalleres(normalize(rows));await cacheCatalog(rows);setError('');}
 useEffect(()=>{refresh().catch(async()=>{const rows=await cachedCatalog();if(rows)setTalleres(normalize(rows));setError(rows?'Mostrando el catálogo guardado; no se pudo actualizar.':'La API no está disponible. No se pudieron cargar los talleres.');});},[]);
 useEffect(()=>{setQueue([]);if(owner)listLocal('queue',owner).then(setQueue).catch(e=>setError(e.message));},[owner]);
 useEffect(()=>{const online=()=>setOffline(false),offline=()=>setOffline(true);window.addEventListener('online',online);window.addEventListener('offline',offline);return()=>{window.removeEventListener('online',online);window.removeEventListener('offline',offline);};},[]);
 async function queue(record){
  if(!owner)throw new Error('Inicia sesión antes de guardar.');
  await saveLocal('queue',{...record,owner});
  setQueue(await listLocal('queue',owner));
 }
 async function sync(){
  if(syncing.current)throw new Error('Ya hay una sincronización en curso.');
  if(!navigator.onLine)throw new Error('No hay conexión. Se conservaron los pendientes.');
  syncing.current=true;let count=0;
  try{
   const rows=await listLocal('queue',owner);const token=await getAccessTokenSilently();
   for(let offset=0;offset<rows.length;offset+=100){
    const batch=rows.slice(offset,offset+100);
    const result=await apiSyncBatch(batch.map(({owner,...row})=>row),token);
    const ids=new Set(batch.map(r=>r.id));
    for(const id of result.acceptedIds||[]){if(ids.has(id)){await removeLocal('queue',id,owner);count++;}}
   }
   return count;
  }finally{syncing.current=false;setQueue(await listLocal('queue',owner));}
 }
 const protect=(children,roles)=> <RequireAuth roles={roles}>{children}</RequireAuth>;
 return <div className="layout-app"><a className="salto-contenido" href="#contenido">Saltar al contenido principal</a><Navbar offlineCount={unsyncedItems.length} isOfflineMode={isOfflineMode}/>
 <div id="contenido" className="layout-cuerpo">{error&&!['/login','/registro'].includes(window.location.pathname)&&<p className="contenedor alert alert-warning" role="status">{error}</p>}
 {isLoading?<p className="contenedor">Verificando sesión…</p>:<Switch>
 <Route path="/"><HomePage talleres={talleres} onSelectTaller={t=>navigate('/taller/'+t.id)}/></Route>
 <Route path="/taller/:id">{params=><TallerDetailPage taller={talleres.find(t=>String(t.id)===params.id)}/>}</Route>
 <Route path="/mi-panel">{protect(<RoleDashboard/>)}</Route>
 <Route path="/mi-qr">{protect(<MyQrPage/>)}</Route>
 <Route path="/talleres">{protect(<TalleresAdminPage talleres={talleres} onRefresh={refresh}/>,[ROLES.admin,ROLES.coordinator])}</Route>
 <Route path="/proyectos">{protect(<ProyectosAdminPage/>,[ROLES.admin,ROLES.coordinator])}</Route>
 <Route path="/acreditar">{protect(<ScannerPage talleres={talleres} isOfflineMode={isOfflineMode} onAddAttendance={(tallerId,a)=>queue({id:crypto.randomUUID(),tipo:'asistencia',tallerId,participante:a.participante,timestamp:new Date().toISOString()})}/>,staff)}</Route>
 <Route path="/encuesta">{protect(<DraftsPage talleres={talleres} onQueue={queue}/>)}</Route>
 <Route path="/borradores">{protect(<DraftsPage talleres={talleres} onQueue={queue}/>)}</Route>
 <Route path="/sincronizacion">{protect(<SyncPage unsyncedItems={unsyncedItems} isOfflineMode={isOfflineMode} onPerformSync={sync}/>)}</Route>
 <Route path="/panel">{protect(<PanelQuorumPage talleres={talleres}/>,staff)}</Route>
 <Route path="/login"><LoginPage/></Route><Route path="/registro"><RegisterPage/></Route>
 <Route><div className="contenedor estado-bloque"><h1>Página no encontrada</h1><Link href="/">Volver al inicio</Link></div></Route>
 </Switch>}</div><Footer/></div>;
}
