# SyncPass Móvil

Aplicación Android nativa en Kotlin y Jetpack Compose para el caso académico SyncPass.

## Compilar e instalar

Abre esta carpeta con Android Studio y usa un JDK compatible con Gradle 9.6 (el JBR incluido en Android Studio funciona). El wrapper está incluido. Por terminal en Windows:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```

El APK instalable queda en `app/build/outputs/apk/debug/app-debug.apk`. Está firmado con la clave de depuración local de Android; es apto para la demostración académica, no para distribuirlo en Play Store. Se necesita Android 8.0 o superior. Registro, confirmación por correo y login usan HTTPS hacia API Gateway; el backend en EC2 se comunica con Cognito.

Para probar contra Docker local, compila `.\gradlew.bat :app:assembleDebug -PsyncpassApiUrl=http://127.0.0.1:18080/`. En un teléfono USB autorizado ejecuta `adb reverse tcp:18080 tcp:18080` antes de abrir el APK; esta compilación debug usa HTTP únicamente para loopback. La compilación normal conserva API Gateway. Si usas emulador, pasa `-PsyncpassApiUrl=http://10.0.2.2:8080/`. Las pruebas locales y el reporte se generan con `.\gradlew.bat :app:testDebugUnitTest :app:createDebugUnitTestCoverageReport`; abre `app/build/reports/coverage/test/debug/index.html`.

## Firma de release

El módulo sigue la [guía de firma del profesor](https://app.claudioviajando.com/posts/guia-firmando-aab).

1. Usa el almacén JKS de subida existente en `app/keystore/upload-keystore.jks` y el alias `upload`. Conserva una copia privada de la clave y de sus credenciales para futuras versiones.
2. Gradle carga `storeFile`, `storePassword`, `keyAlias` y `keyPassword` desde `app/keystore/upload-keystore.properties`. La ruta `storeFile` es relativa a `app/`. La carpeta completa está excluida de Git.
3. Desde `mobile-kotlin/`, ejecuta `.\gradlew.bat :app:bundleRelease :app:assembleRelease`. El AAB para Play Console queda en `app/build/outputs/bundle/release/app-release.aab`. El APK de instalación directa queda en `app/build/outputs/apk/release/app-release.apk`.
4. Verifica el AAB con `jarsigner -verify app/build/outputs/bundle/release/app-release.aab`. Compara el SHA-256 de `keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab` con el del alias `upload` de `keytool -list -v`. Para el APK usa `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` desde las herramientas del SDK Android.

En Google Play, la clave de subida firma el AAB. Play App Signing firma los APK distribuidos por la tienda. El APK de instalación directa usa la clave configurada para release. Una instalación debug con el mismo identificador tiene otro certificado y Android rechazará la actualización entre ambas. Antes de retirar una instalación anterior, sincroniza sus registros pendientes de Room.

## Patrón de login del curso

Se adaptó el patrón del [ejemplo del profesor](https://github.com/docentedev/android-guia-final/blob/85e17fde91/GUIA_LOGIN.md) al contrato de SyncPass. `LoginScreen` observa `LoginUiState` mientras la pantalla está activa. `LoginViewModel` valida los campos, solicita el acceso al repositorio y emite un evento de éxito de un solo uso. Compose consume ese evento y navega a Inicio. `SyncPassRepository` utiliza Retrofit hacia API Gateway y la sesión se conserva en memoria.

## Demostración

Para ejecutar las pruebas Robolectric usa JDK 21. El JBR de Android Studio con Java 25 produjo `Unsupported class file major version 69` en este equipo.

1. Abre la app, inicia sesión o crea una cuenta en Cognito y verifica el correo. Un usuario de los grupos `admin`, `coordinador` o `monitor` puede acreditar; el grupo `participante` puede mostrar su QR y responder encuestas.
2. En Inicio aparece el catálogo descargado de la API; sin red se muestra la última copia guardada en Room. El QR personal contiene `SP1:` seguido del identificador anónimo de Cognito.
3. En un taller, el monitor abre **Acreditar QR**, concede permiso de cámara y lee el QR del participante. También puede introducir el código manualmente. La asistencia queda en Room aunque se corte la conexión.
4. Una encuesta se guarda en la misma cola local. Al recuperar la señal con la app abierta, la sincronización se intenta automáticamente; también puedes pulsar **Sincronizar ahora**. La app envía lotes a `POST /api/sync/batch` y elimina únicamente los registros aceptados. El quórum actualizado se consulta en la web.

La API configurada es `https://hluamat8vg.execute-api.us-east-1.amazonaws.com/` (API Gateway → EC2 → Supabase). La web está en `https://web-react-nu-nine.vercel.app`.

## Estado comprobado y límites

- Compilación `assembleDebug` y firma APK verificadas localmente el 29-10-2026 en el repositorio SyncPass.
- La API y la ruta web del QR respondieron HTTP 200. Hubo una comprobación manual del retorno de login en un teléfono durante el desarrollo; la lectura de cámara y la sincronización completa deben repetirse en la versión que se presente. El 29-10-2026 no había un dispositivo ADB conectado para repetir esa prueba.
- La sincronización automática funciona al recuperar red con la app abierta; aún no hay `WorkManager` para ejecutarla si el proceso está cerrado. La pantalla principal usa ViewModel/StateFlow; escáner y encuesta todavía gestionan el estado de formulario en Compose. Si el taller tiene geocerca, el escáner pide una ubicación reciente; la API verifica distancia y precisión cuando recibe el lote. Falta probar ese flujo completo en un teléfono con GPS.
- La sesión se mantiene en memoria y se pierde al cerrar el proceso; hay que iniciar sesión otra vez para sincronizar una cola pendiente. Los registros locales permanecen en Room.

Las pruebas académicas deben usar participantes ficticios o identificadores anónimos, sin datos personales reales.
