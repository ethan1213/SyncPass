-- ========================================================
-- Base de Datos Transaccional (OLTP): Sistema SyncPass
-- Asignatura: DSY1105 (Móviles) / DSY1104 (Full Stack)
-- Institución: Duoc UC - CITT Plaza Vespucio
-- Socio Formador: TI CONSULTING DIEGO GARCÉS MIRANDA E.I.R.L. (Ticdgm)
-- Caso: Acreditación QR, Encuestas y Modo Offline-First
-- ========================================================

-- --------------------------------------------------------
-- 1. Tabla de Roles de Usuario
-- --------------------------------------------------------
CREATE TABLE roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE, -- 'Coordinador Docente', 'Estudiante Monitor', 'Administrador'
    descripcion VARCHAR(255) NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------
-- 2. Tabla de Usuarios (Coordinadores y Monitores)
-- --------------------------------------------------------
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    id_rol INT NOT NULL DEFAULT 2,
    nombre_completo VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    clave_hash VARCHAR(255) NOT NULL,
    telefono VARCHAR(20) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES roles(id_rol) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- --------------------------------------------------------
-- 3. Tabla de Unidades Vecinales y Sedes Comunitarias
-- --------------------------------------------------------
CREATE TABLE unidades_vecinales (
    id_unidad INT AUTO_INCREMENT PRIMARY KEY,
    nombre_unidad VARCHAR(120) NOT NULL, -- Ej: 'Unidad Vecinal N° 14 Los Aromos'
    comuna VARCHAR(80) NOT NULL,         -- Ej: 'La Florida', 'Puente Alto'
    direccion VARCHAR(200) NOT NULL,
    latitud_referencial DECIMAL(10, 7) NOT NULL,
    longitud_referencial DECIMAL(10, 7) NOT NULL,
    contacto_comunitario VARCHAR(120) NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------
-- 4. Tabla de Proyectos Comunitarios de Vinculación
-- --------------------------------------------------------
CREATE TABLE proyectos_comunitarios (
    id_proyecto INT AUTO_INCREMENT PRIMARY KEY,
    id_coordinador INT NOT NULL,
    id_unidad INT NOT NULL,
    titulo VARCHAR(160) NOT NULL,
    descripcion TEXT NOT NULL,
    area_tematica ENUM('Tecnología', 'Salud', 'Emprendimiento', 'Sustentabilidad') NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_termino DATE NOT NULL,
    estado ENUM('planificacion', 'en_ejecucion', 'finalizado', 'suspendido') NOT NULL DEFAULT 'en_ejecucion',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_proyecto_coordinador FOREIGN KEY (id_coordinador) REFERENCES usuarios(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_proyecto_unidad FOREIGN KEY (id_unidad) REFERENCES unidades_vecinales(id_unidad) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- --------------------------------------------------------
-- 5. Tabla de Eventos y Talleres en Terreno
-- --------------------------------------------------------
CREATE TABLE eventos_talleres (
    id_evento INT AUTO_INCREMENT PRIMARY KEY,
    id_proyecto INT NOT NULL,
    nombre_actividad VARCHAR(160) NOT NULL,
    descripcion_taller TEXT NULL,
    fecha_hora DATETIME NOT NULL,
    cupos_maximos INT NOT NULL CHECK (cupos_maximos > 0),
    recinto VARCHAR(150) NOT NULL,
    latitud DECIMAL(10, 7) NOT NULL,
    longitud DECIMAL(10, 7) NOT NULL,
    estado ENUM('programado', 'en_curso', 'finalizado', 'cancelado') NOT NULL DEFAULT 'programado',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evento_proyecto FOREIGN KEY (id_proyecto) REFERENCES proyectos_comunitarios(id_proyecto) ON DELETE CASCADE ON UPDATE CASCADE
);

-- --------------------------------------------------------
-- 6. Tabla de Participantes y Asistentes (Datos Ficticios Anonimizados)
-- --------------------------------------------------------
CREATE TABLE participantes (
    id_participante INT AUTO_INCREMENT PRIMARY KEY,
    identificador_anonimo VARCHAR(64) NOT NULL UNIQUE, -- Código alfanumérico anónimo (cumplimiento restricción 5.1)
    nombre_ficticio VARCHAR(120) NOT NULL,
    tipo_participante ENUM('Estudiante', 'Vecino', 'Docente') NOT NULL,
    codigo_qr VARCHAR(255) NOT NULL UNIQUE,            -- Payload firmado contenido en el QR
    estado_inscripcion ENUM('inscrito', 'en_espera', 'cancelado') NOT NULL DEFAULT 'inscrito',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------
-- 7. Tabla de Registro de Asistencia (Soporte Offline-First)
-- --------------------------------------------------------
CREATE TABLE asistencia_marcaciones (
    id_asistencia INT AUTO_INCREMENT PRIMARY KEY,
    id_evento INT NOT NULL,
    id_participante INT NOT NULL,
    id_monitor INT NOT NULL,
    timestamp_marcacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    latitud_marcacion DECIMAL(10, 7) NULL,
    longitud_marcacion DECIMAL(10, 7) NULL,
    estado_validacion ENUM('valido', 'fuera_de_rango', 'duplicado') NOT NULL DEFAULT 'valido',
    is_synced BOOLEAN NOT NULL DEFAULT FALSE, -- Bandera clave para sincronización offline
    synced_at TIMESTAMP NULL,
    CONSTRAINT fk_asistencia_evento FOREIGN KEY (id_evento) REFERENCES eventos_talleres(id_evento) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_asistencia_participante FOREIGN KEY (id_participante) REFERENCES participantes(id_participante) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_asistencia_monitor FOREIGN KEY (id_monitor) REFERENCES usuarios(id_usuario) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_evento_participante UNIQUE (id_evento, id_participante)
);

-- --------------------------------------------------------
-- 8. Tabla de Encuestas de Valoración y Satisfacción Inmediata
-- --------------------------------------------------------
CREATE TABLE encuestas_satisfaccion (
    id_encuesta INT AUTO_INCREMENT PRIMARY KEY,
    id_evento INT NOT NULL,
    id_participante INT NOT NULL,
    satisfaccion_estrellas TINYINT NOT NULL CHECK (satisfaccion_estrellas BETWEEN 1 AND 5),
    utilidad_taller ENUM('muy_util', 'util', 'regular', 'poco_util') NOT NULL,
    comentario TEXT NULL,
    fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_synced BOOLEAN NOT NULL DEFAULT FALSE, -- Bandera para cola de envío offline
    synced_at TIMESTAMP NULL,
    CONSTRAINT fk_encuesta_evento FOREIGN KEY (id_evento) REFERENCES eventos_talleres(id_evento) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_encuesta_participante FOREIGN KEY (id_participante) REFERENCES participantes(id_participante) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_encuesta_evento_participante UNIQUE (id_evento, id_participante)
);

-- --------------------------------------------------------
-- Índices para Optimización en Consultas y Sincronización
-- --------------------------------------------------------
CREATE INDEX idx_asistencia_evento ON asistencia_marcaciones(id_evento);
CREATE INDEX idx_asistencia_is_synced ON asistencia_marcaciones(is_synced);
CREATE INDEX idx_encuesta_is_synced ON encuestas_satisfaccion(is_synced);
CREATE INDEX idx_participante_qr ON participantes(codigo_qr);
