import {API_URL} from '../config/auth';
type RequestOptions = {method?: string; body?: unknown; token?: string; signal?: AbortSignal};
export async function request(path: string,{method='GET',body,token,signal}: RequestOptions={}){
 const res=await fetch(API_URL+path,{method,signal,headers:{...(body?{'Content-Type':'application/json'}:{}),...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined});
 if(!res.ok){const data=await res.json().catch(()=>({}));throw new Error(data.message || ({401:'Tu sesión expiró. Inicia sesión.',403:'Tu perfil no tiene permiso.',409:'El registro cambió o ya existe. Recarga y revisa los datos.'}[res.status]) || 'No se pudo completar la operación ('+res.status+').');}
 return res.status===204?null:res.json();
}
export const apiGet=(path: string,token?: string)=>request(path,{token});
export const apiPost=(path: string,body: unknown,token?: string)=>request(path,{method:'POST',body,token});
export const apiGetEventos=()=>apiGet('/api/eventos');
export const apiSyncBatch=(registros: unknown[],token: string)=>apiPost('/api/sync/batch',{registros},token);
