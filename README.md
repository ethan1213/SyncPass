# SyncPass

Aplicación académica de acreditación de talleres comunitarios para Duoc UC, CITT Plaza Vespucio. Sustituye las planillas de papel por un catálogo de talleres, registro de asistencia, encuestas breves y una cola local que se sincroniza al recuperar la conexión.

## Demostración del viernes 30 de octubre

1. Presentar el problema y abrir [`mockups.html`](mockups.html) **antes** de la aplicación. Mostrar la vista web, el móvil y los dos mantenedores.
2. Abrir la [web en Vercel](https://web-react-nu-nine.vercel.app/) e iniciar sesión.
3. Mostrar **Proyectos comunitarios** y **Talleres**: crear, listar, editar y eliminar. Ambos consumen la API Java en EC2 y requieren el grupo `admin` o `coordinador` de Cognito.
4. Mostrar el CRUD **local** de borradores de encuestas en IndexedDB, sin conexión.
5. Abrir la aplicación Android Kotlin y mostrar catálogo, login, escáner y cola local. La prueba de QR y GPS de punta a punta corresponde a la tercera evaluación.

Guion y comprobaciones: [`docs/DEMO_VIERNES_30.md`](docs/DEMO_VIERNES_30.md).

## Arquitectura implementada

```text
Web React (Vercel) ──HTTPS──► API Gateway ──HTTPS──► Caddy + JAR Spring Boot (EC2)
       │                                                 ├──► Cognito (usuarios y grupos)
       └──► IndexedDB (borradores y cola local)           └──► Supabase PostgreSQL

Android Kotlin ──HTTPS──► API Gateway (catálogo y sincronización)
       ├──► Cognito (login OAuth con PKCE)
       └──► Room / SQLite (catálogo y pendientes locales)
```

La URL de API Gateway permanece estable; la IP pública de EC2 puede cambiar al reiniciar el laboratorio. [`deploy/fix-ip.sh`](deploy/fix-ip.sh) ayuda a reapuntar la integración.

## Tecnologías y estructura

| Pieza | Tecnología real | Carpeta |
| --- | --- | --- |
| Web | React 18, Vite 6, JSX, Wouter, CSS propio, IndexedDB | `web-react/src/` |
| API | Java 21, Spring Boot 3.5, JPA, Flyway, Maven | `backend/` |
| Móvil | Kotlin, Jetpack Compose, Retrofit, Room, CameraX, ML Kit | `mobile-kotlin/` |
| Datos | PostgreSQL en Supabase y SQLite local | `backend/src/main/resources/db/migration/`, `mobile-kotlin/` |
| Mockups | SVG y visor HTML | `img/mockups/`, `mockups.html` |

`web-react/src/components/` contiene piezas compartidas; `web-react/src/pages/` contiene las pantallas; `web-react/src/index.css` define colores, diseño adaptable y estados. El proyecto actual **no utiliza Tailwind ni TypeScript**. Tailwind es un sistema de clases utilitarias para componer estilos; se explica como tecnología del curso, no como una implementación propia. Los archivos `.jsx` son los componentes reales; no hay pantallas `.tsx` que mostrar.

## Ejecución local

```bash
cd web-react
npm install
npm run dev
```

La web requiere `VITE_API_URL`; ver [`web-react/.env.example`](web-react/.env.example). Para probar la API:

```bash
cd backend
./mvnw test
./mvnw package
```

En Windows se usa `mvnw.cmd`. Las variables de base de datos y Cognito se configuran localmente según [`docs/DEMO_EC2_JAR_POSTMAN.md`](docs/DEMO_EC2_JAR_POSTMAN.md); no se guardan credenciales en Git. Para Android, abrir `mobile-kotlin/` en Android Studio y compilar `app-debug.apk`.

## Requisitos y pruebas

- Requisitos funcionales y no funcionales: [`docs/REQUISITOS_SYNCPASS.md`](docs/REQUISITOS_SYNCPASS.md).
- Mockups y explicación de cada vista: [`docs/MOCKUPS.md`](docs/MOCKUPS.md).
- Modelo relacional y copo de nieve: [`docs/MODELO_BASE_DATOS.md`](docs/MODELO_BASE_DATOS.md), [`docs/MODELO_ESTRELLA_COPO_NIEVE.md`](docs/MODELO_ESTRELLA_COPO_NIEVE.md).
- Pruebas automatizadas de la API: `backend/src/test/`. La cobertura porcentual se informa solo desde un reporte generado; la meta de **Vitest 80%** es para la próxima evaluación y aún no está implementada.

Proyecto académico con datos ficticios de demostración.

Colaboración y ramas: [`docs/RAMAS.md`](docs/RAMAS.md).
