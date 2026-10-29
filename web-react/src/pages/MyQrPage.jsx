import React,{useEffect,useState} from 'react';
import QRCode from 'qrcode';
import {useSession} from '../auth/SessionProvider';
import {participantCode} from '../data/participantQr';

export default function MyQrPage(){
 const {user}=useSession();
 const code=user?.sub?participantCode(user.sub):'';
 const [image,setImage]=useState(''),[error,setError]=useState('');
 useEffect(()=>{if(!code)return;QRCode.toDataURL(code,{width:320,margin:2,errorCorrectionLevel:'M'}).then(setImage).catch(()=>setError('No se pudo generar el QR.'));},[code]);
 return <div className="contenedor" style={{paddingBlock:30}}><h1>Mi código de acreditación</h1><p>Muestra este QR al monitor al ingresar al taller. También puedes mostrar el código escrito si no hay cámara.</p>
 {error&&<p role="alert">{error}</p>}{image&&<img src={image} alt="QR personal de acreditación SyncPass" width="320" height="320" style={{maxWidth:'100%',background:'#fff',padding:12,borderRadius:12}}/>}
 <p><code style={{overflowWrap:'anywhere'}}>{code}</code></p><p>Este código identifica tu cuenta; evita publicarlo.</p></div>;
}
