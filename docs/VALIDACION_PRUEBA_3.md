# Validación de SyncPass para Prueba 3

Responsable: Ethan Astorga. La pauta Android y la pauta conjunta de Android/React distinguen la calidad del código de la demostración en nube y la defensa individual.

| Criterio | Ubicación y comprobación |
| --- | --- |
| EC2, Cognito, Supabase y API REST | [Arquitectura C1 a C4](ARQUITECTURA.md). Comprobar salud pública, catálogo, 401 sin token y operaciones con una sesión real. La infraestructura existente usa Gateway como entrada de ambos clientes. |
| Perfiles | El caso SyncPass usa participante, coordinador, monitor y administrador. En la defensa, explicar la relación funcional con estudiante, docente y voluntario mediante participación, gestión y apoyo de taller, respectivamente. Verificar con el docente si pide también los nombres literales de la pauta. |
| Android nativo y hardware | Kotlin y pantallas `@Composable`, cámara para QR y ubicación para geocerca. Probar ambos permisos en un teléfono. |
| MVVM y dos fuentes | `BitacoraScreen` observa `BitacoraViewModel`. El ViewModel usa `SyncPassRepository`. Retrofit obtiene el contenido remoto. Room guarda catálogo y pendientes que pueden crearse sin señal. |
| Sincronización | Crear un pendiente sin red, reconectar y pulsar sincronizar. Verificar en la API que llegó una sola vez. Repetir con un lote mixto para observar que se conservan los registros rechazados y se envían los válidos. |
| React | Vite y TypeScript, `SessionContext`, `useSession`, Wouter, componentes atómicos, Tailwind y cliente HTTP con tipos de entrada y salida en autenticación. `RequireAuth` protege las páginas privadas. Probar ambos mantenedores tras autenticación. |
| Mockups y alcance | [Catálogo de vistas y estado por plataforma](MOCKUPS.md) y [alcance funcional](ALCANCE.md). Comparar las vistas que están en el catálogo con las rutas y pantallas ejecutables. Los pendientes marcados allí no equivalen a pantallas verificadas. |
| IA | [Contexto y uso de IA en ingeniería](IA_CONTEXTO.md). La pauta evalúa la coherencia de los documentos y la defensa. La transcripción Gemini y los borradores revisados son una función adicional. |
| Pruebas | Vitest con React Testing Library, JaCoCo con JUnit/Robolectric y JUnit con MockMvc. La pauta pide al menos 30 % en web y Android. |

## Cómo repetir las pruebas

La ejecución de esta revisión registró 32 pruebas de backend, 95 de React y 100 de Android sin fallos. Vitest midió 73,99 % de líneas web y JaCoCo 79,66 % de líneas en su reporte filtrado de Android. Ambos superan el 30 % exigido.

- En `backend`, ejecutar `mvnw.cmd test`.
- En `web-react`, ejecutar `npm ci`, `npm run coverage` y `npm run build`. Leer el porcentaje de líneas en `coverage/index.html`.
- En `mobile-kotlin`, ejecutar `gradlew.bat :app:testDebugUnitTest :app:jacocoFilteredTestReport :app:assembleDebug`. Los informes quedan en `app/build/reports`.

## Casos de regresión

- Abrir la bitácora sin publicación muestra el estado vacío. Publicar desde el panel web, volver al taller en Android y pulsar actualizar.
- Si el lote Android contiene 404 o 409, se separa para reintentar los registros válidos. Room solo borra los UUID confirmados. Los UUID rechazados permanecen en el dispositivo.
- Editar el mismo taller desde dos sesiones. La edición con versión antigua recibe `409 STALE_VERSION`, conserva la versión guardada por la otra sesión y permite consultar el dato vigente.
- Instalar una actualización sobre un Samsung con Room 5. El APK de la web debe apuntar al Gateway y mantener la misma versión de esquema o una migración superior.

## Evidencia física y nube

Registrar por separado el arranque del APK, el inicio de sesión real, el dato publicado que cruza web y Android, la lectura de QR y GPS, y la ejecución con clave Gemini. Un resultado de JUnit, una respuesta pública de salud y una instalación correcta no prueban por sí solos esos recorridos.

## Recorrido entre dos cuentas

1. En web, una cuenta de administrador o coordinador crea un taller desde el mantenedor. La otra cuenta lo consulta en el catálogo Android. El catálogo se actualiza al volver a la pantalla y cada 20 segundos mientras está visible. Con red disponible, compara el título, fecha y cupos con la ficha web.
2. En el taller, abre la bitácora web. Si está vacía, «Cargar apuntes de prueba» prepara texto ficticio en el formulario. «Guardar y resumir» publica el texto, que se guarda por ID de taller. Esta publicación usa el permiso del usuario ya autenticado.
3. En el mismo taller en Android, pulsa «Actualizar bitácora». Abre «Ver apuntes publicados y fuentes» y compara las frases numeradas con la cita devuelta por la pregunta. Repite tras editar la bitácora en web.
4. Para probar sin tocar Supabase, `deploy/local-seed.sql` deja tres ejemplos. En Emprendimiento local pregunta «¿Cuál fue el punto de equilibrio?». En Bienestar digital pregunta «¿Cuánto duró la pausa?». En ambos, una pregunta de astronomía debe indicar que los apuntes no contienen la información.
