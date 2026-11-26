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

-- Añade solo material ficticio. Volver a ejecutar el archivo conserva las publicaciones existentes.
INSERT INTO taller_bitacoras (taller_id, contenido, resumen, actualizado_en)
SELECT t.id, demo.contenido, demo.resumen, NOW()
FROM talleres t
JOIN (VALUES
  ('Emprendimiento local',
   'El costo variable de cada cuaderno fue de 1200 pesos.' || chr(10) ||
   'Al vender cada cuaderno a 2000 pesos, el margen unitario fue de 800 pesos.' || chr(10) ||
   'Con un costo fijo de 16000 pesos, se necesitan vender 20 cuadernos para alcanzar el punto de equilibrio.' || chr(10) ||
   'La tarea acordada fue comparar tres proveedores antes de comprar materiales.',
   'Costos: 1200 pesos variables por cuaderno y 16000 pesos fijos.' || chr(10) ||
   'Precio: 2000 pesos y margen: 800 pesos por cuaderno.' || chr(10) ||
   'Punto de equilibrio: 20 cuadernos. Próximo paso: comparar tres proveedores.'),
  ('Bienestar digital',
   'El objetivo fue reducir las interrupciones durante el estudio.' || chr(10) ||
   'El grupo desactivó las notificaciones de redes sociales durante bloques de 25 minutos.' || chr(10) ||
   'Después de cada bloque se recomendó una pausa de 5 minutos lejos de la pantalla.' || chr(10) ||
   'Durante una semana se anotarán las interrupciones en una tabla para comparar resultados.',
   'Estudio: bloques de 25 minutos con las notificaciones sociales desactivadas.' || chr(10) ||
   'Pausas: 5 minutos lejos de la pantalla después de cada bloque.' || chr(10) ||
   'Seguimiento: anotar las interrupciones durante una semana.')
) AS demo(titulo, contenido, resumen) ON demo.titulo = t.title
ON CONFLICT (taller_id) DO NOTHING;
