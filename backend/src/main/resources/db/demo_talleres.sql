-- Datos exclusivamente ficticios para la demostración académica.
-- Ejecutar una sola vez en el esquema syncpass cuando el catálogo esté vacío.
INSERT INTO syncpass.talleres
  (title, descripcion, area, unidad_vecinal, comuna, direccion, fecha_hora, docente_lider, cupos_maximos, version)
VALUES
  ('Alfabetizacion digital comunitaria', 'Taller ficticio para demostrar acreditacion QR y encuesta.', 'Tecnologia', 'Unidad Vecinal de Prueba 01', 'La Florida', 'Centro comunitario de prueba', '2026-11-05T10:00:00', 'Docente de ejemplo', 30, 0),
  ('Emprendimiento local', 'Actividad ficticia para ensayar cupos, quorum y reportes.', 'Emprendimiento', 'Unidad Vecinal de Prueba 02', 'La Florida', 'Sala de actividades de prueba', '2026-11-12T15:00:00', 'Docente de ejemplo', 25, 0),
  ('Bienestar y salud digital', 'Sesión ficticia de apoyo comunitario para la demostracion.', 'Salud', 'Unidad Vecinal de Prueba 03', 'Puente Alto', 'Espacio comunitario de prueba', '2026-11-19T11:00:00', 'Docente de ejemplo', 20, 0);
