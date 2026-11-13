-- Datos ficticios solo para PostgreSQL local. No modifica Supabase.
INSERT INTO talleres
  (title, descripcion, area, unidad_vecinal, comuna, direccion, fecha_hora, docente_lider, cupos_maximos, version)
SELECT * FROM (VALUES
  ('Alfabetización digital comunitaria', 'Taller de prueba para QR, asistencia y encuesta.', 'Tecnología', 'Unidad Vecinal Demo 01', 'La Florida', 'Centro comunitario de prueba', '2026-11-19T10:00:00', 'Docente de ejemplo', 30, 0),
  ('Emprendimiento local', 'Actividad ficticia para probar cupos y quórum.', 'Emprendimiento', 'Unidad Vecinal Demo 02', 'La Florida', 'Sala de actividades de prueba', '2026-11-20T15:00:00', 'Docente de ejemplo', 25, 0),
  ('Bienestar digital', 'Sesión ficticia para demostrar bitácora y resumen.', 'Salud', 'Unidad Vecinal Demo 03', 'Puente Alto', 'Espacio comunitario de prueba', '2026-11-21T11:00:00', 'Docente de ejemplo', 20, 0)
) AS demo(title, descripcion, area, unidad_vecinal, comuna, direccion, fecha_hora, docente_lider, cupos_maximos, version)
WHERE NOT EXISTS (SELECT 1 FROM talleres);
