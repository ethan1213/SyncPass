# Plan de cierre de SyncPass — 13 de noviembre de 2026

Responsable: Ethan Astorga. Fuente: `prueba3Android.md`, manual de evidencias de curricularización y requisitos docentes compartidos en la conversación. Cada punto se considera cerrado solo con la evidencia indicada.

| Orden | Entregable y criterio de cierre | Estado actual | Evidencia pendiente |
| --- | --- | --- | --- |
| 1 | Ejecución local web, API y PostgreSQL con datos de prueba | `start-local.ps1` terminó; health 200, catálogo 200 con 3 talleres, filtro por comuna 200 y web local activa | Probar los dos CRUD con sesión y persistencia |
| 2 | Login, registro y cuatro perfiles con autorización real | Cognito y Spring Security implementados; local requiere Internet | Registro nuevo, login web/Android, 403 por rol y vistas correctas |
| 3 | Dos mantenedores completos: proyectos y talleres | CRUD web y API implementados | Crear, leer, editar y eliminar desde web; comprobar JSON y persistencia |
| 4 | Android nativo y comunicación compartida | Kotlin/Compose, Retrofit, Room, cámara y GPS; APK local compiló | Teléfono USB, `adb reverse`, QR, asistencia offline, sincronizar y ver cambio web |
| 5 | Arquitectura y alcance para defensa | `ARQUITECTURA.md` C1–C3 y `ALCANCE.md` creados | Revisar cada flujo contra ejecución y explicar View–ViewModel–Repository–Remote/Local, Controller–Service–Repository y CSR |
| 6 | Pruebas y cobertura | Build web pasa; pruebas Android iniciadas | Finalizar JaCoCo, alcanzar o declarar honestamente el umbral Android de 30%; verificar cobertura web exigida para la evaluación correspondiente |
| 7 | Mockups y UI | Mockups por rol y navegación web existentes | Recorrido por cuatro perfiles; comparación Android/web; corregir diferencias funcionales importantes |
| 8 | Innovación | Bitácora y resumen extractivo disponibles con texto publicado | Probar misma bitácora en web/Android; audio y transcripción automática siguen en backlog |
| 9 | Evidencia MP4 y entrega | Procedimiento en `EVIDENCIA_ANDROID.md` | Grabar <=90 s, 1280×720, <=50 MB; validar con ffprobe; subir con AzCopy a sección 001v y guardar captura de terminal |
| 10 | Git y versión de exposición | Rama `feature/bitacora-innovacion` | Commit sin secretos, push, PR revisado hacia `main` después de pasar pruebas |

## Secuencia de demostración

Problema y mockups → arquitectura C1–C3 → web local con login y dos mantenedores → API/JSON y permisos → Android con QR y Room sin red → sincronización y dato visible en web → reporte de pruebas y cobertura → innovación y límites explícitos.

## Bloqueos actuales

- Docker Desktop está activo; el catálogo público ya funciona localmente. Falta una sesión administradora para verificar escritura en ambos mantenedores.
- ADB no detecta teléfono; la prueba física web↔Android queda pendiente.
- JaCoCo Android aún no generó porcentaje confirmado. No presentar 30% como logrado.
- No hay MP4 grabado ni carga AzCopy realizada. Confirmar sede/asignatura exacta antes de entregar.
