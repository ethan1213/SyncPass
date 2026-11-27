# Evidencia Android: entrega de SyncPass

Fuente: `prueba3Android.md`, manual de curricularización entregado y requisitos de video comunicados por el docente. Hay capturas locales; la carga institucional sigue pendiente de permisos.

## Qué demostrar

En un solo recorrido: web local y API sanas; login; dos mantenedores; consulta de taller en Android; QR o código; escritura en Room sin conexión; sincronización Retrofit al volver la red; dato visible en la web. Explicar Vista Compose → ViewModel → Repository → Retrofit/Room y el backend Controller → Service → Repository → PostgreSQL. Mostrar el reporte de cobertura Android con su porcentaje real, sin atribuir la cobertura parcial de Vitest a toda la solución.

## Archivo

Grabar MP4 de hasta 90 segundos, resolución 1280 × 720, peso máximo 50 MB y nombre simple sin espacios ni tildes, por ejemplo `SyncPassEthanAstorga.mp4`. Verificar duración, resolución y tamaño antes de entregar:

```powershell
ffprobe -v error -show_entries format=duration,size -show_entries stream=width,height -of default=noprint_wrappers=1 SyncPassEthanAstorga.mp4
```

## Guion de captura (máximo 90 segundos)

Para una toma nueva, usar el [guion actualizado de 86 segundos](GUION_VIDEO_PRUEBA_3.md) y abrir el [recorrido por capítulos](RECORRIDO_DEMO.html) en el navegador. El siguiente guion queda como referencia de las capturas anteriores.

| Tiempo | Pantalla real | Qué decir o mostrar |
| --- | --- | --- |
| 0–10 s | Mockups y portada | Problema: registrar talleres y asistencia cuando falla Internet. |
| 10–22 s | Docker, health y catálogo | React consulta la API Spring Boot; PostgreSQL conserva talleres compartidos. |
| 22–37 s | Proyectos y talleres | Mostrar dos mantenedores con lectura y una modificación real, usando cuenta autorizada. |
| 37–52 s | Android y QR | Kotlin/Compose abre taller por cámara; ViewModel llama a Repository. |
| 52–68 s | Android sin red | Room guarda asistencia pendiente; mostrar contador local. |
| 68–80 s | Reconexión y web | Sincronizar por Retrofit y mostrar el cambio recibido por API en la web. |
| 80–90 s | Arquitectura y cobertura | Señalar C1–C3 y el porcentaje JaCoCo real: 19-11-2026, 88 pruebas, 84,8 % líneas (`jacocoFilteredTestReport`, `SyncPassRepository` 96,8 %); no afirmar funciones que no se vieron. |

Grabar solo después de probar esos pasos. Si no se dispone del teléfono o sesión autorizada, dejar el archivo como pendiente y no enviar una demostración incompleta como si probara la integración móvil.

**Captura local del 15-11-2026:** `.private/evidencias/SyncPassEthanAstorgaEntrega.mp4` (excluida de Git) muestra el panel Android admin, catálogo conectado, resumen de una bitácora ficticia y pregunta con respuesta citada. `ffprobe` midió 70,79 s, 1280×720 y 212 478 bytes. Esta toma no demuestra aún los dos CRUD web, GPS, asistencia por QR ni una sincronización tras modo avión; esos pasos requieren una captura complementaria si el docente exige evidencia visual de cada uno. La entrega por AzCopy solo se marca realizada al recibir un resultado exitoso de la herramienta.

**Capturas del 21-11-2026:** `clip1_trimmed.mp4` (72,95 s) muestra login, catálogo y preinscripción. `clip2_trimmed.mp4` incluye un error HTTP 409 y termina antes de guardar la asistencia; no sirve como prueba de QR y sincronización terminados. No unirlo al video final como si el flujo hubiera concluido. Para una toma nueva de hasta 90 s: comenzar con la sesión ya iniciada, mostrar QR y taller correctos, guardar asistencia, mostrar la confirmación y volver al estado «asistió»; capturar la sincronización offline en una segunda evidencia si no cabe. Verificar duración, 1280×720 y tamaño máximo 50 MB con `ffprobe` antes de intentar AzCopy.

**Estado de entrega:** el 15-11-2026 se intentó `azcopy login` con la cuenta institucional. Ethan informó que Microsoft/Azure indicó falta de permisos y guardó una captura de esa pantalla. No se ejecutó una carga exitosa; el MP4 permanece local para enviarlo cuando el docente habilite el acceso. Adjuntar la captura al aviso al docente y conservar el archivo original sin modificar.

## Subida institucional

El manual exige AzCopy desde terminal y autenticación con correo `@duocuc.cl`. Cuenta de almacenamiento: `stduocvalhito2x95v26`; contenedor: `evidencias-estudiantes`. La sección móvil indicada por Ethan es `001V`, así que la ruta en minúsculas termina en `001v`. Confirmar con el docente el nombre exacto de la sede y asignatura antes de cargar; el ejemplo de ruta del manual es `plaza-vespucio/desarrollo-aplicaciones-moviles/001v`.

```powershell
azcopy login
azcopy copy 'SyncPassEthanAstorga.mp4' 'https://stduocvalhito2x95v26.blob.core.windows.net/evidencias-estudiantes/plaza-vespucio/desarrollo-aplicaciones-moviles/001v/SyncPassEthanAstorga.mp4'
```

Iniciar sesión en el navegador que abra AzCopy con la cuenta institucional propia. El permiso de estudiante es solo de carga: no se podrá listar ni descargar desde el contenedor para verificar. Guardar una captura de la terminal cuando AzCopy termine sin errores como comprobante. No subir desde el portal web de Azure.
