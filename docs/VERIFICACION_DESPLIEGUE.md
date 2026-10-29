# Verificación de SyncPass · 29 de octubre de 2026

Esta evidencia corresponde al laboratorio activo al momento de comprobarlo. La IPv4 cambia después de detener e iniciar EC2; el resultado debe repetirse el día de la exposición.

| Comprobación | Resultado observado |
| --- | --- |
| Repositorio integrado | `main` en `ethan1213/SyncPass`, commit `81ef4c8`; EC2 usa el mismo commit y origen Git. |
| Web publicada | `https://web-react-nu-nine.vercel.app/` y `/proyectos` respondieron HTTP 200; Vercel quedó conectado al repositorio nuevo. |
| API Gateway | Ruta `$default` apunta por HTTPS a `https://api.13-223-59-116.nip.io`; `/actuator/health` respondió `{"status":"UP"}`. |
| Catálogo y base compartida | `GET /api/eventos` respondió HTTP 200 con talleres. |
| Seguridad de API | `GET /api/proyectos` y `/api/me` sin token respondieron HTTP 401. |
| CORS web | Preflight `OPTIONS /api/proyectos` desde Vercel respondió HTTP 204 con origen, métodos y cabeceras permitidos. |
| Cognito desde EC2 | Un login con cuenta ficticia devolvió `NOT_AUTHORIZED`, respuesta propia del controlador tras consultar Cognito. No se usaron credenciales personales. |
| Grupos Cognito | `admin`, `coordinador`, `monitor` y `participante` presentes. El cliente usado por la API admite login con contraseña y refresh. |
| Acceso Android | El código Kotlin usa Retrofit hacia `/api/auth/*` en Gateway; se retiró AppAuth y el retorno directo de Cognito del APK. Falta probar login real con la versión recompilada en teléfono. |
| EC2 y red | Instancia `i-01d851d6163897cb1` en ejecución; IPv4 temporal `13.223.59.116`; grupo de seguridad con ingreso público solo 80 y 443; puerto 8080 permanece interno. |
| Arranque automático | `syncpass-api`, `caddy` y `syncpass-gateway-sync` habilitados y activos; este último terminó con `Result=success` y registró la IP. |
| Pruebas y compilación | 11 pruebas de Spring Boot sin fallos, `npm run build` correcto y `:app:assembleDebug` correcto. |

## Avance QR/GPS y pruebas locales

La rama actual agrega geocerca opcional por taller con migración Flyway V3. Web y Android capturan latitud, longitud y precisión antes de guardar una asistencia; la API valida el formato del QR y la distancia al radio al sincronizar. La migración pasó en H2 y las pruebas completas de backend suman 15 casos sin fallos. El APK compila con la migración Room 2→3.

La web usa `main.tsx`, sesión, roles, configuración, cliente HTTP y parser QR en TypeScript, un componente `GeofenceNote.tsx`, Tailwind configurado y las páginas existentes en JSX. `npm run coverage` ejecutó 28 pruebas y reportó 95,45% de líneas en siete módulos seleccionados. **Ese porcentaje no representa toda la web.** Faltan prueba física de cámara/GPS entre dispositivos y comprobación en el JAR publicado tras aplicar la migración en Supabase.

No se ha medido un porcentaje de cobertura. La sesión real, los cuatro perfiles en vivo y el recorrido completo de cámara/QR en el APK requieren pruebas manuales con una cuenta y un teléfono durante la presentación. En la última compilación no había teléfono ADB conectado.

## Repetir la comprobación al iniciar el laboratorio

```bash
sudo journalctl -u syncpass-gateway-sync -n 30 --no-pager
sudo systemctl status syncpass-api caddy syncpass-gateway-sync --no-pager
```

```powershell
curl.exe https://hluamat8vg.execute-api.us-east-1.amazonaws.com/actuator/health
curl.exe -I https://web-react-nu-nine.vercel.app/
```

La URL de Gateway y la de Vercel son las que se comparten. Para saber la IPv4 actual, leer `SYNCPASS_PUBLIC_IP` en el registro o la columna **Dirección IPv4 pública** del panel EC2. Si Gateway devuelve 503, seguir [Operación del laboratorio](OPERACION_LAB.md).
