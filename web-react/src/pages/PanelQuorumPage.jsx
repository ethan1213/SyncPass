import React,{useEffect,useState} from 'react';
import {useSession} from '../auth/SessionProvider';
import {request} from '../clients/api';
export default function PanelQuorumPage({talleres}){
 const {getAccessTokenSilently}=useSession();const [counts,setCounts]=useState([]),[error,setError]=useState('');
 async function load(){setError('');try{setCounts(await request('/api/quorum',{token:await getAccessTokenSilently()}));}catch(e){setError(e.message);}}
 useEffect(()=>{load();},[]);
 return <div className="contenedor" style={{paddingBlock:30}}><h1>Quórum registrado</h1><button className="btn btn-secondary" onClick={load}>Actualizar</button>{error&&<p role="alert">{error}</p>}
 {talleres.map(t=><article className="panel" key={t.id}><h2>{t.title}</h2><p>{counts.find(c=>c.id===t.id)?.asistentes ?? '—'} asistencias confirmadas · {t.cuposMaximos} cupos</p></article>)}</div>;
}
