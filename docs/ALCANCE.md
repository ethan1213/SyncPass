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

## Pendientes explícitos

- Subida de audio al servidor y transcripción automática; actualmente se graba y descarga localmente en la web.
- Aviso por correo del resumen; actualmente hay aviso en la vista.
- Persistencia segura y renovación automática de la sesión Android tras cerrar la app.
- Sincronización con la app cerrada mediante `WorkManager`.
- Cobertura Android ≥30 % con JaCoCo y pruebas unitarias de ViewModels/Repository: debe medirse antes de declararla cumplida.
