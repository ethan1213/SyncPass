# SyncPass Móvil

Aplicación Android nativa en Kotlin y Jetpack Compose para el caso académico SyncPass.

## Compilar e instalar

Abre esta carpeta con Android Studio y usa un JDK compatible con Gradle 9.6 (el JBR incluido en Android Studio funciona). El wrapper está incluido. Por terminal en Windows:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```

El APK instalable queda en `app/build/outputs/apk/debug/app-debug.apk`. Está firmado con la clave de depuración local de Android; es apto para la demostración académica, no para distribuirlo en Play Store. Se necesita Android 8.0 o superior. El login usa el navegador del dispositivo para Cognito y vuelve a la app con `syncpass://auth/callback`.

## Demostración

1. Abre la app, inicia sesión o crea una cuenta en Cognito y verifica el correo. Un usuario de los grupos `admin`, `coordinador` o `monitor` puede acreditar; el grupo `participante` puede mostrar su QR y responder encuestas.
2. En Inicio aparece el catálogo descargado de la API; sin red se muestra la última copia guardada en Room. El QR personal contiene `SP1:` seguido del identificador anónimo de Cognito.
3. En un taller, el monitor abre **Acreditar QR**, concede permiso de cámara y lee el QR del participante. También puede introducir el código manualmente. La asistencia queda en Room aunque se corte la conexión.
4. Una encuesta se guarda en la misma cola local. Al recuperar la señal con la app abierta, la sincronización se intenta automáticamente; también puedes pulsar **Sincronizar ahora**. La app envía lotes a `POST /api/sync/batch` y elimina únicamente los registros aceptados. El quórum actualizado se consulta en la web.

La API configurada es `https://hluamat8vg.execute-api.us-east-1.amazonaws.com/` (API Gateway → EC2 → Supabase). La web está en `https://web-react-nu-nine.vercel.app`.

## Estado comprobado y límites

- Compilación `assembleDebug` y firma APK verificadas localmente el 23-10-2026.
- La API y la ruta web del QR respondieron HTTP 200. No se ejecutó una prueba de login/cámara en un teléfono o emulador porque no había uno conectado.
- La sincronización automática funciona al recuperar red con la app abierta; aún no hay `WorkManager` para ejecutarla si el proceso está cerrado. La pantalla principal usa ViewModel/StateFlow; escáner y encuesta todavía gestionan el estado de formulario en Compose. La app no implementa validación GPS. Por eso no se afirma cumplir todos los criterios del caso móvil.
- La sesión OAuth se mantiene en memoria y se pierde al cerrar el proceso; hay que iniciar sesión otra vez para sincronizar una cola pendiente. Los registros locales permanecen en Room.

Las pruebas académicas deben usar participantes ficticios o identificadores anónimos, sin datos personales reales.
