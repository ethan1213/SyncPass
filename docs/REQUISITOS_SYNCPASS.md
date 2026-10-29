# Requisitos de SyncPass para la demostración

Fuente: caso SyncPass de Duoc UC y pauta compartida por el equipo el 27 de octubre de 2026. Esta lista distingue lo implementado de la meta de la tercera evaluación.

## Funcionales

| ID | Requisito | Implementación y prueba de aceptación |
| --- | --- | --- |
| RF01 | Registro e inicio de sesión | Web y móvil consumen `/api/auth/*` por Gateway; EC2 se comunica con Cognito. Probar con cuenta de demostración, sin compartir contraseñas. |
| RF02 | Cuatro perfiles | Grupos Cognito `admin`, `coordinador`, `monitor`, `participante`; el menú y las rutas cambian según el token. |
| RF03 | Mantenedor de proyectos | `/proyectos` y `/api/proyectos`: crear, listar, consultar, actualizar, eliminar. Solo admin/coordinador. |
| RF04 | Mantenedor de talleres | `/talleres` y `/api/eventos`: crear, listar, consultar, actualizar, eliminar. Asociación opcional con un proyecto. Solo admin/coordinador. |
| RF05 | Persistencia local web | `/borradores`: crear, leer, editar y eliminar encuestas en IndexedDB; al enviar, pasan a la cola local. |
| RF06 | Catálogo móvil | Android descarga talleres de API Gateway y conserva copia en Room/SQLite. |
| RF07 | Sincronización | La web y Android envían registros pendientes por lotes a `/api/sync/batch`. |
| RF08 | Acreditación y encuesta | QR generado y leído en web/Android; encuesta local y sincronización. Validación de geocerca cuando el taller tiene coordenadas y radio. Falta prueba física entre dispositivos con la versión actual. |
| RF09 | Quórum | Personal autorizado consulta el panel de asistencias y la API `/api/quorum`. |

## No funcionales

| ID | Requisito | Evidencia o límite |
| --- | --- | --- |
| RNF01 | Diseño adaptable | CSS con puntos de corte para escritorio, tablet y teléfono; mockups en tres formatos. |
| RNF02 | Interfaz clara y propia | Paleta territorial verde/arcilla, tipografía de titulares y lenguaje del caso; sin el antiguo catálogo de ventas. |
| RNF03 | Control de acceso | JWT de Cognito; control de rutas en React y autorización `@PreAuthorize` en Java. |
| RNF04 | Transporte seguro | Vercel y API Gateway exponen HTTPS; Caddy protege la entrada a la EC2. |
| RNF05 | Disponibilidad con señal intermitente | IndexedDB en web y Room en Android conservan datos locales. El login nuevo requiere conexión. |
| RNF06 | Datos y cambios trazables | PostgreSQL en Supabase, migraciones Flyway, versión optimista de proyectos y talleres. |
| RNF07 | Pruebas repetibles | Maven ejecuta pruebas de API y geocerca. Vitest reporta 100% sobre cuatro módulos (cliente HTTP, IndexedDB, parser QR y aviso de geocerca), no sobre toda la web. El 80% global aún no está demostrado. |
| RNF08 | Mantenibilidad | Componentes compartidos, páginas por ruta Wouter, cliente HTTP separado y README con comandos de ejecución. |

## Fuera del alcance del viernes 30

La validación GPS y QR entre dispositivos está implementada en código para adelantar la tercera evaluación. Se muestra como función terminada solo después de repetirla con dispositivos reales y la API desplegada. La disponibilidad de la API depende del laboratorio AWS Academy.
