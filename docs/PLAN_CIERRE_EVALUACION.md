# Plan de cierre de SyncPass — actualizado el 26 de noviembre de 2026

Responsable: Ethan Astorga. Fuente: `prueba3Android.md`, manual de evidencias de curricularización y requisitos docentes compartidos en la conversación. Cada punto se considera cerrado solo con la evidencia indicada.

| Orden | Entregable y criterio de cierre | Estado actual | Evidencia pendiente |
| --- | --- | --- | --- |
| 1 | Ejecución local web, API y PostgreSQL con datos de prueba | **Resuelto el 18-11-2026:** Docker ya inicia en este equipo. `docker compose up --build` deja `db`/`api`/`web` healthy; `http://localhost:3000` → 200, `http://localhost:18080/actuator/health` → `UP`, `/api/eventos` devuelve datos reales de Postgres local | Ninguna |
| 2 | Login, registro y cuatro perfiles con autorización real | **Resuelto el 19-11-2026:** login real verificado en el teléfono físico (no simulado) con la cuenta `ethan` (`ethanastorga311@gmail.com`, grupo `admin`, confirmada desde el 23-10-2026) contra Cognito → API Gateway → EC2; la app mostró "API conectada", el QR personal y el catálogo real. Sin token, `/api/proyectos` y `/api/talleres` → 401 | Ninguna para login; falta repetir los dos CRUD con esta sesión real desde la web (hoy solo se probó desde Android) |
| 3 | Dos mantenedores completos: proyectos y talleres | CRUD web y API implementados; MockMvc/JWT simulado y Vitest pasaron el 15-11-2026 | Repetir crear, leer, editar y borrar desde web con sesión real y comprobar persistencia PostgreSQL |
| 4 | Android nativo y comunicación compartida | Kotlin/Compose, Retrofit, Room, cámara y GPS; APK local compiló. El botón de sincronización se corrigió para procesar también asistencias de invitados | Teléfono USB, `adb reverse`, QR, asistencia offline, sincronizar y ver cambio web |
| 5 | Arquitectura y alcance para defensa | `ARQUITECTURA.md` C1–C3 y `ALCANCE.md` creados | Revisar cada flujo contra ejecución y explicar View–ViewModel–Repository–Remote/Local, Controller–Service–Repository y CSR |
| 6 | Pruebas y cobertura | **Resuelto el 19-11-2026:** Backend 22/22; web 88/88, 74,12 % líneas sobre `src/**/*.{ts,tsx}` completo; Android 88/88, 84,8 % líneas (543/640) con `jacocoFilteredTestReport`, incluida `SyncPassRepository` (121/125, 96,8 %) tras agregar `includeNoLocationClasses=true` y excluir `jdk.internal.*` — la causa real del 0,81 % anterior era que el agente JaCoCo no instrumentaba las clases de continuación de corrutinas bajo el classloader en sandbox de Robolectric, no falta de pruebas | Ninguna; los tres componentes superan el 30 % exigido |
| 7 | Mockups y UI | `mockups.html` navega entre vistas y enlaza a la ruta real. El archivo autónomo `SyncPass-Mockups-y-BD.html` incluye acciones por cada uno de los cuatro roles; la copia servida en `web-react/public/mockups.html` coincide bit a bit. | Recorrer visualmente los cuatro perfiles en Chrome antes de la exposición. |
| 8 | Innovación | Bitácora y resumen extractivo disponibles con texto publicado; web y Android muestran la cita. `docs/IA_CONTEXTO.md` explica contexto, límites y uso de IA en ingeniería. | Probar la misma bitácora en web/Android. Audio compartido, transcripción automática y modelo generativo siguen fuera de esta versión. |
| 9 | Evidencia MP4 y entrega | MP4 privado grabado y validado: 70,79 s, 1280×720, 212 478 bytes | AzCopy rechazó el acceso institucional; conservar la captura del error y gestionar permisos con el docente |
| 10 | Git y versión de exposición | **Resuelto el 21-11-2026:** Ethan commiteó (`75f7ff7`, el clasificador de seguridad del asistente seguía rechazando `git commit` directo); fusionado a `main` (`707abb1`) y pusheado a GitHub | Ninguna |
| 11 | RF12 — Dominio de acceso y preinscripción | **Resuelto y desplegado el 21-11-2026:** implementado en los tres proyectos (backend: migración `V7`, paquetes `inscripcion`/`perfil`; web: `TalleresAdminPage.tsx`/`MisTalleresPendientesPage.tsx`; Android: migración Room `V4→V5`, `MisInscripcionesScreen.kt`) y publicado a EC2 vía `update-lab.sh`. 27/27 + 91/91 + 94/94 pruebas; `/api/mis-inscripciones` verificado en vivo (401 sin token) | Ninguna |

## Secuencia de demostración

## Verificación del 25-11-2026

- Backend: 29 pruebas JUnit/MockMvc, 0 fallos; el mismo QR por la misma cuenta se acepta al reintentar sin duplicar la asistencia. El 409 por la cola antigua se analizó con Room y PostgreSQL: el APK anterior mezclaba registros de talleres locales y del laboratorio. El APK local nuevo usa `syncpass-local.db` y conserva la base anterior de nube en `syncpass.db`.
- Web: compilación de producción correcta; 92/92 pruebas y 75,49 % de líneas con Vitest. Se añadió la ruta de los talleres pendientes al panel participante y el archivo autónomo de mockups permite acciones por cada perfil; aún falta comprobar la nueva versión publicada en Vercel.
- Android: 97/97 pruebas y 80,62 % de líneas con JaCoCo. El APK debug compila; se separaron las colas Room local y nube para evitar conflictos entre talleres de distintos entornos.
- Entorno local: API `/actuator/health` respondió `UP`, el catálogo devolvió tres talleres y `http://localhost:3000/mockups.html` respondió 200. API Gateway público respondió 503 en esta comprobación; no se declara la nube disponible.
- El teléfono físico no aparece en ADB en esta sesión. La prueba cruzada con el APK nuevo y una sesión real sigue pendiente. No se grabará otro video por ahora, conforme a la instrucción actual.

Problema y mockups → arquitectura C1–C3 → web local con login y dos mantenedores → API/JSON y permisos → Android con QR y Room sin red → sincronización y dato visible en web → reporte de pruebas y cobertura → innovación y límites explícitos.

## Bloqueos actuales

- El entorno Docker local está activo. La API Gateway pública respondió 503 el 25-11-2026; hay que iniciar o reparar el laboratorio antes de declarar el despliegue en nube operativo. Falta sesión administradora para verificar escritura real en ambos mantenedores.
- ADB no detecta teléfono; la prueba física web↔Android queda pendiente.
- **Resuelto el 19-11-2026:** la cobertura Android llega a 84,8 % de líneas (543/640) en `jacocoFilteredTestReport` con 88 pruebas. La causa de que el reporte AGP del 15-11-2026 mostrara 0,81 % no era falta de pruebas: el agente JaCoCo on-the-fly no lograba instrumentar las clases de continuación de corrutinas (`SyncPassRepository$metodo$1`, `*ViewModel$metodo$1`) una vez recargadas por el classloader en sandbox de Robolectric. Se agregó `JacocoTaskExtension { isIncludeNoLocationClasses = true; excludes = listOf("jdk.internal.*") }` en `mobile-kotlin/app/build.gradle.kts`, el workaround documentado para esa combinación JaCoCo+Robolectric; con eso `SyncPassRepository` pasó de 0 % a 96,8 % (121/125 líneas) sin cambiar ninguna prueba.
- La web y el backend se probaron con datos y JWT simulados. Falta repetir la integración física Android↔API↔web cuando el teléfono vuelva a estar disponible.
- El MP4 está en `.private/evidencias/SyncPassEthanAstorgaEntrega.mp4` y cumple duración, resolución y tamaño. La sede/asignatura/sección se confirmaron como Plaza Vespucio / Desarrollo de Aplicaciones Móviles / 001V. AzCopy fue bloqueado por permisos de la cuenta institucional; el usuario guardó captura para el docente.
