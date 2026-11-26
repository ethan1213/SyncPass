# Innovación SyncPass: bitácora consultable del taller

## Problema y solución

Al terminar un taller, los apuntes quedan dispersos y las personas no pueden repasar fácilmente lo tratado. La bitácora vincula una transcripción o apuntes al **ID del taller**. Un administrador o coordinador publica el texto; la API genera un resumen extractivo y permite consultar preguntas mostrando el fragmento de origen. Web y Android leen el mismo registro en PostgreSQL a través de API Gateway y EC2.

La web permite grabar o seleccionar audio y solicitar transcripción a Gemini con autorización explícita. El resumen generado queda como borrador editable hasta que el equipo lo publica. El modo extractivo sigue disponible si falta el proveedor. Las preguntas en ambos clientes buscan términos en el texto publicado y muestran la cita original. Android consulta el material publicado por el equipo desde la web. Véase [IA_CONTEXTO.md](IA_CONTEXTO.md).

## Arquitectura y archivos

| Capa | Responsabilidad | Archivo |
| --- | --- | --- |
| Web | Edición por admin/coordinador, dictado o grabación local opcional con consentimiento, resumen y preguntas | `web-react/src/pages/BitacoraPage.tsx` |
| Navegación web | Enlace desde el detalle y ruta protegida | `web-react/src/pages/TallerDetailPage.tsx`, `web-react/src/App.tsx` |
| API | Permisos, validación, resumen y respuestas con cita | `backend/src/main/java/cl/duoc/syncpass/bitacora/BitacoraController.java` |
| Base | Una bitácora por taller, borrado en cascada con el taller | `backend/src/main/resources/db/migration/V4__bitacoras.sql` |
| Android | Consulta de resumen y preguntas con el mismo tallerId | `mobile-kotlin/app/src/main/java/cl/duoc/syncpass/ui/screens/BitacoraScreen.kt` |
| Red Android | DTO y Retrofit hacia API Gateway | `mobile-kotlin/app/src/main/java/cl/duoc/syncpass/data/SyncPassRepository.kt` |

Flujo: web `PUT /api/eventos/{id}/bitacora` → API Gateway → Spring Boot → PostgreSQL. Android `GET` y `POST /preguntas` al mismo ID. Ningún cliente accede directamente a la base. La bitácora requiere sesión: lectura y preguntas para los cuatro grupos; escritura solo para `admin` y `coordinador`.

## Contrato REST

| Método | Ruta | Entrada | Respuesta |
| --- | --- | --- | --- |
| `GET` | `/api/eventos/{id}/bitacora` | JWT | `contenido`, `resumen`, `actualizadoEn`, `fragmentos` |
| `PUT` | `/api/eventos/{id}/bitacora` | JWT admin/coordinador + `{ "contenido": "..." }` | Bitácora publicada |
| `POST` | `/api/eventos/{id}/bitacora/preguntas` | JWT + `{ "pregunta": "..." }` | `respuesta`, `fragmento`, `cita` |

El contenido admite hasta 20 000 caracteres; la pregunta, hasta 300. El resumen selecciona hasta tres fragmentos con vocabulario recurrente y mantiene su orden original. La búsqueda normaliza acentos y palabras frecuentes; devuelve el fragmento con más términos compartidos. Una pregunta sin coincidencias recibe una respuesta explícita y `fragmento: 0`.

## Demostración de punta a punta

1. Recuperar Gateway: `/actuator/health` debe responder 200. El 12-11-2026 devolvió 503; sin resolverlo no hay demo en nube.
2. Como administrador, crear un taller ficticio y anotar su ID. Abrir **Resumen y preguntas del taller** en su detalle.
3. Confirmar consentimiento antes de activar el micrófono. Grabar audio, detener y descargar el archivo local; dictar o pegar apuntes ficticios por separado. Guardar el texto y verificar resumen y `GET` en Postman. No usar una grabación real de personas sin autorización.
4. Consultar “¿Qué hacemos con los envases?” y comprobar que la cita coincide con un fragmento del texto. Preguntar por un tema ausente y comprobar el mensaje de insuficiencia.
5. Abrir el mismo taller en Android y entrar en **Resumen y preguntas del taller**. Comprobar el mismo resumen y respuesta. Editar la bitácora en web, reabrir Android y verificar el cambio.
6. Probar participante: lectura y preguntas 200, escritura 403. Sin sesión: 401. Registrar capturas y respuestas sin mostrar tokens.

## Evidencia y pendientes

En el entorno local, Flyway V4, la prueba de permisos/persistencia/respuesta en Spring y la compilación web y Android son las comprobaciones mínimas. Falta desplegar el JAR nuevo, aplicar la migración a Supabase y repetir la prueba cruzada con un teléfono. La bitácora requiere red en Android; no tiene caché Room propia. El dictado de Chrome debe probarse en el navegador y con permiso de micrófono.

Para una segunda versión con **audio compartido y transcripción automática**, diseñar consentimiento revocable, borrado y retención, cifrado/almacenamiento privado, tamaño máximo, costo de transcripción, permisos y revisión humana. Para respuestas generativas, exigir citas verificables y rechazo cuando la fuente no respalde la respuesta. No presentar esas capacidades como implementadas en este MVP.

La explicación del contexto, la fuente, los permisos y el uso de IA durante el desarrollo está en [`IA_CONTEXTO.md`](IA_CONTEXTO.md).
