import React,{createContext,useContext,useState,type ReactNode} from 'react';
import {API_URL} from '../config/auth';
import {request} from '../clients/api';
// La web nunca llama a Cognito: registro y login van a la API (API Gateway -> EC2), que habla con Cognito.
const configured=Boolean(API_URL);
const STORE='syncpass.session';
type Tokens={accessToken:string;idToken:string;refreshToken:string;expiresIn:number};
type Session=Tokens&{expiresAt:number};
type UserClaims={sub?:string;nickname?:string;email?:string;[key:string]:unknown};
type Registration={username:string;email:string;nickname:string;password:string};
type SessionContext={configured:boolean;isLoading:boolean;error:string|null;user:UserClaims|null;isAuthenticated:boolean;
 loginWithRedirect:()=>void;login:(username:string,password:string)=>Promise<void>;
 register:(data:Registration)=>Promise<{userConfirmed:boolean;destination:string}>;
 confirm:(username:string,code:string)=>Promise<unknown>;resend:(username:string)=>Promise<unknown>;
 logout:()=>Promise<void>;getAccessTokenSilently:()=>Promise<string>};
const Context=createContext<SessionContext|null>(null);
const decode=(jwt:string):UserClaims|null=>{try{const b=jwt.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');return JSON.parse(decodeURIComponent(atob(b).split('').map(c=>'%'+c.charCodeAt(0).toString(16).padStart(2,'0')).join('')));}catch{return null;}};
const load=():Session|null=>{try{const raw=sessionStorage.getItem(STORE);return raw?JSON.parse(raw):null;}catch{return null;}};
const save=(s:Session|null)=>{try{s?sessionStorage.setItem(STORE,JSON.stringify(s)):sessionStorage.removeItem(STORE);}catch{}};
const withExpiry=(t:Tokens):Session=>({...t,expiresAt:Date.now()+t.expiresIn*1000});
export function SessionProvider({children}:{children:ReactNode}){
 const [session,setSession]=useState(load);
 const [error,setError]=useState<string|null>(null);
 const store=(s:Session|null)=>{save(s);setSession(s);};
 async function login(username:string,password:string){setError(null);store(withExpiry(await request('/api/auth/login',{method:'POST',body:{username,password}})));}
 const register=(data:Registration)=>request('/api/auth/register',{method:'POST',body:data});
 const confirm=(username:string,code:string)=>request('/api/auth/confirm',{method:'POST',body:{username,code}});
 const resend=(username:string)=>request('/api/auth/resend',{method:'POST',body:{username}});
 async function logout(){
  const current=session;store(null);
  if(current?.refreshToken)await request('/api/auth/logout',{method:'POST',body:{refreshToken:current.refreshToken}}).catch(()=>{});
  window.location.assign('/');
 }
 async function getAccessTokenSilently(){
  let current=load();
  if(!current)throw new Error('Inicia sesión para continuar.');
  if(current.expiresAt-Date.now()<60000){
   try{current=withExpiry(await request('/api/auth/refresh',{method:'POST',body:{refreshToken:current.refreshToken}}));store(current);}
   catch{store(null);throw new Error('Tu sesión expiró. Inicia sesión.');}
  }
  return current.accessToken;
 }
 const user=session?decode(session.idToken):null;
 const value:SessionContext={configured,isLoading:false,error,user,isAuthenticated:Boolean(user),
  loginWithRedirect:()=>window.location.assign('/login'),login,register,confirm,resend,logout,getAccessTokenSilently};
 return <Context.Provider value={value}>{children}</Context.Provider>;
}
export const useSession=():SessionContext=>{
 const session=useContext(Context);
 if(!session)throw new Error('SessionProvider no está montado.');
 return session;
};
