# Alcance funcional de SyncPass — evaluación móvil 3

## Problema y solución

Los talleres comunitarios necesitan inscripción, acreditación, encuestas y recuento de asistentes aunque la conexión del lugar sea intermitente. SyncPass comparte talleres y resultados en una API, y conserva un catálogo y registros pendientes en el dispositivo para enviarlos después. Una persona puede abrir un taller con un código o QR sin crear cuenta; una cuenta iniciada usa su identidad de Cognito.

## Requisitos funcionales

| ID | Requisito | Implementación | Comprobación |
| --- | --- | --- | --- |
| RF01 | Registrar, confirmar e iniciar sesión | `/api/auth/*` → Cognito; web y Android consumen API | Registro, confirmación y login con cuenta de prueba |
| RF02 | Cuatro perfiles | Grupos `admin`, `coordinador`, `monitor`, `participante` | Panel y respuesta 403 al intentar operación ajena |
| RF03 | Mantener proyectos y talleres | CRUD web → Controllers/Services/JPA | Crear, leer, editar y borrar datos ficticios |
| RF04 | Consultar talleres con o sin señal | Retrofit y copia Room; IndexedDB en web | Desconectar red y abrir catálogo ya visitado |
| RF05 | Entrar por número o QR | `SPT:<id>` abre detalle en Android | Escanear QR de `/taller/:id` |
| RF06 | Registrar presencia invitada o con cuenta | `/ingreso/invitado` o `/ingreso/cuenta` | Ver respuesta `presente` o `ya_presente` y quórum |
| RF07 | Acreditar participantes | QR `SP1:<UUID>` y `/api/sync/batch` | Escaneo con rol de monitor y envío de la cola |
| RF08 | Guardar encuestas y asistencias sin conexión | Room `pending` e `ingresos`; IndexedDB web | Modo avión, crear pendiente, reconectar y comprobar envío |
| RF09 | Validar ubicación cuando hay geocerca | LocationManager + `GeoFence` del backend | Dentro/fuera del radio y precisión en pruebas |
| RF10 | Consultar bitácora y resumen | Texto publicado, resumen extractivo y preguntas con cita | Web y Android consultan el mismo taller |
| RF11 | Declarar si el taller se grabó | Estado `/grabacion` de admin/coordinador | Ver aviso de resumen pendiente o disponible |
| RF12 | Restringir un taller por dominio (rol y/o zona) y preinscripción con asistencia separada | **Implementado y desplegado el 21-11-2026** en backend (`taller/Taller.java`, `inscripcion/`, `perfil/`), web (`TalleresAdminPage.tsx`, `MisTalleresPendientesPage.tsx`) y Android (`SyncPassRepository.kt`, `MisInscripcionesScreen.kt`) | 27/27 pruebas backend (`DominioTallerTest`), 91/91 web, 94/94 Android; en vivo desde `main` (`707abb1`) vía `update-lab.sh`, verificado con `/api/mis-inscripciones` → 401 sin token |

## Requisitos no funcionales

| ID | Requisito | Evidencia o límite |
| --- | --- | --- |
| RNF01 | App nativa Kotlin y MVVM | `Screen` Compose → `ViewModel` → `SyncPassRepository` → Retrofit/Room |
| RNF02 | Dos APIs nativas | CameraX/ML Kit para cámara; LocationManager para GPS |
| RNF03 | Offline first | Room y cola con reintento; requiere app abierta para sincronización automática |
| RNF04 | Acceso por rol | Spring Security valida JWT Cognito; rutas web ocultan acciones y API aplica 403 |
| RNF05 | Integridad | Flyway, claves únicas, bloqueo de cupos y control optimista de versiones |
| RNF06 | Interfaz adaptable | React/CSS y Compose para teléfono/tablet; mockups por rol |
| RNF07 | Pruebas reproducibles | Maven/JUnit para backend; Vitest web; cobertura Android requiere reporte JaCoCo propio |
| RNF08 | Datos privados fuera de Git | `.env` ignorado; contraseñas locales y tokens no se publican |

## Casos de uso principales

**CU01 — Ingreso a taller:** el creador publica taller y QR `SPT:<id>`; participante escanea o escribe número; la app consulta API o Room y abre detalle. Invitado escribe nombre; cuenta iniciada usa su `sub`. Si hay geocerca, Android pide GPS. El backend valida cupos y devuelve `presente` o `ya_presente`. Si no hay red, Room guarda el intento y se reenvía al reconectar.

**CU02 — Acreditación por monitor:** monitor inicia sesión; escanea `SP1:<UUID>`; Android guarda una asistencia con hora y GPS si corresponde; la cola se envía por lotes. La API verifica rol, QR, geocerca e idempotencia; el quórum cambia al aceptar el registro.

**CU03 — Mantenedores:** administrador o coordinador abre `/proyectos` o `/talleres`, edita un formulario y envía JSON a la API. El Service valida reglas y la versión antes de persistir. Un usuario sin rol recibe 403 aunque invoque la URL manualmente.

**CU04 — Resumen de taller:** coordinador marca si hubo grabación y publica transcripción o apuntes. El backend construye un resumen extractivo y permite preguntas con citas del texto. Las vistas muestran el estado; un archivo de audio por sí solo no genera transcripción en esta versión.

## Trazabilidad de mockups

`/mockups.html#roles` muestra los cuatro perfiles y enlaza al panel real. `/mockups.html#crud` enlaza a los mantenedores reales. El QR y la vista móvil de ingreso se demuestran en el APK. Las maquetas representan pantallas y navegación; no deben presentarse como sustituto de pruebas de la API.

Las seis vistas de `docs/MOCKUPS.md` ahora incluyen boceto Web además del móvil
(se agregaron los bocetos web que faltaban para las vistas 3, 4 y 6). Cada boceto
trae una nota "Implementado" con el archivo y la ruta real de `web-react/src/pages/`,
y una nota "Pendiente" con lo que falta para esa vista puntual. `SurveyPage.tsx`
existe como boceto alternativo de la encuesta pero no está enrutado: la encuesta
real en producción es `DraftsPage.tsx` (`/encuesta` y `/borradores`).

## Pendientes explícitos

- QR personal para el invitado/vecino sin cuenta: hoy `SP1:<UUID>` solo existe para quien inició sesión (sale del `sub` de Cognito), así que el vecino se acredita por un formulario de nombre (`/ingreso/invitado`), no porque el monitor le escanee un código propio como sugiere el mockup de la Vista 3. Implementarlo exige un formato de identificador que no rompa la idempotencia `guest:<deviceId>` ya usada en ambas colas de Android (`ingresos` y `pending`). Ver `DEFENSA_ARQUITECTURA.md` §8.
- Subida de audio al servidor y transcripción automática; actualmente se graba y descarga localmente en la web.
- Aviso por correo del resumen; actualmente hay aviso en la vista.
- Persistencia segura y renovación automática de la sesión Android tras cerrar la app: se evaluó agregar `SharedPreferences` para recordar la sesión, pero se revirtió a propósito el 20-11-2026 — la pauta exige que ingresar requiera cuenta siempre y que cerrar sesión la termine de verdad, así que la app vuelve a pedir login cada vez que el proceso se reinicia.
- Sincronización con la app cerrada mediante `WorkManager`.
- ~~Cobertura Android ≥30 % con JaCoCo y pruebas unitarias de ViewModels/Repository~~ **Resuelto el 19-11-2026:** 88 pruebas, 84,8 % líneas filtradas (543/640); ver `PLAN_CIERRE_EVALUACION.md`.
- **RF12 — Dominio de acceso y preinscripción (implementado el 19-11-2026).** El dominio combina **rol Y zona**, y la preinscripción es un registro real en el servidor (no un filtro de la app):
  - **Backend — columnas nuevas en `Taller`:** `rolesPermitidos` (texto CSV con los roles Cognito que pueden ver/unirse; `null` = abierto a los cuatro) y `zonaRestringida` (texto con la unidad vecinal exigida; `null` = sin restricción de zona). Migración `V7__dominio_y_preinscripcion.sql`.
  - **Backend — tabla `usuario_zona` (sub, zona)** y controlador `perfil/UsuarioZonaController.java` (`PUT /api/usuarios/{sub}/zona`, solo admin): como Cognito no tiene un atributo de zona por usuario, se guarda en PostgreSQL.
  - **Backend — entidad `inscripcion/Inscripcion.java`** (tallerId, participanteSub o deviceId para invitado, estado `pendiente`/`asistio`): `POST /api/eventos/{id}/inscripcion` crea el registro en `pendiente`; al aceptarse una acreditación real, `IngresoService` lo pasa a `asistio` (o lo crea directo en `asistio` si no hubo preinscripción, para no bloquear la asistencia espontánea). `GET /api/eventos/{id}/inscripciones` (admin/coordinador) y `GET /api/mis-inscripciones` (cualquier cuenta, sus propias pendientes).
  - **Backend — regla de dominio en `IngresoService.verificarDominio()`:** antes de aceptar el ingreso, valida `rolesPermitidos` contra `cognito:groups` y `zonaRestringida` contra `usuario_zona`; si no cumple, 403. Un invitado sin cuenta no tiene rol Cognito, así que cualquier taller con `rolesPermitidos` definido lo excluye automáticamente — mismo criterio que ya aplicaba a la bitácora.
  - **Pruebas:** `DominioTallerTest.java` (5 casos: rechazo por rol, invitado nunca pasa un taller restringido por rol, rechazo/aceptación por zona, ciclo pendiente→asistio, aislamiento de `mis-inscripciones` por usuario). Suite completa: 27/27.
  - **Web:** `TalleresAdminPage.tsx` agrega checkboxes de rol + campo de zona; `MisTalleresPendientesPage.tsx` (ruta `/mis-talleres`) lista el catálogo, marca "🔒" en los talleres restringidos y deja "Marcar que voy". Suite completa: 91/91, 75,37% líneas.
  - **Android:** `TallerDto` suma `rolesPermitidos`/`zonaRestringida` (migración Room `V4→V5`), `SyncPassRepository` suma `marcarPendiente()`/`misInscripciones()`, y `MisInscripcionesScreen.kt`/`MisInscripcionesViewModel.kt` repiten la misma pantalla. Suite completa: 94/94, 85,0% líneas filtradas.
  - **Desplegado el 21-11-2026:** commit `75f7ff7` fusionado a `main` (`707abb1`), publicado a EC2 vía `deploy/update-lab.sh` y verificado en vivo: `/api/mis-inscripciones` responde 401 sin token (antes era 404), `/actuator/health` y `/api/eventos` siguen en 200.
  - **Confirmado y sin cambios:** el resumen de bitácora con IA ya estaba restringido a cuentas con rol real (`BitacoraController` exige `hasAnyRole('admin','coordinador','monitor','participante')`); un invitado nunca lo recibe, sin necesidad de cambio adicional.
