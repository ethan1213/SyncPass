import React,{useEffect,useState} from 'react';
import {useSession} from '../auth/SessionProvider';
import {listLocal,saveLocal,removeLocal} from '../data/localDb';
const empty={tallerId:'',rating:5,utilidad:'muy_util',comentario:''};
export default function DraftsPage({talleres,onQueue}){
 const {user}=useSession();const owner=user?.sub;
 const [rows,setRows]=useState([]),[form,setForm]=useState(empty),[error,setError]=useState(''),[message,setMessage]=useState(''),[busy,setBusy]=useState(false);
 const reload=async()=>setRows(await listLocal('drafts',owner));
 useEffect(()=>{reload().catch(e=>setError(e.message));},[owner]);
 async function save(e){e.preventDefault();setError('');try{await saveLocal('drafts',{...form,id:form.id||crypto.randomUUID(),owner,tallerId:Number(form.tallerId),updatedAt:new Date().toISOString()});setForm(empty);await reload();setMessage('Borrador guardado en este dispositivo.');}catch(e){setError(e.message);}}
 async function remove(id){try{await removeLocal('drafts',id,owner);await reload();if(form.id===id)setForm(empty);}catch(e){setError(e.message);}}
 async function enqueue(row){setBusy(true);setError('');try{await onQueue({id:row.id,tipo:'encuesta',tallerId:row.tallerId,rating:row.rating,utilidad:row.utilidad,comentario:row.comentario,timestamp:new Date().toISOString()});await removeLocal('drafts',row.id,owner);await reload();setMessage('Encuesta agregada a pendientes de sincronización.');}catch(e){setError(e.message);}finally{setBusy(false);}}
 return <div className="contenedor" style={{paddingBlock:30}}><h1>Borradores de encuestas</h1><p>Se guardan en este navegador y puedes editarlos sin conexión.</p>
 {error&&<p role="alert">{error}</p>}{message&&<p role="status">{message}</p>}
 <form className="panel" onSubmit={save}><h2>{form.id?'Editar borrador':'Nuevo borrador'}</h2><div className="form-field"><label htmlFor="draft-taller">Taller</label><select id="draft-taller" required className="form-input" value={form.tallerId} onChange={e=>setForm({...form,tallerId:e.target.value})}><option value="">Selecciona un taller</option>{talleres.map(t=><option key={t.id} value={t.id}>{t.title}</option>)}</select></div>
 <div className="form-field"><label htmlFor="draft-rating">Satisfacción (1 a 5)</label><input id="draft-rating" className="form-input" required type="number" min="1" max="5" value={form.rating} onChange={e=>setForm({...form,rating:Number(e.target.value)})}/></div>
 <div className="form-field"><label htmlFor="draft-comment">Comentario</label><textarea id="draft-comment" className="form-input" maxLength={2000} value={form.comentario} onChange={e=>setForm({...form,comentario:e.target.value})}/></div>
 <button className="btn btn-primary">Guardar borrador</button> <button type="button" className="btn btn-secondary" onClick={()=>setForm(empty)}>Cancelar edición</button></form>
 <h2>Mis borradores</h2>{!rows.length&&<p>No hay borradores guardados.</p>}{rows.map(r=><article className="panel" key={r.id}><h3>{talleres.find(t=>t.id===r.tallerId)?.title||'Taller '+r.tallerId}</h3><p>{r.rating}/5 · {r.comentario}</p><button className="btn btn-secondary" onClick={()=>setForm(r)}>Editar</button> <button className="btn btn-secondary" onClick={()=>remove(r.id)}>Eliminar</button> <button className="btn btn-primary" disabled={busy} onClick={()=>enqueue(r)}>Enviar a pendientes</button></article>)}</div>;
}
