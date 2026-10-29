import React,{useEffect,useState} from 'react';
import {useSession} from '../auth/SessionProvider';
import {request} from '../clients/api';
const blank={title:'',descripcion:'',area:'',unidadVecinal:'',comuna:'',direccion:'',fechaHora:'',docenteLider:'',cuposMaximos:20,proyectoId:'',latitud:'',longitud:'',radioMetros:''};
const labels={title:'Nombre del taller',descripcion:'Descripción',area:'Área temática',unidadVecinal:'Unidad vecinal',comuna:'Comuna',direccion:'Dirección',fechaHora:'Fecha y hora',docenteLider:'Coordinador',cuposMaximos:'Cupos máximos'};
export default function TalleresAdminPage({talleres,onRefresh}){
 const {getAccessTokenSilently}=useSession();
 const [form,setForm]=useState(blank),[error,setError]=useState(''),[busy,setBusy]=useState(false),[message,setMessage]=useState('');
 const [proyectos,setProyectos]=useState([]);
 useEffect(()=>{getAccessTokenSilently().then(token=>request('/api/proyectos',{token})).then(setProyectos).catch(e=>setError(e.message));},[]);
 async function save(e){e.preventDefault();setBusy(true);setError('');setMessage('');
  try{await request('/api/eventos'+(form.id?'/'+form.id:''),{method:form.id?'PUT':'POST',body:{...form,proyectoId:form.proyectoId?Number(form.proyectoId):null,latitud:form.latitud===''?null:Number(form.latitud),longitud:form.longitud===''?null:Number(form.longitud),radioMetros:form.radioMetros===''?null:Number(form.radioMetros)},token:await getAccessTokenSilently()});await onRefresh();setForm(blank);setMessage('Taller guardado.');}
  catch(e){setError(e.message);}finally{setBusy(false);}
 }
 async function remove(t){if(!window.confirm('¿Eliminar el taller '+t.title+'?'))return;setBusy(true);setError('');
  try{await request('/api/eventos/'+t.id,{method:'DELETE',token:await getAccessTokenSilently()});await onRefresh();if(form.id===t.id)setForm(blank);setMessage('Taller eliminado.');}
  catch(e){setError(e.message);}finally{setBusy(false);}
 }
 return <div className="contenedor" style={{paddingBlock:30}}><h1>Gestión de talleres</h1>
 {error&&<p role="alert" className="alert alert-warning">{error}</p>}{message&&<p role="status">{message}</p>}
 <div className="crud-grid"><form className="panel" onSubmit={save}><h2>{form.id?'Editar taller':'Nuevo taller'}</h2>
 <div className="form-field"><label htmlFor="t-proyecto">Proyecto comunitario</label><select id="t-proyecto" className="form-input" value={form.proyectoId||''} onChange={e=>setForm({...form,proyectoId:e.target.value})}><option value="">Sin proyecto asociado</option>{proyectos.map(p=><option key={p.id} value={p.id}>{p.title}</option>)}</select></div>
 {Object.entries(labels).map(([key,label])=><div className="form-field" key={key}><label htmlFor={'t-'+key}>{label}</label><input id={'t-'+key} className="form-input" required type={key==='cuposMaximos'?'number':key==='fechaHora'?'datetime-local':'text'} min={key==='cuposMaximos'?1:undefined} max={key==='cuposMaximos'?10000:undefined} maxLength={key==='descripcion'?2000: key==='fechaHora'?50:100} value={form[key]} onChange={e=>setForm({...form,[key]:key==='cuposMaximos'?Number(e.target.value):e.target.value})}/></div>)}
 <fieldset><legend>Ubicación del taller (opcional)</legend><p>Si completas estos tres datos, la asistencia exigirá GPS dentro del radio indicado.</p>
 {[['latitud','Latitud',-90,90],['longitud','Longitud',-180,180],['radioMetros','Radio en metros',10,5000]].map(([key,label,min,max])=><div className="form-field" key={key}><label htmlFor={'t-'+key}>{label}</label><input id={'t-'+key} className="form-input" type="number" step={key==='radioMetros'?'1':'any'} min={min} max={max} value={form[key]??''} onChange={e=>setForm({...form,[key]:e.target.value})}/></div>)}</fieldset>
 <button className="btn btn-primary" disabled={busy}>Guardar</button> <button type="button" className="btn btn-secondary" onClick={()=>setForm(blank)}>Cancelar edición</button></form>
 <div><h2>Talleres registrados</h2>{!talleres.length&&<p>No hay talleres. Crea el primero.</p>}
 {talleres.map(t=><article className="panel maintainer-card" key={t.id}><p className="eyebrow">{t.area} · {proyectos.find(p=>p.id===t.proyectoId)?.title||'Actividad independiente'}</p><h3>{t.title}</h3><p>{t.comuna} · {t.fechaHora} · {t.cuposMaximos} cupos</p><button className="btn btn-secondary" disabled={busy} onClick={()=>setForm(Object.fromEntries([...Object.keys(blank),'id','version'].map(k=>[k,t[k]])))}>Editar</button> <button className="btn btn-secondary" disabled={busy} onClick={()=>remove(t)}>Eliminar</button></article>)}</div></div></div>;
}
