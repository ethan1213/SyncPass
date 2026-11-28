CREATE TABLE taller_archivos (
 taller_id BIGINT PRIMARY KEY REFERENCES talleres(id) ON DELETE CASCADE,
 formato VARCHAR(80) NOT NULL,
 contenido BYTEA NOT NULL
);
