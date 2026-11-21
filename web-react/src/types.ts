/** Tipos compartidos del catálogo de talleres y registros locales. */

export interface Asistente {
  id: number | string;
  nombre: string;
  hora: string;
  estado: string;
  synced: boolean;
}

/** Forma que entrega la API antes de normalizar el taller para la UI. */
export interface TallerRaw {
  id: number;
  title: string;
  descripcion: string;
  area: string;
  comuna: string;
  unidadVecinal: string;
  direccion: string;
  fechaHora: string;
  docenteLider: string;
  cuposMaximos: number;
  proyectoId?: number | string | null;
  latitud?: number | string | null;
  longitud?: number | string | null;
  radioMetros?: number | string | null;
  inscritos?: number;
  version?: number;
  /** CSV de roles Cognito que pueden ver/unirse; null/vacío = abierto a todos. */
  rolesPermitidos?: string | null;
  /** Unidad vecinal exigida para unirse; null/vacío = sin restricción de zona. */
  zonaRestringida?: string | null;
}

/** Taller ya normalizado por App para mostrarse en las páginas. */
export interface Taller extends TallerRaw {
  image: string;
  asistentes: Asistente[];
  inscritos: number;
  quorumPct: number;
}

export interface QueueRecord {
  id: string;
  owner?: string;
  tipo: 'asistencia' | 'encuesta';
  tallerId: number;
  timestamp: string;
  [key: string]: unknown;
}

export interface DraftRecord {
  id?: string;
  owner?: string;
  tallerId: number | string;
  rating: number;
  utilidad: string;
  comentario: string;
  updatedAt?: string;
}

/** RF12: preinscripción a un taller (quedó "pendiente" y aún no se acredita). */
export interface Inscripcion {
  id: number;
  tallerId: number;
  participanteSub?: string | null;
  deviceId?: string | null;
  estado: 'pendiente' | 'asistio';
  creadoEn: string;
  asistioEn?: string | null;
}

export interface Proyecto {
  id: number;
  title: string;
  descripcion: string;
  area: string;
  unidadVecinal: string;
  fechaInicio: string;
  fechaTermino: string;
  estado: string;
  version?: number;
}
