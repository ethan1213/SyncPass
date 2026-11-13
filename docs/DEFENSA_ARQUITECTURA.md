# Pauta de defensa: capas y patrones de SyncPass

Respuestas cortas con el archivo real que las respalda. Si el profesor pide
"muéstrame dónde", abre el archivo indicado.

---

## 1. Resumen en una frase

SyncPass tiene **dos clientes y un servidor**: app Android en Kotlin + Compose
(MVVM), web React renderizada en el navegador (CSR) y una API REST en Spring Boot
sobre PostgreSQL, con autenticación en AWS Cognito. Los dos clientes guardan datos
localmente (Room en Android, IndexedDB en la web) para seguir operando sin señal.

---

## 2. Tabla capa por capa

| Concepto | Android (Kotlin) | Web (React) | Backend (Spring Boot) |
|---|---|---|---|
| **View** | `ui/screens/*Screen.kt` (`@Composable`) | `src/pages/*.jsx` | — (no renderiza HTML) |
| **ViewModel** | `ui/screens/*ViewModel.kt` (7) | hooks + `SessionProvider.tsx` | — |
| **Controller** | `MainActivity.kt` → `SyncPassNavigation` (NavHost) | `App.jsx` (router) | `*/<Dominio>Controller.java` |
| **Service** | el repositorio cumple ese rol | `clients/api.ts` | `*/<Dominio>Service.java` (7) |
| **Repository** | `data/SyncPassRepository.kt` | `data/localDb.js` + `clients/api.ts` | `TallerRepository`, `ProyectoRepository` |
| **Model / entidades** | `TallerDto`, `PendingRecord`, `PendingEntry` | JSON de la API | `Taller`, `Proyecto` (`@Entity`) |
| **DAO / fuente local** | `PendingDao`, `TallerDao`, `IngresoDao` (Room) | IndexedDB (`idb`) | JPA y `JdbcTemplate` |
| **Red** | Retrofit + Gson (`SyncPassApi`) | `fetch` | — |

### Paquetes del backend (uno por dominio)

```
cl.duoc.syncpass
├── Application.java
├── config/     SecurityConfig · CognitoConfig · ApiErrors
├── common/     GeoFence
├── auth/       AuthController · AuthService
├── taller/     Taller · TallerRepository · TallerService · TallerController
├── proyecto/   Proyecto · ProyectoRepository · ProyectoService · ProyectoController
├── ingreso/    IngresoController · IngresoService
├── bitacora/   BitacoraController · BitacoraService · BitacoraTexto
├── grabacion/  GrabacionController · GrabacionService
└── sync/       SyncController · SyncService
```

Regla que se cumple en los 7 dominios: **el controller solo traduce HTTP**
(ruta, validación de entrada, rol y código de respuesta) y **el service decide**.
Ningún controller tiene una consulta SQL.

---

## 3. Si pregunta por **MVVM** en Android

Flujo: **Composable → ViewModel → Repository → (Retrofit | Room)**

1. `HomeScreen.kt` es la View: solo dibuja y entrega eventos. No sabe de HTTP.
2. `HomeViewModel.kt` expone un `StateFlow<HomeUiState>`; la pantalla lo consume
   con `collectAsState()`.
3. `SyncPassRepository.kt` decide de dónde salen los datos y es el único que habla
   con Retrofit y Room.
4. El ViewModel se crea con `syncPassViewModel(repository) { XViewModel(it) }`
   (`ui/SyncPassViewModel.kt`): esa función es la inyección de dependencias del
   proyecto, hecha a mano, sin Hilt.

Los siete ViewModels: `Home`, `Entry`, `Login`, `Register`, `Scanner`, `Survey`,
`Bitacora`. **Ninguna pantalla llama ya al repositorio directamente.**

**Por qué el ViewModel y no hacerlo en la pantalla:** sobrevive al giro de
pantalla y a la recomposición; la corrutina vive en `viewModelScope` y se cancela
sola cuando la vista muere.

La única excepción deliberada: `ScannerScreen` conserva la cámara y los permisos,
porque son API del sistema operativo atadas al ciclo de vida del Composable. El
QR leído entra al ViewModel por `onQrLeido()`, igual que si se tecleara.

---

## 4. Si pregunta por **"View Controller"**

En Android con Jetpack Compose **no existe** el ViewController: ese patrón es de
iOS/UIKit y del Android antiguo con XML + Activity. Aquí el rol se reparte:

- `MainActivity.kt` es el único `Activity` y actúa como *host*.
- `SyncPassNavigation` + `NavHost` hacen de controlador de navegación; los
  destinos están declarados como tipos en `ui/navigation/Screen.kt`.
- La lógica de presentación está en el ViewModel, no en la Activity.

En el servidor, "controller" sí es literal: `@RestController` recibe la petición
HTTP, valida y responde JSON.

---

## 5. Si pregunta por **CSR**

La web es **CSR (Client Side Rendering)**: Vite entrega un `index.html` casi
vacío y el navegador arma la interfaz con JavaScript.

- `web-react/src/main.tsx` monta el árbol de React en el navegador.
- `App.jsx` resuelve las rutas en el cliente; no hay petición al servidor al
  cambiar de pantalla.
- Vercel sirve archivos estáticos; los datos llegan aparte por `fetch`.

**Por qué CSR y no SSR:** la app es para terreno. Una vez cargada funciona con lo
que tiene en IndexedDB aunque se corte la red; con SSR cada navegación exigiría
servidor. El costo es el primer arranque y el SEO, que aquí no importan.

---

## 6. CRUD: qué protege cada mantenedor

| Operación | Regla y dónde está |
|---|---|
| `GET /api/eventos` | Filtros opcionales `?comuna=&area=&proyectoId=&q=`, orden por fecha (`TallerRepository.buscar`) |
| `POST`/`PUT` taller | Geocerca todo o nada; bloqueo optimista con `@Version` |
| `PUT` taller | No deja menos cupos que personas ya acreditadas |
| `DELETE` taller | 409 si tiene asistencias: no se borra historial |
| `DELETE` proyecto | 409 si tiene talleres asociados |
| `POST` ingreso | Cupos con `SELECT ... FOR UPDATE`; idempotente por (taller, participante) |
| `PUT`/`DELETE` bitácora | Solo admin y coordinador; el resumen se recalcula al publicar |

**Corrección de rendimiento:** el listado de talleres hacía una consulta `COUNT`
por cada taller (N+1). Ahora `TallerService.buscar()` resuelve todos los
inscritos con **una** consulta agrupada.

Las reglas están cubiertas por `TallerCrudTest`; la suite completa son **22
pruebas** (`./mvnw test`).

---

## 7. Política de datos: local o nube

Quién decide: `ConnectivityMonitor.kt` (ConnectivityManager). Entrega `isOnline()`
para una consulta inmediata y `online: Flow<Boolean>` que avisa cuando cambia la red.

### Lectura

`SyncPassRepository.catalog()`:

1. Sin conexión → devuelve Room directamente, sin intentar la red.
2. Con conexión → pide a la API y **reescribe la copia local**.
3. Si la API falla aunque haya red → vuelve a Room.

Informa en `fromCache` de dónde salió el dato; la barra superior muestra
"API conectada", "Catálogo local" o "Esperando señal · N".

### Escritura: dos colas en Room

| Cola | Tabla | Qué guarda | Cómo se vacía |
|---|---|---|---|
| `PendingRecord` | `pending` | acreditaciones por QR y encuestas del equipo | `POST /api/sync/batch` |
| `PendingEntry` | `ingresos` | asistencia propia o de invitado al entrar | `POST /api/eventos/{id}/ingreso/...` |

El GPS se captura igual sin datos (el satélite no necesita internet), así que la
geocerca se valida después con la ubicación real del momento.

### Reconexión automática

`HomeViewModel.observarConexion()` escucha el flujo. Al volver la red: refresca el
catálogo → vacía la cola de ingresos → envía el lote del equipo → actualiza el
contador. El usuario no aprieta nada.

**Por qué reintentar no duplica:** `IngresoService` es idempotente por
(taller, participante) y responde `ya_presente`; `SyncService` usa el UUID que
generó el teléfono como clave primaria.

**Rechazo definitivo** (sin cupos, fuera del radio): se descarta la fila con su
motivo en vez de reintentar para siempre.

---

## 8. Lo que todavía se puede criticar (dilo tú primero)

1. **El backend expone la entidad JPA como JSON.** `Taller` es a la vez `@Entity`
   y cuerpo de la respuesta; lo correcto en un sistema grande es un DTO aparte
   para no atar el contrato HTTP al esquema de la base.
2. **No usamos Hilt ni Dagger.** La inyección es manual (`syncPassViewModel`).
   Es suficiente a esta escala y se explica en una línea.
3. **`EntryViewModel` usa `mutableStateOf` y los demás `StateFlow`.** Funciona
   igual; es una inconsistencia de estilo heredada.
4. **La web no es MVVM**: usa hooks y contexto de React, que es el patrón propio
   de esa plataforma.

---

## 9. Preguntas probables y respuesta de una línea

- *¿Dónde se guardan los datos?* Room/SQLite en el teléfono (`syncpass.db`),
  IndexedDB en el navegador, PostgreSQL en el servidor.
- *¿Qué es un DTO?* `TallerDto` en Android: viaja por la red y además es
  `@Entity` de Room para la copia local.
- *¿Por qué Retrofit?* Define la API como una interfaz Kotlin (`SyncPassApi`) con
  funciones `suspend`; evita escribir el cliente HTTP a mano.
- *¿Quién inyecta las dependencias?* Spring en el backend (constructor);
  `syncPassViewModel` en Android.
- *¿Cómo se autentica?* AWS Cognito. `AuthService` es el único que lo llama;
  `SecurityConfig` protege las rutas y los roles salen de `cognito:groups`.
- *¿Y si dos monitores acreditan a la vez?* `@Version` (bloqueo optimista) y
  `SELECT ... FOR UPDATE` al contar cupos.
- *¿Cómo pruebas el backend?* `./mvnw test`: 22 pruebas con H2 en memoria y
  Flyway aplicando las mismas 6 migraciones que producción.
