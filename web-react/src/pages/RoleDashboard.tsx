import { Link } from 'wouter';
import { useUserRoles } from '../hooks/useRoles';

type ViewEntry = [string, [string, string][]];

const views: [string, ViewEntry[1]][] = [
  ['admin', [['/proyectos', 'Gestionar proyectos'], ['/talleres', 'Gestionar talleres'], ['/panel', 'Ver quórum'], ['/sincronizacion', 'Revisar sincronización']]],
  ['coordinador', [['/proyectos', 'Crear y editar proyectos'], ['/talleres', 'Crear y editar talleres'], ['/panel', 'Revisar asistencia'], ['/acreditar', 'Acreditar asistentes']]],
  ['monitor', [['/acreditar', 'Acreditar por QR'], ['/sincronizacion', 'Sincronizar marcaciones'], ['/', 'Consultar talleres']]],
  ['participante', [['/mi-qr', 'Mostrar mi QR'], ['/mis-talleres', 'Mis talleres pendientes'], ['/', 'Ver talleres'], ['/borradores', 'Mis encuestas'], ['/sincronizacion', 'Enviar encuestas pendientes']]],
];

const titles: Record<string, string> = {
  admin: 'Administración',
  coordinador: 'Coordinación de proyectos',
  monitor: 'Operación en terreno',
  participante: 'Mis actividades',
};

const descriptions: Record<string, string> = {
  admin: 'Configura proyectos y talleres, revisa el quórum y supervisa los registros.',
  coordinador: 'Planifica actividades, publica la bitácora y revisa la asistencia del equipo.',
  monitor: 'Acredita asistentes en terreno y envía las marcaciones cuando vuelve la conexión.',
  participante: 'Consulta talleres, muestra tu QR y completa encuestas desde tu cuenta.',
};

export default function RoleDashboard() {
  const roles = useUserRoles();
  const view = views.find(([role]) => roles.includes(role));
  return <div className="contenedor" style={{ paddingBlock: 30 }}><p className="eyebrow">Mi espacio en SyncPass</p><h1>{view ? titles[view[0]] : 'Perfil pendiente'}</h1>{view ? <><p className="texto-apoyo">{descriptions[view[0]]}</p><div className="grid-beneficios">{view[1].map(([url, label]) => <Link href={url} key={url} className="panel"><h2>{label}</h2><span aria-hidden="true">Abrir →</span></Link>)}</div></> : <p>Solicita al administrador que asigne tu grupo en Cognito.</p>}</div>;
}
