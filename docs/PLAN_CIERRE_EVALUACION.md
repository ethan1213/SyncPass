# Plan de cierre de SyncPass — actualizado el 17 de noviembre de 2026

Responsable: Ethan Astorga. Fuente: `prueba3Android.md`, manual de evidencias de curricularización y requisitos docentes compartidos en la conversación. Cada punto se considera cerrado solo con la evidencia indicada.

| Orden | Entregable y criterio de cierre | Estado actual | Evidencia pendiente |
| --- | --- | --- | --- |
| 1 | Ejecución local web, API y PostgreSQL con datos de prueba | Se probó anteriormente; hoy Docker Desktop no inicia el daemon. El despliegue Vercel y Gateway responde 200 | Recuperar Docker y probar los dos CRUD con sesión y persistencia |
| 2 | Login, registro y cuatro perfiles con autorización real | Cognito y Spring Security implementados; local requiere Internet | Registro nuevo, login web/Android, 403 por rol y vistas correctas |
| 3 | Dos mantenedores completos: proyectos y talleres | CRUD web y API implementados; MockMvc/JWT simulado y Vitest pasaron el 15-11-2026 | Repetir crear, leer, editar y borrar desde web con sesión real y comprobar persistencia PostgreSQL |
| 4 | Android nativo y comunicación compartida | Kotlin/Compose, Retrofit, Room, cámara y GPS; APK local compiló. El botón de sincronización se corrigió para procesar también asistencias de invitados | Teléfono USB, `adb reverse`, QR, asistencia offline, sincronizar y ver cambio web |
| 5 | Arquitectura y alcance para defensa | `ARQUITECTURA.md` C1–C3 y `ALCANCE.md` creados | Revisar cada flujo contra ejecución y explicar View–ViewModel–Repository–Remote/Local, Controller–Service–Repository y CSR |
| 6 | Pruebas y cobertura | Backend: 22/22; web: 37/37, 90,06 % líneas instrumentadas; Android: 4/4, 0,81 % líneas globales con reporte AGP | Ampliar pruebas Android hasta ≥30 % global medido; luego volver a ejecutar todos los reportes |
| 7 | Mockups y UI | Mockups por rol y navegación web existentes | Recorrido por cuatro perfiles; comparación Android/web; corregir diferencias funcionales importantes |
| 8 | Innovación | Bitácora y resumen extractivo disponibles con texto publicado | Probar misma bitácora en web/Android; audio y transcripción automática siguen en backlog |
| 9 | Evidencia MP4 y entrega | MP4 privado grabado y validado: 70,79 s, 1280×720, 212 478 bytes | AzCopy rechazó el acceso institucional; conservar la captura del error y gestionar permisos con el docente |
| 10 | Git y versión de exposición | Rama `feature/bitacora-innovacion` | Commit sin secretos, push, PR revisado hacia `main` después de pasar pruebas |

## Secuencia de demostración

Problema y mockups → arquitectura C1–C3 → web local con login y dos mantenedores → API/JSON y permisos → Android con QR y Room sin red → sincronización y dato visible en web → reporte de pruebas y cobertura → innovación y límites explícitos.

## Bloqueos actuales

- Docker Desktop no pudo iniciar el daemon el 17-11-2026. La API pública en Gateway respondió 200 en `/actuator/health` y `/api/eventos`; `/api/proyectos` respondió 401 sin token, como corresponde. Falta sesión administradora para verificar escritura en ambos mantenedores.
- ADB no detecta teléfono; la prueba física web↔Android queda pendiente.
- La cobertura Android oficial AGP del 15-11-2026 es 0,81 % de líneas (16 cubiertas de 1 979). Las dos pruebas Robolectric pasan, pero sus clases transformadas no suman líneas al reporte; las dos pruebas JVM puras sí. No presentar 30 % como logrado.
- La web y el backend se probaron con datos y JWT simulados. Falta repetir la integración física Android↔API↔web cuando el teléfono vuelva a estar disponible.
- El MP4 está en `.private/evidencias/SyncPassEthanAstorgaEntrega.mp4` y cumple duración, resolución y tamaño. La sede/asignatura/sección se confirmaron como Plaza Vespucio / Desarrollo de Aplicaciones Móviles / 001V. AzCopy fue bloqueado por permisos de la cuenta institucional; el usuario guardó captura para el docente.
