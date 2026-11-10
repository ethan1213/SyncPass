# SyncPass: informe técnico y plan de cierre

Actualizado: 10 de noviembre de 2026. Este informe distingue lo que está implementado en el repositorio de las pruebas de integración que aún deben repetirse con AWS Academy activo y un teléfono conectado.

Línea base local del 10 de noviembre: `npm run build` completó; `npm run coverage` ejecutó 34 pruebas sin fallos y reportó 97,29 % de líneas y 89,28 % de ramas **solo en los módulos incluidos por Vitest**. `mvnw.cmd test` ejecutó 19 pruebas sin fallos usando JDK 27. `:app:assembleDebug` completó usando el JBR de Android Studio y `ANDROID_HOME` apuntando al SDK instalado. Estos resultados no certifican todavía la integración publicada.

## 1. Problema, cliente y solución

El caso describe talleres de vinculación comunitaria gestionados con planillas y registros en papel. Esto retrasa la acreditación, dificulta consolidar asistencia y encuestas, y deja sin operación a los equipos cuando falla la señal. SyncPass propone planificación web, consulta y captura móvil, acreditación QR, encuestas y sincronización de pendientes.

La propuesta es académica. Los datos usados para demostración deben ser ficticios o anonimizados, como exige `docs/CASO.md`. Las metas del caso sobre tiempos, disponibilidad y tasas de respuesta son objetivos, no resultados medidos.

## 2. Arquitectura implementada

```text
React/Vite en Vercel ────────┐
                            ├─ HTTPS → API Gateway → HTTPS/Caddy → Spring Boot JAR en EC2
Android/Kotlin/Compose ─────┘                                      ├─ AWS Cognito: identidad y grupos
                                                                   └─ PostgreSQL/Supabase: datos compartidos
React → IndexedDB: borradores y cola local
Android → Room/SQLite: catálogo y registros pendientes
```

Los clientes no se llaman entre sí. Comparten datos cuando ambos leen y escriben mediante la API y la misma base PostgreSQL. IndexedDB y Room son almacenes distintos en cada dispositivo; guardar solo en uno de ellos no hace que el dato aparezca en el otro. Un registro pendiente se vuelve compartido después de que `/api/sync/batch` lo acepte y lo persista.

### Flujo web

`web-react/src/main.tsx` monta React; `App.jsx` y Wouter seleccionan la página. `SessionProvider.tsx` llama a `/api/auth/*`, conserva la sesión en `sessionStorage` y entrega el token. `clients/api.ts` serializa JSON, envía `Authorization: Bearer` y procesa la respuesta. `pages/ProyectosAdminPage.jsx` y `pages/TalleresAdminPage.jsx` son los dos mantenedores remotos. `data/localDb.js` maneja borradores y pendientes en IndexedDB. La interfaz usa CSS propio y Tailwind como utilidades puntuales. Varias páginas siguen en JSX, aunque la entrada y módulos compartidos son TypeScript: no corresponde afirmar que toda la web está migrada a TSX.

### Flujo Android

`MainActivity.kt` crea `SyncPassRepository` y organiza la navegación Compose. `HomeScreen.kt` usa `HomeViewModel.kt`, que publica `HomeUiState` con `StateFlow`. El repositorio usa Retrofit para autenticación, catálogo y lotes, y Room para catálogo y pendientes. `LoginScreen`, `RegisterScreen`, `ScannerScreen` y `SurveyScreen` aún reciben el repositorio directamente; para cumplir de forma consistente el flujo de la pizarra deben tener ViewModel y estado propios. Los DTO de red y las entidades Room están concentrados en `SyncPassRepository.kt`; conviene separarlos por responsabilidad.

### Backend y seguridad

`AuthController` expone registro, confirmación, login, refresh y logout; la implementación usa el SDK de Cognito desde EC2. `SecurityConfig` valida JWT para rutas protegidas. Los grupos son `admin`, `coordinador`, `monitor` y `participante`. `ProyectoController` y `TallerController` implementan CRUD con Spring Data JPA; Flyway versiona el esquema. `SyncController` recibe lotes de asistencia/encuesta y expone quórum. El backend entrega JSON; React y Compose construyen las vistas.

Para exponerlo: **vista → ViewModel → repositorio → fuente de datos** describe Android. En web: **página/componente → cliente HTTP → API REST**. En servidor: **controlador REST → repositorio JPA → PostgreSQL**. Los DTO son contratos de transporte entre capas y no sustituyen a una fuente de datos.

## 3. Qué está completo y qué falta demostrar

| Área | Estado en el código | Evidencia que falta para declararla cerrada |
| --- | --- | --- |
| Registro/login | Web y Android llaman a API Gateway; backend usa Cognito. | Repetir registro, login y rechazo por rol en el despliegue final. |
| Proyectos | CRUD web + REST con validaciones y versión optimista. | Recorrido real POST, GET, PUT, GET, DELETE con respuesta HTTP y persistencia. |
| Talleres | CRUD web + REST, asociación opcional a proyecto. | Mismo recorrido real y consulta del taller en Android. |
| Datos locales | IndexedDB web; Room Android. | Mostrar sin red, pendiente, reintento y eliminación solo tras `acceptedIds`. |
| Conexión web/móvil | Comparten URL de API y catálogo remoto. | Probar creación web → consulta móvil y asistencia móvil → quórum web. |
| QR/GPS | Hay generación, lectura y validación en código. | Prueba física entre dispositivos con JAR y APK actuales. |
| Cobertura | Vitest mide módulos seleccionados; hay pruebas Java. | Medir alcance global y alcanzar el umbral exigido en las capas evaluadas. |
| Diseño | Paleta y mockups propios. | Evaluación de usabilidad con cliente y revisión de móvil, formularios y estados. |
| Infraestructura | Guiones para Vercel, Gateway, EC2, Caddy, Cognito y Supabase. | Verificar rutas y salud tras reiniciar el laboratorio; conservar evidencia fechada. |

## 4. Plan de cierre, en orden

### Fase A: contrato y modelo compartido

1. Hacer una tabla de entidades y campos: proyecto, taller, usuario/rol, inscripción, asistencia y encuesta. Marcar dueño del dato: Cognito para identidad, PostgreSQL para negocio, Room/IndexedDB para copia o cola.
2. Definir DTO de entrada y salida, estados HTTP, permisos y ejemplos JSON de cada endpoint. Conservar compatibilidad para el APK y la web ya publicados.
3. Fijar identificadores estables, versión para edición y clave de idempotencia para lotes. Definir qué ocurre ante duplicado, error parcial y falta de conexión.

**Aceptación:** Postman y ambos clientes interpretan el mismo contrato; un cambio de esquema tiene migración Flyway y prueba de compatibilidad.

### Fase B: dos mantenedores completos

1. Terminar proyectos: crear, listar, detalle, editar, borrar, validación de fechas, asociación y permisos.
2. Terminar talleres: mismos pasos, cupos, ubicación opcional y relación con proyecto; impedir eliminación inconsistente cuando existan dependencias.
3. Añadir estados de carga, vacío, éxito, error y confirmación de borrado en React. No enseñar una operación como verificada si solo aparece el botón.

**Aceptación:** para cada mantenedor, ejecutar `POST → GET → PUT → GET → DELETE → GET`, verificar códigos y contenido en Postman y en la interfaz; probar `401/403`, `400`, `404` y conflicto `409` cuando aplique.

### Fase C: app Android y dos fuentes de datos

1. Crear ViewModel/UiState para login, registro, escáner y encuesta. Las pantallas envían eventos y renderizan estado; el repositorio decide entre Retrofit y Room.
2. Separar `remote` (API y DTO), `local` (Room, entidades y DAO), `repository` y `ui`. Añadir mapeos explícitos DTO ↔ entidad local ↔ modelo de pantalla cuando sus campos difieran.
3. Definir estrategia de lectura: mostrar caché de Room al abrir, actualizar desde API cuando haya red, conservar caché ante error. En escritura, guardar pendiente local con dueño e identificador único; sincronizar y retirar solo los IDs aceptados.
4. Mantener el mismo contrato y las mismas reglas de rol en web y móvil. El móvil no necesita duplicar el mantenedor administrativo si la pauta solo pide dos mantenedores web; sí debe consumir y reflejar los datos compartidos.

**Aceptación:** en teléfono real, taller creado en web aparece en móvil; asistencia/encuesta creada sin señal queda pendiente, luego se sincroniza y se ve en web sin duplicarse.

### Fase D: interfaz hecha para el cliente

1. Revisar mockups y lenguaje con tareas reales: planificar taller, acreditar rápido, responder encuesta y revisar quórum.
2. Simplificar navegación por perfil y dar prioridad al botón principal de cada pantalla. Mejorar contraste, tamaños táctiles, mensajes de error y accesibilidad de formularios.
3. Compartir criterios visuales entre web y Android: paleta, jerarquía, iconografía, estados y términos. No copiar literalmente la disposición de escritorio al teléfono.
4. Validar en escritorio, teléfono y modo sin conexión. Rehacer capturas de mockups de acuerdo con la versión final.

**Aceptación:** un usuario de prueba completa las tareas principales sin indicaciones; las dos interfaces se reconocen como SyncPass y el flujo de acreditación es legible en terreno.

Primer ajuste local: la portada web presenta dos acciones principales, deja la descarga y los mockups como enlaces secundarios y explica el estado sin talleres. El rediseño de los flujos internos sigue pendiente de validación con el cliente.

### Fase E: innovación útil al caso

Empezar por **resumen de taller a partir de notas aprobadas**. Permite generar acuerdos, temas y preguntas frecuentes sin grabar personas. Luego evaluar audio/transcripción y preguntas sobre el contenido del taller, con consentimiento, control de acceso, corrección humana y un límite de costo. Una respuesta debe indicar qué nota o tramo de transcripción la respalda; si no existe evidencia, debe decir que no sabe. No mezclar el prototipo con el CRUD obligatorio hasta que este esté cerrado.

**Aceptación:** demostración con datos ficticios, respuesta trazable a la fuente y prueba de que no inventa información ausente.

### Fase F: calidad, despliegue y entrega

1. Pruebas unitarias de reglas, DTO/mapeos y estados; integración de controladores y base; recorrido web/API/móvil de las tareas principales.
2. Publicar cobertura con denominador y exclusiones visibles. El porcentaje actual de Vitest corresponde a módulos seleccionados, no a toda la web. Si la pauta exige 80% global, medir y cerrar esa diferencia antes de afirmarlo.
3. Compilar JAR y APK, revisar que el APK descargable corresponde al mismo commit, publicar web en Vercel, actualizar EC2 y probar Gateway, Cognito y PostgreSQL.
4. Guardar en Git código, migraciones, pruebas, mockups y documentación; excluir `.env`, tokens, contraseñas, llaves y datos reales. Usar rama de trabajo y PR revisable antes de integrar a `main`.
5. Preparar exposición: problema → mockups → arquitectura → login/roles → dos mantenedores → web/móvil compartidos → offline/QR/GPS → pruebas y límites.

**Aceptación:** compilaciones limpias, pruebas exigidas aprobadas, enlaces públicos operativos mientras el laboratorio esté activo, Postman reproducible, APK instalable y documentación que coincide con el código publicado.

## 5. Orden recomendado para la próxima sesión de implementación

1. Cerrar contrato y pruebas de los dos CRUD.
2. Completar flujo móvil ViewModel/Repository/Room/Retrofit y prueba cruzada.
3. Mejorar interfaz según las tareas del cliente.
4. Validar QR/GPS y desplegar artefactos finales.
5. Añadir innovación con alcance controlado.

Los repositorios docentes `docentedev/fs2-react-app`, `fs2-api` y `fs2-mobile` sirven para estudiar estructura y convenciones. SyncPass conserva sus propios nombres, entidades y reglas del caso. Su código no debe copiarse sin comprobar licencia, pertinencia y compatibilidad.

## 6. Preguntas frecuentes para la defensa

**¿Cómo se comunican web y móvil?** Por la misma API y PostgreSQL. No comparten memoria ni bases locales. Un dato local pasa a ser visible para el otro cliente tras sincronizarse.

**¿Por qué API Gateway y EC2?** Gateway ofrece entrada HTTPS estable y enruta las solicitudes; EC2 ejecuta Caddy y el JAR con la lógica REST. Cuando el laboratorio se apaga, la web de Vercel puede cargar, pero las funciones que necesitan la API dejan de responder.

**¿Dónde se valida el rol?** El cliente oculta rutas y acciones, pero la autorización efectiva está en Spring Security y `@PreAuthorize`, usando grupos del JWT de Cognito.

**¿Qué hace un DTO?** Define el JSON de una petición o respuesta. Evita acoplar directamente la interfaz a las tablas locales o entidades JPA.

**¿Qué hace el Repository de Android?** Coordina Retrofit y Room: obtiene catálogo remoto, guarda copia local, encola registros y envía lotes pendientes.

**¿Qué ocurre sin internet?** Se puede consultar lo ya guardado y capturar operaciones locales previstas. El inicio de sesión nuevo y la sincronización remota necesitan conexión. No se debe prometer disponibilidad total de funciones sin red.

**¿Cómo evita duplicados la sincronización?** Cada pendiente tiene ID; el servidor devuelve `acceptedIds`; el cliente elimina de su cola únicamente los aceptados. La prueba de idempotencia debe repetirse extremo a extremo.

**¿Qué significa 80% de cobertura?** Es la proporción de código instrumentado que ejecutan las pruebas, según líneas o ramas y las inclusiones configuradas. Debe mostrarse el reporte y explicar qué módulos quedaron fuera; no demuestra por sí sola corrección funcional.
