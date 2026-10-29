import React from 'react';
import {Link} from 'wouter';
import {useUserRoles} from '../hooks/useRoles';
const views=[
 ['admin','Administración',[['/proyectos','Gestionar proyectos'],['/talleres','Gestionar talleres'],['/panel','Ver quórum'],['/sincronizacion','Revisar sincronización']]],
 ['coordinador','Coordinación de proyectos',[['/proyectos','Crear y editar proyectos'],['/talleres','Crear y editar talleres'],['/panel','Revisar asistencia'],['/acreditar','Acreditar asistentes']]],
 ['monitor','Operación en terreno',[['/acreditar','Acreditar por QR'],['/sincronizacion','Sincronizar marcaciones'],['/','Consultar talleres']]],
 ['participante','Mis actividades',[['/mi-qr','Mostrar mi QR'],['/','Ver talleres'],['/borradores','Mis encuestas'],['/sincronizacion','Enviar encuestas pendientes']]]
];
export default function RoleDashboard(){
 const roles=useUserRoles();const view=views.find(([role])=>roles.includes(role));
 return <div className="contenedor" style={{paddingBlock:30}}><h1>{view?.[1]||'Perfil pendiente'}</h1>{view?<div className="grid-beneficios">{view[2].map(([url,label])=><Link href={url} key={url} className="panel"><h2>{label}</h2></Link>)}</div>:<p>Solicita al administrador que asigne tu grupo en Cognito.</p>}</div>;
}
