# Plan de acción y verificación de comunicación de SyncPass

Fecha de preparación: 10 de noviembre de 2026. Este documento es una lista de cierre: una casilla se marca solo con evidencia de ejecución, no por la existencia del código.

Estado al preparar esta versión: build web y 34 pruebas Vitest aprobadas; APK debug compilado y copiado al enlace de descarga de la web con SHA-256 coincidente. API Gateway `/actuator/health` respondió **503** y ADB no detectó un teléfono. Por ello las filas de integración remota y dispositivo siguen pendientes. La publicación de este commit en producción también debe verificarse aparte.

## Regla de cierre

Para declarar SyncPass terminado deben coincidir el commit de web, JAR, APK y documentación; pasar las pruebas locales; y repetirse el recorrido cruzado web ↔ API ↔ móvil ↔ base de datos con el laboratorio activo. Cada fallo debe quedar con solicitud, respuesta, causa y corrección. La web puede abrir desde Vercel aunque EC2 esté apagada, por lo que cargar la portada no demuestra conectividad.

## Secuencia de ejecución

| Orden | Acción | Evidencia requerida | Estado inicial |
| --- | --- | --- | --- |
| 1 | Congelar contrato de DTO y rutas, y documentar roles | JSON de ejemplo, métodos, códigos HTTP y permisos | Pendiente |
| 2 | Ejecutar pruebas y build web, API y Android | Salida de Vitest, Maven y Gradle con commit | Línea base local aprobada el 10/11; repetir al cierre |
| 3 | Desplegar JAR en EC2 y comprobar Caddy/Gateway | `systemctl active`, health 200 en EC2 y Gateway | Repetir con versión final |
| 4 | Probar registro, login, refresh y cuatro roles | 201/204/200, JWT válido, 401/403 en rutas indebidas | Repetir con versión final |
| 5 | Probar CRUD de proyectos y talleres | POST, GET, PUT, GET, DELETE, GET; versión y errores | Pendiente extremo a extremo |
| 6 | Crear taller web y actualizar catálogo Android | Mismo ID, título y fecha en ambos | Pendiente |
| 7 | Capturar QR y encuesta Android sin red | Pendientes en Room, estado visible y sin pérdida | Pendiente |
| 8 | Restablecer red y sincronizar | `acceptedIds`, cola vacía, quórum/encuesta visibles en web, sin duplicados | Pendiente |
| 9 | Repetir caso equivalente con IndexedDB web | Borrador local, cola, envío y dato remoto | Pendiente |
| 10 | Probar GPS dentro/fuera del radio y permisos | Aceptación/rechazo explicable, sin ubicación real guardada en evidencias | Pendiente |
| 11 | Revisar diseño y accesibilidad en teléfono y PC | Capturas de estados, tareas completadas por usuario de prueba | En progreso |
| 12 | Publicar APK, web y documentación del mismo commit | Descarga e instalación de APK, enlaces y guía actualizados | Pendiente |

## Matriz de comunicación

| Origen → destino | Ruta o mecanismo | Prueba positiva | Prueba de fallo |
| --- | --- | --- | --- |
| Web → Gateway → EC2 | `GET /api/eventos` | Lista JSON y taller en portada | EC2 apagada: aviso y caché si existe |
| Android → Gateway → EC2 | Retrofit `GET api/eventos` | Taller remoto aparece tras Actualizar | Sin red: catálogo Room y estado local |
| Web/Android → Cognito vía EC2 | `/api/auth/register`, `confirm`, `login` | Registro confirmado y rol inicial `participante` | Contraseña incorrecta, código inválido, token vencido |
| Web → PostgreSQL vía API | `/api/proyectos`, `/api/eventos` | CRUD completo con persistencia tras recargar | 400 validación, 401 sin token, 403 sin rol, 404 ID ausente, 409 versión |
| Android → Room | DAO de talleres y pendientes | Cierre/reapertura conserva catálogo y cola | Dato de otro usuario no aparece |
| Android/web → PostgreSQL vía API | `POST /api/sync/batch` | Solo IDs aceptados salen de la cola; web muestra resultado | Reenvío no duplica; error parcial conserva pendientes |
| QR/GPS → API | Asistencia en lote con ubicación opcional | QR válido y posición dentro del radio | QR inválido, ubicación fuera de radio o antigua |

El `GET /api/eventos` es público; los mantenedores y el quórum requieren token y grupo. La interfaz puede ocultar botones, pero el rechazo decisivo se comprueba en Spring Security. API Gateway enruta; las reglas finas de roles están en la API.

## Prueba cruzada mínima para exposición

1. En Postman, mostrar health, catálogo y un `401` sin token.
2. Iniciar sesión como coordinador en web. Crear un proyecto y un taller asociado; anotar sus IDs.
3. Abrir Android con otra cuenta autorizada, pulsar **Actualizar talleres** y buscar ese mismo ID/título. Cortar la red y comprobar que el catálogo permanece.
4. Con monitor, escanear QR de un participante ficticio y guardar asistencia sin red. Restaurar conexión y sincronizar.
5. Recargar quórum en web y verificar incremento una sola vez. Reenviar el mismo lote y comprobar idempotencia.
6. Editar y eliminar los registros de prueba. Verificar la respuesta de la API y la desaparición tras actualizar ambas interfaces.

## Diagnóstico de fallos

| Síntoma | Comprobar en orden |
| --- | --- |
| Web abre, API 503 | Laboratorio Ready → EC2 running → `syncpass-api` active → health local → Caddy → integración Gateway/IP. |
| Web ve taller, móvil no | Mismo `API_BASE_URL` → respuesta Retrofit → botón Actualizar → caché Room → APK recompilado e instalado. |
| Login funciona, operación 403 | Grupo Cognito en token actual → sesión renovada tras cambio de rol → `@PreAuthorize` de la ruta. |
| Pendiente no aparece en web | Usuario dueño de cola → respuesta `acceptedIds` → estado HTTP → tabla remota → recarga del panel. |
| Registros duplicados | ID enviado en lote → unicidad servidor → reintento después de respuesta perdida. |

## Registro de evidencia

Para cada recorrido guardar fecha, commit, versión APK/JAR, rol, URL sin token, método HTTP, estado, cuerpo JSON ficticio, captura de web/móvil y resultado esperado/real. Nunca adjuntar contraseñas, códigos de correo, JWT, claves, datos personales ni coordenadas reales al repositorio.

## Trabajo de interfaz que acompaña al plan

La portada web y Android comparten ahora mensaje principal, paleta territorial y acción **Actualizar talleres**. Android distingue carga, API conectada y catálogo local; separa el QR de la bienvenida y muestra un estado vacío. La siguiente revisión visual debe cubrir login, registro, escáner, encuesta, mantenedores y paneles por rol con tamaños táctiles, contraste y textos entendibles para coordinadores, monitores y participantes.
