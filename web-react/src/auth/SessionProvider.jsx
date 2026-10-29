import React,{createContext,useContext,useState} from 'react';
import {API_URL} from '../config/auth';
import {request} from '../clients/api';
// La web nunca llama a Cognito: registro y login van a la API (API Gateway -> EC2), que habla con Cognito.
const configured=Boolean(API_URL);
const STORE='syncpass.session';
const Context=createContext(null);
const decode=jwt=>{try{const b=jwt.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');return JSON.parse(decodeURIComponent(atob(b).split('').map(c=>'%'+c.charCodeAt(0).toString(16).padStart(2,'0')).join('')));}catch{return null;}};
const load=()=>{try{return JSON.parse(sessionStorage.getItem(STORE));}catch{return null;}};
const save=s=>{try{s?sessionStorage.setItem(STORE,JSON.stringify(s)):sessionStorage.removeItem(STORE);}catch{}};
const withExpiry=t=>({...t,expiresAt:Date.now()+t.expiresIn*1000});
export function SessionProvider({children}){
 const [session,setSession]=useState(load);
 const [error,setError]=useState(null);
 const store=s=>{save(s);setSession(s);};
 async function login(username,password){setError(null);store(withExpiry(await request('/api/auth/login',{method:'POST',body:{username,password}})));}
 const register=data=>request('/api/auth/register',{method:'POST',body:data});
 const confirm=(username,code)=>request('/api/auth/confirm',{method:'POST',body:{username,code}});
 const resend=username=>request('/api/auth/resend',{method:'POST',body:{username}});
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
   catch(e){store(null);throw new Error('Tu sesión expiró. Inicia sesión.');}
  }
  return current.accessToken;
 }
 const user=session?decode(session.idToken):null;
 const value={configured,isLoading:false,error,user,isAuthenticated:Boolean(user),
  loginWithRedirect:()=>window.location.assign('/login'),login,register,confirm,resend,logout,getAccessTokenSilently};
 return <Context.Provider value={value}>{children}</Context.Provider>;
}
export const useSession=()=>useContext(Context);
