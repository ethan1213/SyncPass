# Asistente contextual de talleres y uso de IA en ingeniería

## Qué existe en la aplicación

La innovación implementada es una **bitácora consultable por taller**. Un coordinador o administrador marca si la sesión se grabó y publica apuntes o una transcripción revisada. `PUT /api/eventos/{id}/bitacora` guarda el texto en PostgreSQL y calcula un resumen extractivo. `POST /api/eventos/{id}/bitacora/preguntas` busca el fragmento que comparte más términos con la pregunta y devuelve la respuesta junto con su cita. Web y Android consultan el mismo `tallerId` por la API. Una pregunta sin coincidencia recibe una respuesta de insuficiencia; el sistema no inventa contenido.

La edición web admite grabar o seleccionar audio y transcribirlo con Gemini. El administrador o coordinador autoriza el envío al proveedor y revisa el resultado antes de publicarlo. Puede generar y editar un borrador de resumen. Si falta la clave o falla el proveedor al resumir, el servidor prepara un borrador extractivo. Las preguntas de web y Android usan el texto publicado y conservan citas literales verificables.

| Operación bajo `/api/eventos/{id}/bitacora` | Resultado |
| --- | --- |
| `POST /transcripcion` | Recibe audio multipart de hasta 4 MB con autorización expresa. Devuelve texto editable y, si el proveedor falla, permite seguir con apuntes manuales. |
| `POST /borrador` | Devuelve un resumen y el origen `gemini` o `extractivo`. Crear un borrador no modifica PostgreSQL. |
| `PUT` | Publica el texto y el resumen revisado o calcula uno extractivo. |
| `GET` / `POST /preguntas` | Comparten el resultado y las citas entre navegador y Android. |

El backend es el único que llama al proveedor. En local se configura `GEMINI_API_KEY` en el `.env` privado para Docker Compose. En EC2 se configura en `/etc/syncpass/syncpass.env` y se reinicia `syncpass-api`. `GEMINI_MODEL` permite cambiar el modelo. Los audios pueden contener voces de terceros. Usa grabaciones autorizadas y cortas, y verifica las condiciones del proveedor antes de usar grabaciones reales. SyncPass no guarda el audio al publicar. [Referencia REST de Gemini](https://ai.google.dev/api/generate-content).

## Contexto, fuentes y límites

La unidad de contexto es el ID del taller. El servicio recupera solo la bitácora de ese ID, fragmenta su contenido, calcula el resumen y responde sobre esos fragmentos. No incorpora conversaciones de otros talleres, datos privados de participantes ni resultados abiertos de Internet. Los permisos de Spring Security exigen un grupo Cognito para leer y preguntar; solo administrador y coordinador publican. Android usa Retrofit y la web usa `clients/api.ts`; ambos reciben el mismo contrato JSON.

```text
Docente publica texto revisado → API → PostgreSQL
Pregunta con tallerId → API → fragmentos del mismo taller → respuesta + cita
Sin coincidencia → aviso explícito de información insuficiente
```

La cita permite comprobar el resultado contra la fuente. El resumen y la pregunta requieren red en Android porque la bitácora no tiene caché Room. La política de consentimiento de la interfaz se aplica antes de activar el micrófono; publicar apuntes es una acción separada.

## Cómo se usó IA durante la ingeniería

El contexto de trabajo para el asistente de desarrollo fue el caso SyncPass, la pauta Android, los requisitos funcionales y no funcionales, los contratos REST y el código existente. Se pidió revisar cada propuesta contra clases, rutas y pruebas antes de implementarla. Los resultados se verificaron con JUnit/MockMvc, Vitest, pruebas Android y respuestas HTTP. Las decisiones que el asistente propuso no sustituyen la revisión del estudiante: el código, las pruebas y la demostración física son la evidencia.

Ejemplo de instrucción de ingeniería reproducible:

> Revisa el flujo `BitacoraPage` → `/api/eventos/{id}/bitacora` → `BitacoraService` → PostgreSQL y el flujo Android `BitacoraScreen` → `BitacoraViewModel` → `SyncPassRepository`. Señala rutas o permisos que no coincidan con el caso, cita el archivo concreto y propone una prueba que falle antes de corregir. No atribuyas transcripción ni generación de texto a un módulo que solo hace extracción.

## Verificación para la defensa

1. Publicar apuntes ficticios como coordinador y comprobar `GET` del mismo taller.
2. Preguntar por un término presente y comparar `cita` con el texto guardado.
3. Preguntar por un tema ausente y mostrar la respuesta de insuficiencia.
4. Abrir el mismo taller en Android y comparar resumen y fuente.
5. Probar `401` sin token y `403` al intentar publicar con un rol sin permiso.

Para comprobar Gemini en vivo, usa un audio ficticio corto y confirma que el borrador indica origen `gemini`. Repite sin clave para comprobar el resumen extractivo. El material publicado y las preguntas siguen disponibles sin la clave.

### Bitácoras de demostración

`deploy/local-seed.sql` agrega datos ficticios a PostgreSQL local y conserva las publicaciones existentes al reejecutarse. La web local usa el mismo catálogo que un APK compilado para el entorno local.

| Taller | Pregunta de ensayo | Qué debe citar |
| --- | --- | --- |
| Huerto comunitario · Demo | ¿Cómo mezclaron hojas secas y restos vegetales? | Dos partes de hojas secas por una de restos vegetales. |
| Primeros pasos con planillas · Demo | ¿Cuál fue el saldo con 45000 pesos de ingresos? | 17000 pesos de saldo con gastos de 28000 pesos. |

Pregunta de control: «¿Qué se explicó de astronomía?». El mensaje debe indicar que los apuntes carecen de información suficiente. La suite `BitacoraGeminiTest` prueba el contrato del cliente generativo con HTTP simulado. La prueba con el proveedor real requiere acceso de red y una clave configurada en el backend.
