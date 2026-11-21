import { Link } from 'wouter';
import { useUserRoles } from '../hooks/useRoles';

type ViewEntry = [string, [string, string][]];

const views: [string, ViewEntry[1]][] = [
  ['admin', [['/proyectos', 'Gestionar proyectos'], ['/talleres', 'Gestionar talleres'], ['/panel', 'Ver quórum'], ['/sincronizacion', 'Revisar sincronización']]],
  ['coordinador', [['/proyectos', 'Crear y editar proyectos'], ['/talleres', 'Crear y editar talleres'], ['/panel', 'Revisar asistencia'], ['/acreditar', 'Acreditar asistentes']]],
  ['monitor', [['/acreditar', 'Acreditar por QR'], ['/sincronizacion', 'Sincronizar marcaciones'], ['/', 'Consultar talleres']]],
  ['participante', [['/mi-qr', 'Mostrar mi QR'], ['/', 'Ver talleres'], ['/borradores', 'Mis encuestas'], ['/sincronizacion', 'Enviar encuestas pendientes']]],
];

const titles: Record<string, string> = {
  admin: 'Administración',
  coordinador: 'Coordinación de proyectos',
  monitor: 'Operación en terreno',
  participante: 'Mis actividades',
};

export default function RoleDashboard() {
  const roles = useUserRoles();
  const view = views.find(([role]) => roles.includes(role));
  return <div className="contenedor" style={{ paddingBlock: 30 }}><h1>{view ? titles[view[0]] : 'Perfil pendiente'}</h1>{view ? <div className="grid-beneficios">{view[1].map(([url, label]) => <Link href={url} key={url} className="panel"><h2>{label}</h2></Link>)}</div> : <p>Solicita al administrador que asigne tu grupo en Cognito.</p>}</div>;
}
