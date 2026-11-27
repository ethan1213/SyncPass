# Validación de SyncPass para Prueba 3

Responsable: Ethan Astorga. La pauta Android y la pauta conjunta de Android/React distinguen la calidad del código de la demostración en nube y la defensa individual.

Revisión del 27-11-2026. Las tres copias del PDF conjunto tienen el mismo SHA-256. La pauta conjunta exige 30 % de líneas verificables por cliente y diagramas C1, C2 y C3. El proyecto incorpora C4 como apoyo para explicar el código. La firma de release está en una guía del profesor adicional a esos PDF.

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
| Firma Android | AAB y APK 1.0.1 firmados con el certificado de subida y huellas coincidentes. El Samsung conserva el APK debug 1.0.0 con otra firma. Repetir la prueba de instalación en un dispositivo limpio o preservar primero la cola local. [Procedimiento](../mobile-kotlin/README.md#firma-de-release). |

## Cómo repetir las pruebas

La revisión del 27-11-2026 ejecutó 96 pruebas web y regeneró el reporte con 73,90 % de líneas. Android registró 102 pruebas correctas y 79,87 % de líneas en el reporte JaCoCo filtrado a la lógica de negocio. Este porcentaje excluye pantallas Compose y clases generadas. Los dos reportes superan el mínimo del 30 % dentro de su alcance. La verificación previa de Chromium y del backend corresponde a ejecuciones anteriores.

- [Informe Vitest](../web-react/coverage/index.html)
- [Informe JaCoCo de lógica Android](../mobile-kotlin/app/build/reports/jacoco/jacocoFilteredTestReport/html/index.html)

## Revisión cruzada con el portal

| Material del profesor | Evidencia de SyncPass | Por cerrar |
| --- | --- | --- |
| [Guía de login](https://app.claudioviajando.com/posts/guia-login) | Compose observa `LoginUiState` y consume un evento de éxito. Retrofit llega a Gateway, Spring Boot y Cognito. | La sesión vive en memoria. La persistencia de sesión y Hilt del ejemplo pertenecen al diseño de referencia. |
| [Guía de firma AAB](https://app.claudioviajando.com/posts/guia-firmando-aab) | El AAB y el APK de release se verificaron con `jarsigner` y `apksigner`. La carpeta del almacén no se rastrea en Git. | Instalación de release en un teléfono con el mismo certificado o limpio; publicación en Play solo si se solicita. |
| [Testing con Vitest y React](https://app.claudioviajando.com/posts/testing-vitest-react) | Vitest y Testing Library prueban sesión, páginas, componentes y los dos CRUD. | Los tests con API simulada no sustituyen una captura autenticada de los mantenedores desplegados. |
| [Android, Kotlin y Room](https://app.claudioviajando.com/posts/android-kotlin-y-room) | Room conserva catálogo y registros pendientes; el repositorio coordina la API y la cola local. | Mostrar en la misma toma un pendiente nuevo y su confirmación después de recuperar señal. |

- En `backend`, ejecutar `mvnw.cmd test`.
- En `web-react`, ejecutar `npm ci`, `npm run coverage` y `npm run build`. Leer el porcentaje de líneas en `coverage/index.html`. Para el navegador real, ejecutar `npx playwright install chromium` y `npm run test:browser`.
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
