-- Datos ficticios solo para PostgreSQL local. No modifica Supabase.
INSERT INTO talleres
  (title, descripcion, area, unidad_vecinal, comuna, direccion, fecha_hora, docente_lider, cupos_maximos, version)
SELECT * FROM (VALUES
  ('Alfabetización digital comunitaria', 'Taller de prueba para QR, asistencia y encuesta.', 'Tecnología', 'Unidad Vecinal Demo 01', 'La Florida', 'Centro comunitario de prueba', '2026-11-19T10:00:00', 'Docente de ejemplo', 30, 0),
  ('Emprendimiento local', 'Actividad ficticia para probar cupos y quórum.', 'Emprendimiento', 'Unidad Vecinal Demo 02', 'La Florida', 'Sala de actividades de prueba', '2026-11-20T15:00:00', 'Docente de ejemplo', 25, 0),
  ('Bienestar digital', 'Sesión ficticia para demostrar bitácora y resumen.', 'Salud', 'Unidad Vecinal Demo 03', 'Puente Alto', 'Espacio comunitario de prueba', '2026-11-21T11:00:00', 'Docente de ejemplo', 20, 0)
) AS demo(title, descripcion, area, unidad_vecinal, comuna, direccion, fecha_hora, docente_lider, cupos_maximos, version)
WHERE NOT EXISTS (SELECT 1 FROM talleres);

-- Corrige datos demo importados por una terminal Windows con codificación antigua.
UPDATE talleres SET title = 'Alfabetización digital comunitaria', area = 'Tecnología'
WHERE title LIKE 'Alfabetizaci%' AND title <> 'Alfabetización digital comunitaria';

-- Bitácora ficticia para recorrer la innovación sin devolver 404 en el taller demo.
INSERT INTO taller_bitacoras (taller_id, contenido, resumen, actualizado_en)
SELECT id,
  'En la sesión de alfabetización digital se practicó el uso de contraseñas únicas. Los participantes aprendieron a reconocer mensajes sospechosos y a verificar enlaces antes de abrirlos. Al finalizar, cada persona preparó un plan simple para proteger sus cuentas.',
  'En la sesión de alfabetización digital se practicó el uso de contraseñas únicas. Los participantes aprendieron a reconocer mensajes sospechosos y a verificar enlaces antes de abrirlos.',
  NOW()
FROM talleres
WHERE title = 'Alfabetización digital comunitaria'
  AND NOT EXISTS (SELECT 1 FROM taller_bitacoras WHERE taller_id = talleres.id);
