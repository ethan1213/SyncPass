-- ========================================================
-- Base de Datos Multidimensional: Modelo en Estrella (Star Schema)
-- Proyecto: SyncPass (DSY1105 / DSY1104 - Duoc UC)
-- Caso: Data Warehouse de Métricas de Asistencia e Impacto Comunitario
-- ========================================================

-- 1. Dimensión Tiempo
CREATE TABLE dim_tiempo (
    id_tiempo INT PRIMARY KEY, -- Formato AAAAMMDD
    fecha_completa DATE NOT NULL,
    dia TINYINT NOT NULL,
    mes TINYINT NOT NULL,
    nombre_mes VARCHAR(20) NOT NULL,
    semestre TINYINT NOT NULL, -- 1 o 2
    anio SMALLINT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL
);

-- 2. Dimensión Proyecto y Taller
CREATE TABLE dim_proyecto_taller (
    id_proyecto_taller INT PRIMARY KEY,
    nombre_proyecto VARCHAR(160) NOT NULL,
    area_tematica VARCHAR(80) NOT NULL, -- 'Tecnología', 'Salud', 'Emprendimiento', 'Sustentabilidad'
    nombre_actividad VARCHAR(160) NOT NULL,
    docente_coordinador VARCHAR(120) NOT NULL
);

-- 3. Dimensión Territorio y Unidad Vecinal
CREATE TABLE dim_territorio (
    id_territorio INT PRIMARY KEY,
    unidad_vecinal VARCHAR(120) NOT NULL,
    comuna VARCHAR(80) NOT NULL, -- 'La Florida', 'Puente Alto', 'La Granja'
    region VARCHAR(80) NOT NULL DEFAULT 'Región Metropolitana',
    tipo_recinto VARCHAR(80) NOT NULL -- 'Sede Vecinal', 'Colegio', 'Centro Cultural'
);

-- 4. Dimensión Perfil Participante
CREATE TABLE dim_participante (
    id_participante INT PRIMARY KEY,
    tipo_participante VARCHAR(50) NOT NULL, -- 'Vecino', 'Estudiante Voluntario', 'Docente'
    estado_inscripcion VARCHAR(50) NOT NULL,
    rango_edad_referencial VARCHAR(50) DEFAULT 'Adulto Mayor / Adulto'
);

-- 5. Dimensión Conectividad y Modalidad
CREATE TABLE dim_conectividad (
    id_conectividad INT PRIMARY KEY,
    modo_registro VARCHAR(50) NOT NULL,      -- 'Offline Local (Sin Cobertura)', 'Online Inmediato'
    protocolo_sync VARCHAR(50) NOT NULL      -- 'Batch Sync Posterior', 'Tiempo Real HTTP'
);

-- 6. Tabla Central de Hechos: Asistencia e Impacto Comunitario
CREATE TABLE fact_asistencia_impacto (
    id_hecho INT AUTO_INCREMENT PRIMARY KEY,
    id_tiempo INT NOT NULL,
    id_proyecto_taller INT NOT NULL,
    id_territorio INT NOT NULL,
    id_participante INT NOT NULL,
    id_conectividad INT NOT NULL,
    asistencias_efectivas INT NOT NULL DEFAULT 1,
    tiempo_acreditacion_segundos DECIMAL(4, 2) NOT NULL, -- < 5 segundos objetivo
    calificacion_satisfaccion TINYINT NULL,              -- 1 a 5 estrellas
    horas_participacion_taller DECIMAL(4, 2) NOT NULL DEFAULT 2.0,
    CONSTRAINT fk_fact_tiempo FOREIGN KEY (id_tiempo) REFERENCES dim_tiempo(id_tiempo),
    CONSTRAINT fk_fact_taller FOREIGN KEY (id_proyecto_taller) REFERENCES dim_proyecto_taller(id_proyecto_taller),
    CONSTRAINT fk_fact_territorio FOREIGN KEY (id_territorio) REFERENCES dim_territorio(id_territorio),
    CONSTRAINT fk_fact_participante FOREIGN KEY (id_participante) REFERENCES dim_participante(id_participante),
    CONSTRAINT fk_fact_conectividad FOREIGN KEY (id_conectividad) REFERENCES dim_conectividad(id_conectividad)
);

-- Datos analíticos iniciales
INSERT INTO dim_tiempo VALUES
(20261015, '2026-10-15', 15, 10, 'Octubre', 2, 2026, 'Jueves');

INSERT INTO dim_proyecto_taller VALUES
(1, 'Alfabetización Digital para Personas Mayores', 'Tecnología', 'Sesión 1: Manejo de Apps', 'Prof. Nelson Melo');

INSERT INTO dim_territorio VALUES
(1, 'Unidad Vecinal N° 12 Los Castaños', 'La Florida', 'Región Metropolitana', 'Sede Vecinal');

INSERT INTO dim_participante VALUES
(1, 'Vecino', 'inscrito', 'Adulto Mayor');

INSERT INTO dim_conectividad VALUES
(1, 'Offline Local (Sin Cobertura)', 'Batch Sync Posterior'),
(2, 'Online Inmediato', 'Tiempo Real HTTP');

INSERT INTO fact_asistencia_impacto VALUES
(1, 20261015, 1, 1, 1, 1, 1, 3.20, 5, 2.00);
