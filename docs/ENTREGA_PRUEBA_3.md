# SyncPass · Entrega de Prueba 3

**Responsable:** Ethan Astorga. **Revisión:** 27 de noviembre de 2026.

Esta matriz compara el proyecto con la pauta combinada de web y Android y con la pauta móvil. Las dos copias PDF recibidas son equivalentes, con SHA-256 `3b6a67a53ad52667d7feed8d7cc263c9a83c471f16fef3a2d2ee827c9b62cf59`. El Markdown móvil confirma los criterios de Android y la defensa. Las guías de la sesión privada están contrastadas en la [revisión del portal](REVISION_PORTAL_DOCENTE.md).

## Qué revisar y cómo demostrarlo

| Criterio de la pauta | Evidencia en SyncPass | Estado |
| --- | --- | --- |
| React y TypeScript | `web-react/src/App.tsx`, `src/types.ts` y compilación TypeScript antes de Vite. | Verificado en compilación y pruebas. |
| Un contexto, un hook, navegación y componentes | `SessionProvider.tsx` exporta el contexto y `useSession`. `hooks/useRoles.ts` deriva roles de la sesión. Wouter compone las páginas y `RequireAuth` limita el acceso. Ejemplo atómico: `components/atoms/Button.tsx`, `molecules/NavLinkItem.tsx` y `organisms/Navbar.tsx`. Los estilos combinan Tailwind con CSS propio. | Verificado por código y pruebas web. |
| Contratos HTTP tipados | `clients/api.ts` maneja transporte, código HTTP y errores. Login, registro y renovación tienen contratos explícitos en `auth/SessionProvider.tsx`; ciertos consumidores previos aún infieren el tipo de respuestas. | Tipado explícito demostrado en autenticación. |
| Mantenedores y acceso | Proyectos y talleres permiten crear, listar, editar y eliminar. Spring aplica permisos y detecta cambios concurrentes. | Probado con Vitest y MockMvc. Repetir la escritura con una sesión real en la nube. |
| Kotlin y MVVM | Las pantallas Compose observan estado de sus ViewModels. `SyncPassRepository.kt` coordina `SyncPassRemote.kt` (Retrofit) y `SyncPassLocal.kt` (Room y DAO). Cámara para QR y ubicación para geocerca. | Verificado por código y pruebas. Falta mostrar QR y GPS juntos en la versión de entrega. |
| Trabajo sin conexión y sincronización | Room guarda catálogo, asistencias pendientes y encuestas. Al reconectar o pulsar sincronizar se envían pendientes. Solo se eliminan de la cola los UUID que confirma el servidor. | Verificado en pruebas de Android y API. Falta el recorrido físico con dos cuentas. |
| Infraestructura | Vercel y Android consultan API Gateway. Spring Boot corre en EC2 y está preparado para PostgreSQL de Supabase. Cognito autentica a través de Spring. Docker Compose levanta web, API y base local. | Gateway respondió `UP` y la web responde en Vercel. El laboratorio aún sirve un JAR anterior. |
| Perfiles | Participante, coordinador, monitor y administrador. En la defensa, explicar sus responsabilidades respecto de estudiante, docente y voluntario. | Los permisos existen; los nombres difieren del ejemplo de la pauta y las cuentas de demostración necesitan comprobarse. |
| C4 y alcance | [ARQUITECTURA.md](ARQUITECTURA.md) contiene C1 a C4. [ALCANCE.md](ALCANCE.md) contiene RF, RNF, flujos y casos de uso. [IA_CONTEXTO.md](IA_CONTEXTO.md) documenta decisiones, contexto y límites. | Disponible en el repositorio integral y previsto para la copia documental. |
| Mockups Web y Android | [Inventario de mockups](MOCKUPS.md), [anexo de pantallas](MOCKUPS_ANEXO.md) y [visor interactivo](https://web-react-nu-nine.vercel.app/mockups.html). | Las vistas adicionales tienen wireframes de texto. El visor y las láminas SVG cubren las vistas principales. |
| Pruebas y cobertura ≥30 % | JUnit/MockMvc 32, Vitest 95 con 73,99 % de líneas, dos pruebas en Chromium y JUnit/Robolectric 101 con JaCoCo 79,82 % de líneas en el reporte filtrado. [Reproducir pruebas](VALIDACION_PRUEBA_3.md). | Suites aprobadas en la ejecución registrada. |
| Defensa 2 de 3 preguntas | Ensayar arquitectura, autenticación y sincronización con [defensa técnica](DEFENSA_ARQUITECTURA.md). | Preparación documentada. |

## Comparación con los ejemplos del docente

| Referencia revisada | Coincidencias y diferencias |
| --- | --- |
| [fs2-context-login](https://github.com/docentedev/fs2-context-login/tree/0fe7f923) y [fullstack-2-04-contextos](https://github.com/docentedev/fullstack-2-04-contextos/tree/f7dec403) | Ambos muestran contextos. SyncPass usa contexto de sesión, roles derivados y protección de rutas. Los recursos de dominio del proyecto son talleres y proyectos. No se incorporó un catálogo de productos al caso SyncPass. |
| [android-guia-final](https://github.com/docentedev/android-guia-final/tree/85e17fde) | El ejemplo combina Compose, MVVM y Retrofit con Hilt, Kotlin Serialization y DataStore para sesión. SyncPass usa Compose y Retrofit con Gson, construcción explícita del repositorio y sesión Android en memoria. La guía se registró como referencia técnica. Los archivos recibidos no fijan esas bibliotecas específicas. |
| [android-consumo-datos](https://github.com/docentedev/android-consumo-datos/blob/90d92302/docs/guia-room.md) | El ejemplo divide paquetes local, remoto, repositorio y mapeadores. SyncPass separa las fuentes en archivos dentro de `data` y mantiene su repositorio como punto de coordinación. El catálogo comparte su tipo entre Retrofit y Room. |
| [fs2-web](https://github.com/docentedev/fs2-web), [fs2-mobile](https://github.com/docentedev/fs2-mobile) y [fs2-api](https://github.com/docentedev/fs2-api/blob/5f2c666e/DESPLIEGUE_EC2.md) | El docente publica los tres componentes por separado. Su aplicación de referencia tiene reglas propias. SyncPass conserva su caso, API y despliegue, y prepara repositorios de revisión por componente. |
| [SyncPass del docente](https://github.com/docentedev/SyncPass/tree/25719832) | Tiene su propio contrato y capas de Android. La entrega de Ethan apunta al Gateway de su laboratorio. Se comparan estructura y responsabilidades sin incorporar el código ni los datos del ejemplo. |

## Recorrido para entregar

1. Abrir el [repositorio integral](https://github.com/ethan1213/SyncPass/tree/feature/bitacora-innovacion) y los anexos de web, Android, API y documentación.
2. Mostrar los mockups, luego el login, un mantenedor web, el mismo taller en Android y un envío de asistencia desde Room.
3. Publicar una bitácora de prueba desde una cuenta de gestión y abrirla desde otra cuenta. Mostrar la cita y probar una pregunta fuera del texto. Gemini es una ampliación; su prueba de transcripción en la nube requiere actualizar EC2 y configurar la clave en el servidor.
4. Abrir los reportes de cobertura y explicar cómo el backend verifica tanto la identidad del token como el rol. Los pasos y límites de la demo están en [VALIDACION_PRUEBA_3.md](VALIDACION_PRUEBA_3.md).
