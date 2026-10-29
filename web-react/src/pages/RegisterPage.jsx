import React,{useState} from 'react';
import {Link,useLocation} from 'wouter';
import {useSession} from '../auth/SessionProvider';
export default function RegisterPage(){
 const {register,confirm,resend,login,configured}=useSession();
 const [,navigate]=useLocation();
 const [form,setForm]=useState({username:'',email:'',nickname:'',password:''}),[code,setCode]=useState('');
 const [step,setStep]=useState('form'),[destination,setDestination]=useState(''),[error,setError]=useState(''),[info,setInfo]=useState(''),[busy,setBusy]=useState(false);
 const set=e=>setForm({...form,[e.target.name]:e.target.value});
 const run=async fn=>{setError('');setInfo('');setBusy(true);try{await fn();}catch(err){setError(err.message);}finally{setBusy(false);}};
 const submit=e=>{e.preventDefault();run(async()=>{const r=await register({...form,username:form.username.trim(),email:form.email.trim()});setDestination(r.destination);if(r.userConfirmed){await login(form.username.trim(),form.password);navigate('/login');}else setStep('code');});};
 const verify=e=>{e.preventDefault();run(async()=>{await confirm(form.username.trim(),code.trim());await login(form.username.trim(),form.password);navigate('/login');});};
 return <div className="contenedor"><div className="form-panel"><h1>Crear cuenta en SyncPass</h1>
 <p className="subtitulo">El registro pasa por la API de SyncPass (EC2), que crea la cuenta en Amazon Cognito.</p>
 {!configured && <p className="alert alert-warning">Falta configurar VITE_API_URL con la URL de API Gateway.</p>}
 {error && <p className="alert alert-warning" role="alert">{error}</p>}
 {info && <p className="alert" role="status">{info}</p>}
 {step==='form'?<form onSubmit={submit} noValidate>
  <div className="form-field"><label htmlFor="reg-user">Usuario</label><input id="reg-user" name="username" className="form-input" autoComplete="username" required minLength={3} value={form.username} onChange={set}/><span className="form-help">Letras, números y . _ - @ +</span></div>
  <div className="form-field"><label htmlFor="reg-email">Correo</label><input id="reg-email" name="email" type="email" className="form-input" autoComplete="email" required value={form.email} onChange={set}/></div>
  <div className="form-field"><label htmlFor="reg-nick">Apodo</label><input id="reg-nick" name="nickname" className="form-input" autoComplete="nickname" required value={form.nickname} onChange={set}/></div>
  <div className="form-field"><label htmlFor="reg-pass">Contraseña</label><input id="reg-pass" name="password" type="password" className="form-input" autoComplete="new-password" required minLength={8} value={form.password} onChange={set}/><span className="form-help">Mínimo 8 caracteres con mayúscula, minúscula, número y símbolo.</span></div>
  <button className="btn btn-primary btn-block" disabled={!configured || busy || Object.values(form).some(v=>!v)}>{busy?'Registrando…':'Crear cuenta'}</button>
  <p className="subtitulo">¿Ya tienes cuenta? <Link href="/login">Inicia sesión</Link>.</p>
 </form>:<form onSubmit={verify} noValidate>
  <p>Enviamos un código de verificación a <strong>{destination || form.email}</strong>.</p>
  <div className="form-field"><label htmlFor="reg-code">Código</label><input id="reg-code" className="form-input" inputMode="numeric" autoComplete="one-time-code" required value={code} onChange={e=>setCode(e.target.value)}/></div>
  <button className="btn btn-primary btn-block" disabled={busy || !code}>{busy?'Verificando…':'Confirmar e ingresar'}</button>
  <button type="button" className="btn btn-secondary btn-block" disabled={busy} onClick={()=>run(async()=>{await resend(form.username.trim());setInfo('Código reenviado.');})}>Reenviar código</button>
 </form>}
 </div></div>;
}
