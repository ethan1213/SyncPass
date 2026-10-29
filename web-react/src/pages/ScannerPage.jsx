import React,{useState,useRef,useEffect} from 'react';
import {BrowserQRCodeReader} from '@zxing/browser';
import {parseParticipantCode} from '../data/participantQr';
export default function ScannerPage({talleres,onAddAttendance,isOfflineMode}){
 const [tallerId,setTallerId]=useState(''),[code,setCode]=useState(''),[message,setMessage]=useState(''),[error,setError]=useState(''),[busy,setBusy]=useState(false);
 const video=useRef(null),controls=useRef(null),cancelled=useRef(false);const [camera,setCamera]=useState(false);
 const stop=()=>{cancelled.current=true;controls.current?.stop();controls.current=null;setCamera(false);};
 useEffect(()=>()=>{cancelled.current=true;controls.current?.stop();},[]);
 async function scan(){setError('');cancelled.current=false;setCamera(true);try{const reader=new BrowserQRCodeReader();const active=await reader.decodeFromVideoDevice(undefined,video.current,(result,err,ctl)=>{if(result){try{setCode(parseParticipantCode(result.getText()));setMessage('Código leído. Revisa el taller y guarda la marcación.');ctl.stop();stop();}catch(e){setError(e.message);}}});if(cancelled.current)active.stop();else controls.current=active;}catch(e){setError('No se pudo abrir la cámara. Revisa el permiso o ingresa el código manualmente.');setCamera(false);}}
 async function record(e){e.preventDefault();setBusy(true);setError('');try{await onAddAttendance(Number(tallerId),{participante:parseParticipantCode(code)});setMessage('Marcación guardada en el dispositivo. Pendiente de sincronización.');setCode('');}catch(e){setError(e.message);}finally{setBusy(false);}}
 return <div className="contenedor" style={{paddingBlock:30}}><h1>Acreditación</h1><p>Lee el QR o ingresa el identificador ficticio del participante.</p>
 <video ref={video} muted playsInline style={{width:'100%',maxWidth:420,display:camera?'block':'none'}}/>
 <button type="button" className="btn btn-secondary" onClick={camera?stop:scan}>{camera?'Detener cámara':'Leer código QR con cámara'}</button>
 <p>{isOfflineMode?'Sin conexión':'Con conexión'}</p>{error&&<p role="alert">{error}</p>}{message&&<p role="status">{message}</p>}
 <form className="form-panel" onSubmit={record}><div className="form-field"><label htmlFor="scan-event">Taller</label><select id="scan-event" className="form-input" required value={tallerId} onChange={e=>setTallerId(e.target.value)}><option value="">Selecciona un taller</option>{talleres.map(t=><option key={t.id} value={t.id}>{t.title}</option>)}</select></div>
 <div className="form-field"><label htmlFor="scan-code">Código del participante</label><input id="scan-code" className="form-input" maxLength={100} required value={code} onChange={e=>setCode(e.target.value)} placeholder="SP1:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"/></div>
 <button className="btn btn-primary" disabled={busy}>Guardar marcación</button></form></div>;
}
