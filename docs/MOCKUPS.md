# Mockups y Wireframes de la Aplicación SyncPass (Web y Mobile Kotlin)

**Asignatura:** DSY1105 (Desarrollo de Aplicaciones Móviles) / DSY1104 (Full Stack)
**Institución:** Duoc UC · CITT Plaza Vespucio
**Caso Oficial:** SyncPass (TI CONSULTING DIEGO GARCÉS MIRANDA E.I.R.L.)
**Entorno Web:** Portal de Gestión y Acreditación Docente (React / Vercel)
**Entorno Mobile:** App Nativa en Terreno (Android Jetpack Compose con Modo Offline-First)

---

## Índice de Pantallas Diseñadas

1. [Vista 1: Home / Panel de Actividades Comunitarias](#vista-1-home--panel-de-actividades-comunitarias)
2. [Vista 2: Iniciar Sesión (Login Monitores y Docentes)](#vista-2-iniciar-sesión-login-monitores-y-docentes)
3. [Vista 3: Escáner QR de Acreditación Rápida en Terreno](#vista-3-escáner-qr-de-acreditación-rápida-en-terreno)
4. [Vista 4: Encuesta Rápida de Satisfacción (1 a 5 Estrellas)](#vista-4-encuesta-rápida-de-satisfacción-1-a-5-estrellas)
5. [Vista 5: Detalle del Taller y Lista de Asistentes en Vivo](#vista-5-detalle-del-taller-y-lista-de-asistentes-en-vivo)
6. [Vista 6: Centro de Sincronización Offline-First (Batch Sync)](#vista-6-centro-de-sincronización-offline-first-batch-sync)

---

<!-- dibujos -->
## Dibujos de las vistas (SVG)

Los wireframes están dibujados en estilo draw.io, con los elementos numerados y
una columna de referencias al costado. Se generan con `python tools/generar_mockups.py`
y hay una lámina por vista y por formato:

| Vista | Escritorio | Tablet | Móvil (Kotlin) |
|---|---|---|---|
| 1. Panel de talleres | `img/mockups/panel-web.svg` | `img/mockups/panel-tablet.svg` | `img/mockups/panel-movil.svg` |
| 2. Inicio de sesión (Cognito) | `img/mockups/login-web.svg` | `img/mockups/login-tablet.svg` | `img/mockups/login-movil.svg` |
| 3. Acreditación por QR | `img/mockups/escaner-web.svg` | `img/mockups/escaner-tablet.svg` | `img/mockups/escaner-movil.svg` |
| 4. Encuesta de satisfacción | `img/mockups/encuesta-web.svg` | `img/mockups/encuesta-tablet.svg` | `img/mockups/encuesta-movil.svg` |
| 5. Quórum en vivo | `img/mockups/quorum-web.svg` | `img/mockups/quorum-tablet.svg` | `img/mockups/quorum-movil.svg` |
| 6. Centro de sincronización | `img/mockups/sync-web.svg` | `img/mockups/sync-tablet.svg` | `img/mockups/sync-movil.svg` |

Para verlos todos juntos: `mockups.html` en el navegador, o el documento
`SyncPass-Mockups-y-BD.html`, que los lleva incrustados y se puede enviar solo.

El visor incluye además el [mantenedor de proyectos](../img/mockups/crud-proyectos-web.svg),
el de talleres, los borradores locales y las vistas por perfil. En la exposición,
mostrar primero estos dibujos y después abrir la aplicación. El login web real
envía credenciales a la API en EC2, que consulta Cognito; cualquier referencia
antigua a Auth0 en dibujos previos se considera obsoleta. GPS y el recorrido QR
entre dispositivos pertenecen a la tercera evaluación.
<!-- /dibujos -->


## Vista 1: Home / Panel de Actividades Comunitarias

### 1.1 Boceto Web (Desktop - Portal Coordinador Docente)
```text
+--------------------------------------------------------------------------------+
|  [LOGO SYNCPASS]                       [Talleres]  [Asistencia]  [Sync] [Prof. Melo]|
+--------------------------------------------------------------------------------+
|  PANEL DE CONTROL TERRITORIAL | Sede CITT Plaza Vespucio    [🟢 Conectado API] |
|                                                                                |
|  METRICAS CLAVE DEL SEMESTRE:                                                  |
|  +--------------------+  +--------------------+  +---------------------------+ |
|  | 350 ASISTENTES     |  | 3.2s PROMEDIO QR   |  | 78% RESPUESTA ENCUESTAS   | |
|  | 100% Cero Papel    |  | Meta: < 5 segundos |  | Meta histórica: > 65%     | |
|  +--------------------+  +--------------------+  +---------------------------+ |
|                                                                                |
|  TALLERES EN CURSO Y PROGRAMADOS:                                              |
|  +--------------------------------------------------------------------------+  |
|  | [TALLER 1] Alfabetización Digital para Personas Mayores                  |  |
|  | Lugar: UV N° 12 Los Castaños (La Florida) · Fecha: 18 Octubre 10:00      |  |
|  | Quórum: [||||||||||||||||||.....] 24/35 inscritos (68%)                  |  |
|  | [ Acreditar con QR ]  [ Ver Asistentes ]  [ Descargar Reporte VCM ]      |  |
|  +--------------------------------------------------------------------------+  |
|  | [TALLER 2] Taller de Emprendimiento y Comercio Digital                   |  |
|  | Lugar: Sede Villa Cordillera (Puente Alto) · Fecha: 20 Octubre 15:30     |  |
|  | Quórum: [||||||||||||............] 15/25 inscritos (60%)                  |  |
|  | [ Acreditar con QR ]  [ Ver Asistentes ]  [ Descargar Reporte VCM ]      |  |
|  +--------------------------------------------------------------------------+  |
+--------------------------------------------------------------------------------+
```

### 1.2 Boceto Mobile (Android Kotlin - Monitor en Terreno)
```text
+-----------------------------+
| 09:41              5G [98%] |
| = SyncPass CITT     [🔴 OFFLINE]| <- Badge indicador de red (Modo Terreno)
+-----------------------------+
| Hola, Monitor Voluntario 01 |
| Sede asignada: UV Los Castaños
|                             |
| TALLER DE HOY:              |
| +-------------------------+ |
| | Alfabetización Digital  | |
| | Personas Mayores        | |
| | 10:00 - 12:00 hrs       | |
| | Asistentes hoy: 24 / 35 | |
| |                         | |
| | +---------------------+ | |
| | | [📷 ESCANEAR QR]    | | | <- Botón grande (área táctil >48dp)
| | +---------------------+ | |
| +-------------------------+ |
|                             |
| ACCIONES RÁPIDAS:           |
| [⭐ Aplicar Encuesta Rápida]|
| [📦 Ver Pendientes Sync (3)]|
|                             |
+-----------------------------+
| [Inicio]  [Escanear]  [Sync]| <- BottomNavigationBar
+-----------------------------+
```

---

## Vista 2: Iniciar Sesión (Login Monitores y Docentes)

> **Autenticación implementada:** SyncPass presenta el formulario y envía las credenciales a la API, que usa Amazon Cognito. La sesión y el perfil determinan las rutas y acciones permitidas.

### 2.1 Boceto Mobile (Android Kotlin - Jetpack Compose)
```text
+-----------------------------+
| 09:41              5G [98%] |
+-----------------------------+
|                             |
|        [LOGO SYNCPASS]      |
|    Duoc UC · CITT Vespucio   |
|   Acreditación Comunitaria  |
|                             |
| Rol de Usuario:             |
| (•) Estudiante Monitor      |
| ( ) Docente Coordinador     |
|                             |
| Correo Institucional:       |
| [ monitor01@alumnos.duoc.cl ]
|                             |
| Contraseña:                 |
| [ ••••••••••••••        (O) ]
|                             |
| [x] Mantener sesión local   | <- Permite abrir app en terreno sin cobertura
|     (Habilitar Modo Offline)|
|                             |
| +-------------------------+ |
| |     INGRESAR A TERRENO  | |
| +-------------------------+ |
|                             |
| *Datos 100% ficticios (MVP) |
+-----------------------------+
```

---

## Vista 3: Escáner QR de Acreditación Rápida (< 5 segundos)

### 3.1 Boceto Mobile (Android Kotlin - Escaneo en Acceso)
```text
+-----------------------------+
| 09:41          [🔴 MODO OFFLINE]|
| <- [Volver]     Escaneo QR  |
+-----------------------------+
|                             |
|  +-----------------------+  |
|  |     [ ]       [ ]     |  | <- Visor Cámara (CameraX / MLKit)
|  |                       |  |
|  |       [===QR===]      |  |
|  |                       |  |
|  |     [ ]       [ ]     |  |
|  +-----------------------+  |
|  Apunta al código QR del    |
|  asistente para acreditar.   |
|                             |
|  ¡ACREDITACIÓN EXITOSA! ⚡   | <- Feedback inmediato en <3 segundos
|  +------------------------+ |
|  | Participante A (Vecino)| |
|  | Hora: 10:02:15 hrs     | |
|  | Estado: [GUARDADO EN   | |
|  |          SQLITE LOCAL] | |
|  +------------------------+ |
|                             |
|  [ Siguiente Acreditación ] |
+-----------------------------+
```

---

## Vista 4: Encuesta Rápida de Satisfacción (1 a 5 Estrellas)

### 4.1 Boceto Mobile (Android Kotlin - Al Cierre del Taller)
```text
+-----------------------------+
| 09:41                       |
| <- [Atrás]   Encuesta Rápida|
+-----------------------------+
| Taller: Alfabetización Digit.|
| Tu opinión nos ayuda a crecer
|                             |
| 1. ¿Cómo evalúas el taller? |
|     ★   ★   ★   ★   ★       | <- Estrellas táctiles grandes (48x48dp)
|    (1) (2) (3) (4) (5)      |
|                             |
| 2. ¿Qué tan útil fue hoy?   |
| [ Muy Útil ]  [ Útil ]      | <- Chips de selección rápida
| [ Regular ]   [ Poco Útil ] |
|                             |
| 3. Comentario breve:        |
| [ Aprendí a usar WhatsApp.. ]
|                             |
| +-------------------------+ |
| |   GUARDAR EVALUACIÓN    | |
| +-------------------------+ |
| *Se almacena localmente y se|
| sincroniza automáticamente. |
+-----------------------------+
```

---

## Vista 5: Detalle del Taller y Quórum en Vivo

Resuelve la dificultad declarada del perfil Docente / Coordinador(a) (§3.1):
*"carece de indicadores en tiempo real durante la ejecución del evento"*.

### 5.1 Boceto Web (Portal — Coordinador(a))

```text
+--------------------------------------------------------------------------------+
|  < Volver al panel de talleres                                                 |
+--------------------------------------------------------------------------------+
|  [ FOTO DEL TALLER ]   | TECNOLOGIA · LA FLORIDA                               |
|                        | Alfabetizacion Digital para Personas Mayores          |
|                        |                                                       |
|                        | Quorum inscrito: 24 / 35                      68.5 %  |
|                        | [############################--------------]           |
|                        |                                                       |
|                        | FECHA Y HORA        18 Octubre 2026 · 10:00 hrs        |
|                        | UNIDAD VECINAL      UV N 12 Los Castanos              |
|                        | RECINTO             Av. Santa Raquel 1450, La Florida |
|                        | COORDINADOR(A)      Prof. Nelson Melo                 |
|                        | AREA TEMATICA       Tecnologia                        |
|                        | CUPOS MAXIMOS       35                                |
|                        |                                                       |
|                        | [ ACREDITAR POR QR ]  [ APLICAR ENCUESTA ]            |
+--------------------------------------------------------------------------------+
|  REGISTRO DE ASISTENCIA        2 sincronizados · 1 pendiente (is_synced = 0)    |
+--------------------------------------------------------------------------------+
|  IDENTIFICADOR | PARTICIPANTE            | HORA     | ESTADO        | SYNC      |
|  PART-LF-001   | Vecino Participante A   | 10:02:15 | Acreditado QR | OK        |
|  PART-LF-002   | Vecina Participante B   | 10:03:02 | Acreditado QR | OK        |
|  PART-PA-003   | Estudiante Voluntario C | 10:04:18 | Acreditado QR | PENDIENTE |
+--------------------------------------------------------------------------------+
```

> En tablet (820 px) la ficha pasa a una sola columna bajo la foto y la tabla gana
> scroll horizontal propio, sin romper el ancho de la pagina.

### 5.2 Boceto Mobile (Android Kotlin — Monitor en terreno)

```text
+------------------------------+
|  < Alfabetizacion Digital    |
+------------------------------+
|  QUORUM EN VIVO              |
|        24 / 35               |
|  [#####################---]  |
|          68.5 %              |
|                              |
|  UV N 12 Los Castanos        |
|  La Florida · 10:00 hrs      |
+------------------------------+
|  ULTIMAS MARCACIONES         |
|  10:04:18  Voluntario C  (!) |
|  10:03:02  Participante B  OK|
|  10:02:15  Participante A  OK|
|                              |
|  (!) = pendiente de sync     |
+------------------------------+
|  [    ACREDITAR POR QR    ]  |  <- 48 dp de alto
|  [      ENCUESTA 1-5*     ]  |
+------------------------------+
```

---

## Vista 6: Centro de Sincronización Offline-First (Batch Sync)

```text
+--------------------------------------------------------------------------------+
|  CENTRO DE SINCRONIZACION OFFLINE-FIRST (Batch Sync)                           |
+--------------------------------------------------------------------------------+
|  ESTADO DE RED: 🟢 CONEXION RESTABLECIDA (Wi-Fi CITT Vespucio)                 |
|                                                                                |
|  REGISTROS ALMACENADOS EN EL DISPOSITIVO (Room / SQLite Local):                |
|  • 3 Marcaciones de asistencia pendientes de subida (is_synced = FALSE)        |
|  • 2 Encuestas de satisfacción pendientes de envío (is_synced = FALSE)         |
|                                                                                |
|  [ BOTON: SINCRONIZAR LOTE CON SERVIDOR CENTRAL (BATCH SYNC) ]                 |
|                                                                                |
|  RESULTADO: 5 registros transferidos con éxito. Timestamp actualizado.         |
|  Integridad garantizada al 100% sin pérdida de planillas en papel.             |
+--------------------------------------------------------------------------------+
```

---

## Comportamiento responsive de las vistas

Las seis vistas son **las mismas** en los tres formatos: lo que cambia es la
distribución, no el contenido. Esto es lo que el visor
[`mockups.html`](../mockups.html) permite alternar con los botones
💻 Escritorio / 📐 Tablet / 📱 Móvil.

| Vista | Escritorio (≥1024 px) | Tablet (600–1023 px) | Móvil (<600 px) |
| --- | --- | --- | --- |
| 1. Panel de talleres | 3 tarjetas por fila | 2 tarjetas por fila | 1 tarjeta por fila |
| 2. Login | Panel centrado de 480 px | Panel centrado | Panel a ancho completo |
| 3. Escáner QR | Visor + lista al costado | Visor sobre la lista | Visor a pantalla completa |
| 4. Encuesta | Estrellas en fila, panel centrado | Igual | Estrellas de 48 dp, botón fijo abajo |
| 5. Detalle y quórum | Foto + ficha en 2 columnas | Ficha bajo la foto | Todo apilado, tabla con scroll propio |
| 6. Sincronización | Panel de 640 px centrado | Igual | Estado de red apilado |

**Reglas comunes a los tres formatos:**

- Navegación: visible en línea sobre 900 px; bajo ese ancho se colapsa en menú
  hamburguesa con cada enlace de 48 px de alto.
- Áreas táctiles de 48×48 dp mínimo en todo control (caso §5.1).
- Sin desbordamiento horizontal a 360 px: las tablas tienen su propio scroll.
- Campos de formulario con tipografía de 16 px, para que iOS no haga zoom al enfocar.
- Tema claro por defecto en la web, igual que el sitio original; la variante
  oscura se activa con `data-theme="dark"`. En la app Android el modo claro y
  oscuro sigue el ajuste del sistema, como exige el caso §3.3.
- Sin emoji decorativos: los íconos que quedan tienen significado (la estrella de
  la encuesta, el visto de confirmación, la flecha de volver).

El detalle técnico de cada punto de corte está en [`RESPONSIVE.md`](RESPONSIVE.md).
