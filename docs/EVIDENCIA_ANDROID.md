# Evidencia Android: entrega de SyncPass

Fuente: `prueba3Android.md`, manual de curricularización entregado y requisitos de video comunicados por el docente. Este procedimiento no implica que la evidencia ya se haya grabado ni subido.

## Qué demostrar

En un solo recorrido: web local y API sanas; login; dos mantenedores; consulta de taller en Android; QR o código; escritura en Room sin conexión; sincronización Retrofit al volver la red; dato visible en la web. Explicar Vista Compose → ViewModel → Repository → Retrofit/Room y el backend Controller → Service → Repository → PostgreSQL. Mostrar el reporte de cobertura Android con su porcentaje real, sin atribuir la cobertura parcial de Vitest a toda la solución.

## Archivo

Grabar MP4 de hasta 90 segundos, resolución 1280 × 720, peso máximo 50 MB y nombre simple sin espacios ni tildes, por ejemplo `SyncPassEthanAstorga.mp4`. Verificar duración, resolución y tamaño antes de entregar:

```powershell
ffprobe -v error -show_entries format=duration,size -show_entries stream=width,height -of default=noprint_wrappers=1 SyncPassEthanAstorga.mp4
```

## Subida institucional

El manual exige AzCopy desde terminal y autenticación con correo `@duocuc.cl`. Cuenta de almacenamiento: `stduocvalhito2x95v26`; contenedor: `evidencias-estudiantes`. La sección móvil indicada por Ethan es `001V`, así que la ruta en minúsculas termina en `001v`. Confirmar con el docente el nombre exacto de la sede y asignatura antes de cargar; el ejemplo de ruta del manual es `plaza-vespucio/desarrollo-aplicaciones-moviles/001v`.

```powershell
azcopy login
azcopy copy 'SyncPassEthanAstorga.mp4' 'https://stduocvalhito2x95v26.blob.core.windows.net/evidencias-estudiantes/plaza-vespucio/desarrollo-aplicaciones-moviles/001v/SyncPassEthanAstorga.mp4'
```

Iniciar sesión en el navegador que abra AzCopy con la cuenta institucional propia. El permiso de estudiante es solo de carga: no se podrá listar ni descargar desde el contenedor para verificar. Guardar una captura de la terminal cuando AzCopy termine sin errores como comprobante. No subir desde el portal web de Azure.
