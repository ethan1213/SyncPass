# SyncPass · Guion de grabación

**Responsable:** Ethan Astorga. **Objetivo:** mostrar web, Android y el dato compartido en 86 segundos. Abrir el [recorrido interactivo](RECORRIDO_DEMO.html) en el navegador como apoyo de la grabación. La navegación por capítulos usa botones o las flechas izquierda y derecha. El formato de capítulos toma como referencia el modo Story de [Archify](https://github.com/tt-a1i/archify).

## Antes de pulsar grabar

- Escoger un entorno para toda la toma. En nube, abrir [SyncPass Web](https://web-react-nu-nine.vercel.app/) e instalar el [APK publicado](https://web-react-nu-nine.vercel.app/descargas/SyncPass-Android.apk). En local, el navegador y el APK deben apuntar al backend de Docker. El APK local se compila con `-PsyncpassApiUrl=http://127.0.0.1:18080/` y el Samsung usa `adb reverse tcp:18080 tcp:18080`.
- Mantener abiertas dos sesiones de prueba. La primera administra desde web. La segunda tiene permiso para acreditar desde Android. Entrar a ambas **antes** de grabar.
- Preparar un taller ficticio, una bitácora publicada y dos códigos distintos. El código del taller abre su ficha. El QR del participante se escanea para acreditarlo. Para repetir la toma, utilizar una asistencia que todavía no exista; una asistencia duplicada puede confirmarse sin aumentar el quórum.
- Dejar listos el [visor de mockups](https://web-react-nu-nine.vercel.app/mockups.html), el [mantenedor](https://web-react-nu-nine.vercel.app/talleres), el [quórum](https://web-react-nu-nine.vercel.app/panel) y los informes web y Android. Apagar notificaciones del teléfono y cerrar pestañas con datos privados.
- Si el laboratorio se apaga, seguir el [runbook](OPERACION_LAB.md). Usar la variante local si la nube impide grabar. Identificar el entorno en el video.

## Toma principal

| Tiempo | Pantalla y acción | Narración breve |
| --- | --- | --- |
| 00:00–00:10 | Portada del recorrido y mockup de monitor | «SyncPass registra actividades en terreno, incluso cuando se corta Internet.» |
| 00:10–00:26 | Abrir taller en web y mostrar el mismo ID en Android | «Los dos clientes consultan la misma API. La web gestiona el taller y Android consume ese catálogo.» |
| 00:26–00:47 | Desactivar la red. Escanear con Android y mostrar confirmación y contador de pendientes | «La cámara lee el QR. El repositorio guarda la asistencia en Room para enviarla después.» |
| 00:47–01:03 | Restablecer red, sincronizar y actualizar quórum web | «Retrofit envía el pendiente. El servidor confirma el identificador y el quórum refleja la asistencia.» |
| 01:03–01:18 | Preguntar en la bitácora y mostrar su cita | «El resumen y la respuesta salen de los apuntes publicados para este taller.» |
| 01:18–01:26 | Mostrar resultados de pruebas y cierre | «La arquitectura separa web, Android y API. El repositorio incluye pruebas reproducibles y cobertura.» |

La demostración de GPS se puede mostrar en el escaneo si se está físicamente en el radio del taller o si se prepara un escenario local adecuado. La transcripción Gemini requiere el JAR y la clave configurados. Mencionar solo los flujos observados en esa toma.

## Exportar y comprobar

Usar `SyncPassEthanAstorga001V.mp4`, H.264, 1280 × 720, máximo 90 segundos y 50 MB. La comprobación de formato distingue un video reproducible de una entrega que aún no se ha cargado.

```powershell
ffprobe -v error -select_streams v:0 -show_entries stream=codec_name,width,height -show_entries format=duration,size -of json SyncPassEthanAstorga001V.mp4
```

Para entregar, seguir [el procedimiento institucional en AzCopy](EVIDENCIA_ANDROID.md). La evidencia anterior de permisos permanece disponible como comprobante del intento anterior.
