import React,{useState} from 'react';
import {Link} from 'wouter';
import {useSession} from '../auth/SessionProvider';
import {useUserRoles} from '../hooks/useRoles';
import {roleLabel} from '../config/auth';
export default function LoginPage(){
 const {isAuthenticated,login,logout,configured,user}=useSession();
 const roles=useUserRoles();
 const [form,setForm]=useState({username:'',password:''}),[error,setError]=useState(''),[busy,setBusy]=useState(false);
 const set=e=>setForm({...form,[e.target.name]:e.target.value});
 async function submit(e){e.preventDefault();setError('');setBusy(true);try{await login(form.username.trim(),form.password);}catch(err){setError(err.message);}finally{setBusy(false);}}
 return <div className="contenedor"><div className="form-panel"><h1>Acceso a SyncPass</h1>
 <p className="subtitulo">Tus credenciales se envían a la API de SyncPass (EC2), que las valida con Amazon Cognito.</p>
 {!configured && <p className="alert alert-warning">Falta configurar VITE_API_URL con la URL de API Gateway.</p>}
 {isAuthenticated?<><p role="status">Sesión iniciada correctamente.</p><p>Correo: <strong>{user?.email || 'No disponible'}</strong></p><p>Perfil: {roles.map(roleLabel).join(' · ') || 'Pendiente de asignación'}</p><Link href="/mi-panel" className="btn btn-primary">Ir a mi panel</Link><button className="btn btn-secondary" onClick={logout}>Cerrar sesión</button></>:
 <form onSubmit={submit} noValidate>
  {error && <p className="alert alert-warning" role="alert">{error}</p>}
  <div className="form-field"><label htmlFor="login-user">Usuario</label><input id="login-user" name="username" className="form-input" autoComplete="username" required value={form.username} onChange={set}/></div>
  <div className="form-field"><label htmlFor="login-pass">Contraseña</label><input id="login-pass" name="password" type="password" className="form-input" autoComplete="current-password" required value={form.password} onChange={set}/></div>
  <button className="btn btn-primary btn-block" disabled={!configured || busy || !form.username || !form.password}>{busy?'Ingresando…':'Iniciar sesión'}</button>
  <p className="subtitulo">¿No tienes cuenta? <Link href="/registro">Regístrate</Link>.</p>
 </form>}
 </div></div>;
}
