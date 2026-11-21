ALTER TABLE talleres ADD COLUMN roles_permitidos VARCHAR(100);
ALTER TABLE talleres ADD COLUMN zona_restringida VARCHAR(120);

CREATE TABLE usuario_zona (
 sub VARCHAR(64) PRIMARY KEY,
 zona VARCHAR(120) NOT NULL
);

CREATE TABLE inscripcion (
 id BIGSERIAL PRIMARY KEY,
 taller_id BIGINT NOT NULL REFERENCES talleres(id) ON DELETE CASCADE,
 participante_sub VARCHAR(64),
 device_id VARCHAR(64),
 estado VARCHAR(20) NOT NULL,
 creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
 asistio_en TIMESTAMP WITH TIME ZONE,
 CONSTRAINT inscripcion_persona CHECK (participante_sub IS NOT NULL OR device_id IS NOT NULL)
);

-- Sin indice unico parcial (H2 en modo PostgreSQL no lo soporta en los tests):
-- la idempotencia por (taller, persona) la garantiza InscripcionService
-- buscando antes de insertar, igual que ya hace IngresoService con "registros".
CREATE INDEX inscripcion_taller_sub_idx ON inscripcion(taller_id, participante_sub);
CREATE INDEX inscripcion_taller_device_idx ON inscripcion(taller_id, device_id);
