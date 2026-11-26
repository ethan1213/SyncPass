import {API_URL} from '../config/auth';
type RequestOptions<Body> = {method?: string; body?: Body; token?: string; signal?: AbortSignal};
export class ApiHttpError extends Error {
 constructor(readonly status: number, message: string, readonly code?: string) { super(message); }
}
export async function request<ResponseBody = any, Body = unknown>(
 path: string, {method='GET',body,token,signal}: RequestOptions<Body> = {},
): Promise<ResponseBody>{
 const multipart = body instanceof FormData;
 const res=await fetch(API_URL+path,{method,signal,headers:{...(body && !multipart?{'Content-Type':'application/json'}:{}),...(token?{Authorization:'Bearer '+token}:{})},body:multipart?body:body?JSON.stringify(body):undefined});
 if (!res.ok) {
   const data = await res.json().catch(() => null);
   const sugerencias: Record<number, string> = {
     401: 'Tu sesión expiró. Inicia sesión.',
     403: 'Tu perfil no tiene permiso.',
     404: 'Este recurso no está disponible. Vuelve a la lista y actualízala.',
     409: 'El registro cambió o ya existe. Recarga y revisa los datos.',
     503: 'El servicio está temporalmente ocupado o no disponible. Vuelve a intentarlo.',
   };
   throw new ApiHttpError(res.status, data?.message || sugerencias[res.status] || `No se pudo completar la operación (${res.status}).`, data?.code);
 }
 return res.status===204?null:res.json();
}
export const apiGet=(path: string,token?: string)=>request(path,{token});
export const apiPost=(path: string,body: unknown,token?: string)=>request(path,{method:'POST',body,token});
export const apiGetEventos=()=>apiGet('/api/eventos');
export const apiSyncBatch=(registros: unknown[],token: string)=>apiPost('/api/sync/batch',{registros},token);
