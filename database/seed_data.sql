-- ========================================================
-- Datos de Prueba Simulados: Sistema SyncPass
-- Cumplimiento Restricción 5.1: Datos 100% ficticios y anonimizados
-- ========================================================

-- 1. Roles
INSERT INTO roles (id_rol, nombre_rol, descripcion) VALUES
(1, 'Docente Coordinador', 'Líder académico que crea proyectos, talleres y audita métricas de acreditación'),
(2, 'Estudiante Monitor', 'Voluntario en terreno que acredita por QR y levanta encuestas con o sin internet'),
(3, 'Administrador CITT', 'Gestor general de la plataforma y sincronización institucional');

-- 2. Usuarios (Monitores y Coordinadores)
INSERT INTO usuarios (id_usuario, id_rol, nombre_completo, correo, clave_hash, telefono, activo) VALUES
(1, 1, 'Prof. Nelson Melo (Ficticio)', 'nelson.melo@duocuc.cl', '$2b$10$SyncPassDemoHashCoordinador2026.', '+56911223344', TRUE),
(2, 2, 'Estudiante Monitor 01', 'monitor01@alumnos.duoc.cl', '$2b$10$SyncPassDemoHashMonitorVoluntario.', '+56955667788', TRUE),
(3, 2, 'Estudiante Monitor 02', 'monitor02@alumnos.duoc.cl', '$2b$10$SyncPassDemoHashMonitorVoluntario2', '+56999887766', TRUE);

-- 3. Unidades Vecinales
INSERT INTO unidades_vecinales (id_unidad, nombre_unidad, comuna, direccion, latitud_referencial, longitud_referencial, contacto_comunitario) VALUES
(1, 'Unidad Vecinal N° 12 Los Castaños', 'La Florida', 'Av. Santa Raquel 1450', -33.5321450, -70.6124580, 'Presidenta Sra. Elena (Simulada)'),
(2, 'Sede Comunitaria Villa Cordillera', 'Puente Alto', 'Calle Central 780', -33.5982140, -70.5784120, 'Directorio Vecinal 2026'),
(3, 'Centro Cultural La Granja Sur', 'La Granja', 'Pasaje Las Violetas 320', -33.5412890, -70.6289740, 'Coordinación Territorial');

-- 4. Proyectos Comunitarios
INSERT INTO proyectos_comunitarios (id_proyecto, id_coordinador, id_unidad, titulo, descripcion, area_tematica, fecha_inicio, fecha_termino, estado) VALUES
(1, 1, 1, 'Alfabetización Digital para Personas Mayores', 'Capacitación práctica en el uso de smartphones, trámites en línea y prevención de fraudes digitales.', 'Tecnología', '2026-08-10', '2026-11-30', 'en_ejecucion'),
(2, 1, 2, 'Taller de Emprendimiento y Comercio Digital', 'Asesoría a pequeños comerciantes en catálogos web, pagos digitales y presencia comunitaria.', 'Emprendimiento', '2026-08-15', '2026-12-05', 'en_ejecucion'),
(3, 1, 3, 'Reparación Sustentable y Reutilización de Hardware', 'Taller de mantención básica de computadores para prolongar la vida útil de equipos comunitarios.', 'Sustentabilidad', '2026-09-01', '2026-11-15', 'en_ejecucion');

-- 5. Eventos / Talleres en Terreno
INSERT INTO eventos_talleres (id_evento, id_proyecto, nombre_actividad, descripcion_taller, fecha_hora, cupos_maximos, recinto, latitud, longitud, estado) VALUES
(1, 1, 'Sesión 1: Manejo de Apps y Seguridad Digital', 'Uso seguro de WhatsApp, correo electrónico y detección de mensajes sospechosos.', '2026-10-18 10:00:00', 35, 'Salón Multiuso Los Castaños', -33.5321450, -70.6124580, 'programado'),
(2, 1, 'Sesión 2: Trámites en Línea y Registro Social', 'Navegación en portales públicos con ClaveÚnica y descarga de certificados.', '2026-10-25 10:00:00', 35, 'Salón Multiuso Los Castaños', -33.5321450, -70.6124580, 'programado'),
(3, 2, 'Taller Práctico: Catálogo de Productos en Línea', 'Creación guiada de una vitrina de productos y cálculo de precios.', '2026-10-20 15:30:00', 25, 'Sede Villa Cordillera', -33.5982140, -70.5784120, 'programado');

-- 6. Participantes Simulados (Anonimizados)
INSERT INTO participantes (id_participante, identificador_anonimo, nombre_ficticio, tipo_participante, codigo_qr, estado_inscripcion) VALUES
(1, 'PART-LF-2026-001', 'Vecino Participante A', 'Vecino', 'SYNCPASS_QR_A8F932BC71E2001', 'inscrito'),
(2, 'PART-LF-2026-002', 'Vecina Participante B', 'Vecino', 'SYNCPASS_QR_B9C843DE82F3002', 'inscrito'),
(3, 'PART-PA-2026-003', 'Estudiante Voluntario C', 'Estudiante', 'SYNCPASS_QR_C1D234EF93A4003', 'inscrito'),
(4, 'PART-LG-2026-004', 'Vecino Participante D', 'Vecino', 'SYNCPASS_QR_D2E345FA04B5004', 'inscrito');

-- 7. Registro de Asistencias (Marcaciones QR en terreno con soporte Offline)
INSERT INTO asistencia_marcaciones (id_asistencia, id_evento, id_participante, id_monitor, timestamp_marcacion, latitud_marcacion, longitud_marcacion, estado_validacion, is_synced, synced_at) VALUES
(1, 1, 1, 2, '2026-10-15 10:02:15', -33.5321400, -70.6124600, 'valido', TRUE, '2026-10-15 12:30:00'),
(2, 1, 2, 2, '2026-10-15 10:03:02', -33.5321420, -70.6124550, 'valido', TRUE, '2026-10-15 12:30:00'),
(3, 1, 3, 2, '2026-10-15 10:04:18', -33.5321440, -70.6124500, 'valido', FALSE, NULL); -- Pendiente de sincronizar (offline)

-- 8. Encuestas de Satisfacción Inmediata en Terreno
INSERT INTO encuestas_satisfaccion (id_encuesta, id_evento, id_participante, satisfaccion_estrellas, utilidad_taller, comentario, fecha_envio, is_synced, synced_at) VALUES
(1, 1, 1, 5, 'muy_util', 'Excelente la paciencia de los estudiantes monitores. Aprendí a usar mi teléfono sin miedo.', '2026-10-15 11:55:10', TRUE, '2026-10-15 12:30:00'),
(2, 1, 2, 5, 'muy_util', 'La acreditación con el código QR fue inmediata, sin filas como antes.', '2026-10-15 11:57:30', TRUE, '2026-10-15 12:30:00');
