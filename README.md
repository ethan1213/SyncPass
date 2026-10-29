# SyncPass

SyncPass registra talleres comunitarios, asistencia y encuestas para actividades de Vinculación con el Medio de Duoc UC. La solución tiene una web de administración y consulta, una app Android para trabajo en terreno, una API Java y almacenamiento local para conservar pendientes cuando se pierde la conexión.

## Enlaces de la demostración

- [Web publicada en Vercel](https://web-react-nu-nine.vercel.app/)
- [Estado de la API en API Gateway](https://hluamat8vg.execute-api.us-east-1.amazonaws.com/actuator/health)
- [Mockups y modelos en un solo archivo](SyncPass-Mockups-y-BD.html) y [visor de mockups](mockups.html)
- [Guion de exposición](docs/DEMO_VIERNES_30.md) y [requisitos](docs/REQUISITOS_SYNCPASS.md)

La web de Vercel puede abrir aunque el laboratorio esté detenido. Las operaciones que usan la API requieren que EC2 esté encendida; el [procedimiento de arranque](docs/OPERACION_LAB.md) explica cómo comprobarlo y obtener su IP.

## Cómo funciona

```text
Navegador (React/Vite) ── HTTPS ──┐
                                  ├── API Gateway (URL estable) ── HTTPS ── Caddy ── Spring Boot JAR (EC2)
Android (Kotlin/Compose) ─ HTTPS ─┘                                           ├── Cognito: identidad y grupos
                                                                             └── PostgreSQL/Supabase: datos compartidos
Navegador ── IndexedDB: catálogo, borradores y cola local
Android ── Room/SQLite: catálogo y registros pendientes
```

1. La web muestra el catálogo público desde `GET /api/eventos`. El login y el registro web envían las credenciales a la API (`/api/auth/*`); la API habla con Cognito. La web conserva la sesión en `sessionStorage` y adjunta el token a las operaciones protegidas.
2. Android usa el navegador del teléfono para el acceso OAuth con PKCE a Cognito y regresa a la app. Retrofit consume la misma API. Room conserva el catálogo y los pendientes del dispositivo.
3. Spring Security valida los JWT y los grupos de Cognito. Los controladores autorizan cada acción. JPA persiste proyectos y talleres; Flyway aplica las migraciones de PostgreSQL. La sincronización guarda asistencias y encuestas recibidas.
4. Vercel sirve el frontend. API Gateway ofrece una dirección HTTPS fija. Caddy termina TLS en EC2 y entrega la petición al JAR en `127.0.0.1:8080`. La instancia está en una VPC del laboratorio AWS Academy.

## Funciones y permisos

| Función | Web | Android | API y permiso |
| --- | --- | --- | --- |
| Catálogo de talleres | Inicio y detalle; copia local | Inicio; copia Room | `GET /api/eventos`, público |
| Registro, confirmación y login | `/registro`, `/login` | Acceso con Cognito | `/api/auth/*`; Cognito |
| Proyectos comunitarios | `/proyectos`: crear, listar, editar y eliminar | No es mantenedor móvil | `/api/proyectos`, `admin` o `coordinador` |
| Talleres | `/talleres`: CRUD y asociación a proyecto | Consulta de catálogo | `/api/eventos`; escritura `admin` o `coordinador` |
| Borradores de encuesta | `/borradores`: CRUD en IndexedDB | Encuestas y pendientes en Room | Se envían por `/api/sync/batch` al sincronizar |
| Acreditación | `/acreditar` | Cámara/QR y entrada manual | Asistencias por `/api/sync/batch`; personal autorizado |
| Quórum | `/panel` | Consulta según flujo móvil | `GET /api/quorum`; `admin`, `coordinador` o `monitor` |
| Perfil personal | `/mi-panel` y `/mi-qr` | Navegación por perfil | Grupos Cognito `admin`, `coordinador`, `monitor`, `participante` |

`admin` y `coordinador` administran proyectos y talleres. `monitor` acredita y consulta quórum. `participante` accede a su QR y a encuestas. La interfaz oculta acciones ajenas al perfil, y la API vuelve a comprobar el permiso; ocultar un botón por sí solo no protege los datos.

La cola local asigna registros al usuario actual. Al sincronizar, la API acepta lotes de hasta 100 registros, devuelve `acceptedIds` y el cliente elimina únicamente los aceptados. La creación de sesión necesita conexión. QR entre dispositivos y validación GPS completa se reservan para la siguiente evaluación; no se presentan como funciones verificadas de punta a punta.

## Tecnologías y directorios

| Ruta | Qué contiene | Herramientas principales |
| --- | --- | --- |
| `web-react/src/pages/` | Pantallas por ruta: inicio, login, perfiles, mantenedores, QR, encuestas y sincronización | React 18, JSX, Wouter |
| `web-react/src/components/` | Barra, pie y control de ruta protegida | React |
| `web-react/src/auth/`, `clients/`, `data/` | Sesión, cliente `fetch`, IndexedDB y QR | `idb`, `qrcode`, `@zxing/browser` |
| `web-react/src/index.css` | Paleta propia, componentes visuales y diseño adaptable | CSS |
| `backend/src/main/java/` | Controladores REST, autorización y entidades | Java 21, Spring Boot 3.5, Spring Security, JPA |
| `backend/src/main/resources/db/migration/` | Cambios versionados de tablas | Flyway, PostgreSQL |
| `backend/src/test/` | Pruebas de API y permisos | JUnit, Spring Boot Test, H2 |
| `mobile-kotlin/` | App Android, navegación, cámara, catálogo y base local | Kotlin, Compose, Retrofit, Room, CameraX, ML Kit |
| `database/` | Esquemas de referencia transaccional y analítico | SQL |
| `img/mockups/`, `mockups.html` | Bocetos por perfil y formato | SVG, HTML |
| `deploy/` | Instalación EC2, Caddy y recuperación de IP | Bash, systemd, AWS CLI |
| `docs/` | Caso, arquitectura, requisitos, operación y demostración | Markdown |

La web usa componentes `.jsx` y CSS propio. No hay componentes `.tsx` ni configuración de Tailwind en esta versión. Vite compila la web y Wouter resuelve las rutas. Tailwind y TypeScript pueden describirse como tecnologías del curso, pero no se deben atribuir a este código.

## Ejecutar en un equipo

**Web:** instalar Node.js, entrar a `web-react/`, copiar `.env.example` a `.env.local` y configurar `VITE_API_URL` con la API local o API Gateway. Luego ejecutar `npm ci` y `npm run dev`. Vite abre el servidor de desarrollo en el puerto configurado por `vite.config.js`. `npm run build` genera `web-react/dist/`.

**API:** instalar JDK 21. En `backend/`, configurar `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `COGNITO_ISSUER`, `COGNITO_CLIENT_ID` y `CORS_ORIGINS` fuera de Git. Ejecutar `./mvnw test` y `./mvnw package` (en Windows, `mvnw.cmd`). El JAR queda en `backend/target/`. Flyway aplica las migraciones al iniciar. Las variables requeridas se detallan en [el ejemplo de EC2](deploy/ec2-supabase.env.example).

**Android:** abrir `mobile-kotlin/` en Android Studio y ejecutar `:app:assembleDebug` o usar `gradlew.bat :app:assembleDebug`. El APK de depuración queda en `mobile-kotlin/app/build/outputs/apk/debug/`. Ver [instrucciones y límites móviles](mobile-kotlin/README.md).

**Contenedores:** `compose.yaml` contiene una opción local con base PostgreSQL. El despliegue de la exposición usa el JAR como servicio `systemd` en EC2 y Supabase como base compartida.

## Publicación, pruebas y colaboración

- La publicación web se hace en Vercel desde `web-react/`. La API está en EC2 tras API Gateway. Cuando el laboratorio se reinicia, [la recuperación de IP](docs/OPERACION_LAB.md) actualiza Caddy y Gateway automáticamente una vez instalado el servicio.
- La última comprobación local de esta versión pasó `npm run build` y 11 pruebas de backend (0 fallos). El reporte porcentual de cobertura no está configurado; no se declara un 80% alcanzado. La meta de Vitest 80% corresponde a una evaluación posterior.
- `main` contiene la versión integrada. Las ramas `developer/ethan`, `developer/jorge` y `feature/*` son puntos de partida para cambios revisados mediante pull request. Ver [convención de ramas](docs/RAMAS.md).
- No subir `.env`, contraseñas, claves de AWS, tokens ni APK de depuración. El repositorio contiene archivos de ejemplo sin secretos.

El caso y los modelos completos están en [la documentación](docs/README.md). Para presentar el proyecto, mostrar primero los mockups, luego la arquitectura y después las dos aplicaciones.
