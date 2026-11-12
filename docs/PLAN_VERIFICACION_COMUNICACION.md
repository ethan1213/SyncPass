# Plan de cierre y verificación de SyncPass

Estado observado el 12 de noviembre de 2026. **Una compilación o prueba simulada no demuestra comunicación entre dispositivos.** Guardar una captura o respuesta por cada paso y registrar fecha, usuario de prueba y versión desplegada.

## Semáforo actual

| Área | Evidencia actual | Estado |
| --- | --- | --- |
| Web publicada | Vercel responde HTTP 200 | Comprobado solo disponibilidad de portada |
| API pública | `GET /actuator/health` en Gateway devuelve HTTP 503 | Bloqueada: revisar laboratorio, EC2, Caddy, JAR e integración |
| Web local | 35 pruebas Vitest pasan; 92,46% de líneas en los módulos incluidos, con bitácora | Comprobado local; no es cobertura global |
| Backend local | 18 pruebas Maven pasan, sin fallos ni errores | Comprobado local con H2; no demuestra EC2/Supabase |
| Android | `:app:assembleDebug` compila; `adb devices` no lista un teléfono | APK local listo; falta prueba física actual |
| Innovación de bitácora consultable | Implementación local en API, web y Android; resumen extractivo y preguntas con cita | Falta desplegar y probar web-Android con la misma bitácora; ver `INNOVACION_BITACORA.md` |
| QR/GPS/sincronización | Hay código y pruebas parciales | Falta recorrido real web-Android-API-base |

## Puerta 1: recuperar infraestructura

1. Iniciar AWS Academy y verificar EC2 `i-01d851d6163897cb1` en ejecución. Registrar IPv4 actual.
2. En EC2 ejecutar `systemctl status syncpass-api caddy syncpass-gateway-sync`; revisar `journalctl` de servicios con error. Verificar JAR y variables de entorno sin imprimir secretos.
3. Consultar `https://hluamat8vg.execute-api.us-east-1.amazonaws.com/actuator/health`: esperar HTTP 200 y `UP`. Si hay 503, comprobar desde EC2 el puerto local 8080, Caddy, TLS/nip.io y la URL de integración de Gateway.
4. Consultar `GET /api/eventos` por Gateway. Confirmar que devuelve el mismo catálogo que usa la web. Probar preflight CORS desde el origen Vercel.
5. Probar sin token `GET /api/proyectos` (401), con participante (403) y con administrador (200). Esto evidencia autorización de Spring; Gateway aporta entrada/ruteo HTTPS.

**Salida:** health 200, catálogo 200, seguridad 401/403/200 y versión JAR registrada. Consultar `OPERACION_LAB.md` para el procedimiento operativo.

## Puerta 2: autenticación y mantenedores web

1. Registrar una cuenta ficticia en web; confirmar el código por correo; comprobar que nace en grupo `participante`. Cerrar sesión y volver a entrar. No guardar contraseña ni tokens en evidencias.
2. Cambiar el grupo desde administración de Cognito y repetir login para comprobar el nuevo menú. Probar admin, coordinador, monitor y participante; una ruta oculta debe seguir protegida en API.
3. Como admin, crear proyecto con nombre único de prueba. Comprobar respuesta 201 e ID; refrescar página y buscarlo por API. Editar, comprobar cambio en GET, eliminar y confirmar 404/ausencia.
4. Repetir CRUD con taller asociado al proyecto. Registrar ID, fecha y versión. Probar validación de datos y rechazo 403 con un perfil sin permiso.
5. Guardar, editar y borrar un borrador web sin conexión en IndexedDB. Reconectar y comprobar qué registros efectivamente llegan a `/api/sync/batch`.

**Salida:** tabla de peticiones, respuestas y IDs para ambos mantenedores; evidencia visual de cada rol y persistencia tras recarga.

## Puerta 3: móvil y comunicación cruzada

1. Conectar Android con depuración USB; `adb devices` debe mostrar `device`. Compilar `:app:assembleDebug`, instalar el APK de esa compilación y anotar versión.
2. Entrar en Android con una cuenta de prueba. Actualizar talleres y comprobar que aparece el **mismo ID** creado en web. Cortar la red, reiniciar la pantalla y verificar la copia Room; volver a conectar y actualizar tras editar el taller en web.
3. Generar QR del participante en web o Android. Leerlo con la cámara del monitor en el otro cliente; guardar asistencia sin red en Room; comprobar pendiente local.
4. Reconectar y sincronizar. Registrar petición `POST /api/sync/batch`, `acceptedIds`, desaparición de pendientes y cambio de quórum en web. Reintentar el mismo lote y comprobar idempotencia.
5. Probar encuesta móvil y borrador web con la misma pauta de IDs. Si el taller tiene geocerca, probar dentro/fuera de radio y permiso de ubicación denegado; guardar respuestas y códigos HTTP.

**Salida:** evidencia del mismo taller en ambos clientes, asistencia y encuesta persistidas en servidor, quórum actualizado y comportamiento offline/reintento.

## Puerta 4: calidad, diseño e innovación

1. Ejecutar `npm run build` y `npm run coverage` en `web-react`; conservar el reporte y declarar explícitamente el alcance de `vitest.config.ts`. La cifra actual 97,29% solo cubre los módulos incluidos; ampliar instrumentación antes de afirmar 80% global.
2. Ejecutar `mvnw.cmd test` en backend y `gradlew.bat :app:assembleDebug` en móvil. Añadir pruebas de repositorio Room/Retrofit y una prueba de contrato JSON web-móvil-API.
3. Comparar inicio, login, catálogo, taller, QR y estados vacíos/error en web y Android: textos, paleta, jerarquía, botones, navegación y accesibilidad. Probar tamaños compacto y amplio y contraste.
4. Innovación implementada localmente: **bitácora de taller con resumen extractivo y preguntas con cita**. Verificar publicación web, lectura y preguntas Android con el mismo tallerId, permisos, pregunta sin coincidencia y cambio visible tras editar. El dictado web requiere consentimiento y comprobación manual en Chrome. Audio persistente y transcripción Android son una fase posterior, no parte del MVP actual.

## Criterio de cierre

Solo declarar el sistema terminado cuando las cuatro puertas tengan evidencia fechada, no haya 503, dos mantenedores tengan CRUD completo, los cuatro perfiles respeten permisos, el teléfono muestre datos creados en web y la web refleje datos sincronizados desde Android. Registrar las limitaciones que queden, incluida la disponibilidad temporal del laboratorio y cualquier función de innovación aún pendiente.
