# Asistente contextual de talleres y uso de IA en ingeniería

## Qué existe en la aplicación

La innovación implementada es una **bitácora consultable por taller**. Un coordinador o administrador marca si la sesión se grabó y publica apuntes o una transcripción revisada. `PUT /api/eventos/{id}/bitacora` guarda el texto en PostgreSQL y calcula un resumen extractivo. `POST /api/eventos/{id}/bitacora/preguntas` busca el fragmento que comparte más términos con la pregunta y devuelve la respuesta junto con su cita. Web y Android consultan el mismo `tallerId` por la API. Una pregunta sin coincidencia recibe una respuesta de insuficiencia; el sistema no inventa contenido.

**Esta versión no llama a un modelo generativo.** El algoritmo de `BitacoraTexto.java` selecciona frases y normaliza términos; no comprende sinónimos ni transcribe grabaciones. En la web, el audio se puede grabar y descargar en el equipo, y el dictado usa la capacidad del navegador cuando está disponible. El audio no se carga al servidor. La etiqueta correcta para la defensa es *asistente contextual extractivo*, no chatbot generativo ni transcripción automática.

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

Una futura versión generativa necesitaría un proveedor configurado solo en el backend, consentimiento para enviar el contenido, límite de costo, validación de citas y rechazo de respuestas no respaldadas. No se declara implementada ni se requieren claves de GCP para ejecutar la versión actual.
