# SyncPass · Guion de grabación

**Responsable:** Ethan Astorga. **Objetivo:** mostrar web, Android y el dato compartido en hasta 88 segundos. Abrir el [recorrido interactivo](RECORRIDO_DEMO.html) en el navegador. Los botones de cada flujo abren el archivo correspondiente. Las flechas izquierda y derecha cambian de capítulo. El formato toma como referencia el modo Story de [Archify](https://github.com/tt-a1i/archify).

## Antes de pulsar grabar

- Escoger un entorno para toda la toma. En nube, abrir [SyncPass Web](https://web-react-nu-nine.vercel.app/) e instalar el [APK publicado](https://web-react-nu-nine.vercel.app/descargas/SyncPass-Android.apk). En local, el navegador y el APK deben apuntar al backend de Docker. El APK local se compila con `-PsyncpassApiUrl=http://127.0.0.1:18080/` y el Samsung usa `adb reverse tcp:18080 tcp:18080`.
- Mantener abiertas dos sesiones de prueba. La primera administra desde web. La segunda tiene permiso para acreditar desde Android. Entrar a ambas **antes** de grabar.
- Preparar un taller ficticio, una bitácora publicada y dos códigos distintos. El código del taller abre su ficha. El QR del participante se escanea para acreditarlo. Para repetir la toma, utilizar una asistencia que todavía no exista; una asistencia duplicada puede confirmarse sin aumentar el quórum.
- Dejar listos el [visor de mockups](https://web-react-nu-nine.vercel.app/mockups.html), el [mantenedor](https://web-react-nu-nine.vercel.app/talleres), el [quórum](https://web-react-nu-nine.vercel.app/panel) y los informes web y Android. Apagar notificaciones del teléfono y cerrar pestañas con datos privados.
- Si el laboratorio se apaga, seguir el [runbook](OPERACION_LAB.md). Usar la variante local si la nube impide grabar. Identificar el entorno en el video.

## Toma principal

| Tiempo | Pantalla y acción | Narración breve |
| --- | --- | --- |
| 00:00–00:09 | Portada del recorrido y mockup de monitor | «SyncPass registra actividades en terreno, incluso cuando se corta Internet.» |
| 00:09–00:25 | Sesión web, Proyectos, Talleres e ID del taller | «React gestiona dos mantenedores. Web y Kotlin consultan la misma API en Gateway con Spring Boot en EC2.» |
| 00:25–00:46 | Android, cámara, modo avión y pendiente nuevo | «Compose delega al ViewModel y al repositorio; Room guarda la asistencia hasta que haya señal.» |
| 00:46–01:03 | Volver a conectar, ver confirmación y refrescar el mismo taller web | «Retrofit envía el pendiente. La API confirma su UUID y el repositorio retira de Room el registro aceptado.» |
| 01:03–01:15 | Pregunta del taller #2 y fuente | «El asistente devuelve 20 cuadernos como punto de equilibrio y cita el fragmento cuatro.» |
| 01:15–01:28 | Cobertura y firma | «Vitest mide 73,90 % de líneas web y JaCoCo 79,87 % de la lógica Android. El AAB está firmado.» |

Para grabar, deja las ventanas preparadas y corta por escenas. En Windows graba solo la ventana del navegador con el grabador de pantalla. En el Samsung activa No molestar y usa la grabación integrada o la captura por USB ya preparada. No grabes credenciales, microdatos de participantes ni menús de otras cuentas. Narra al final si así puedes concentrarte en los pasos.

Para la cámara, el QR del taller `SPT:<id>` abre su ficha. Demuestra la **acreditación** con el QR `SP1:<id>` de un participante ficticio desde una segunda pantalla. Si puedes ensayar con GPS, utiliza un taller de prueba con geocerca válida para tu posición. Si hay un pendiente anterior, anota el contador base y comprueba que aumente en uno. Tras reconectar, la reconciliación automática puede retirar el pendiente antes de pulsar el botón. Muestra la confirmación y el taller correspondiente.

La demostración de GPS se puede mostrar en el escaneo si se está físicamente en el radio del taller o si se prepara un escenario local adecuado. La transcripción Gemini requiere el JAR y la clave configurados. Mencionar solo los flujos observados en esa toma.

## Exportar y comprobar

Usar `SyncPassEthanAstorga001V.mp4`, H.264, 1280 × 720, máximo 90 segundos y 50 MB. La comprobación de formato distingue un video reproducible de una entrega que aún no se ha cargado.

```powershell
ffprobe -v error -select_streams v:0 -show_entries stream=codec_name,width,height -show_entries format=duration,size -of json SyncPassEthanAstorga001V.mp4
```

Para entregar, seguir [el procedimiento institucional en AzCopy](EVIDENCIA_ANDROID.md). La evidencia anterior de permisos permanece disponible como comprobante del intento anterior.
