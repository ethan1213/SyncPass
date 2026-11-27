# Revisión del portal del docente

Se revisaron las dos páginas de Publicaciones, Mis avisos y la sección de
documentos compartidos con la cuenta autenticada. El portal muestra guías
técnicas y material de otras asignaturas. En esas secciones no apareció una
nueva rúbrica de SyncPass ni una instrucción para sustituir la pauta recibida.
Mis avisos corresponde a avisos creados mediante la API del propio portal.

## Aplicación de las guías a SyncPass

| Material | Evidencia y decisión |
| --- | --- |
| [Testing Vitest React](https://app.claudioviajando.com/posts/testing-vitest-react) | SyncPass conserva la suite de Vitest, Testing Library y cobertura V8. Agrega Browser Mode con Chromium para probar formulario de login, contexto de sesión y ruta protegida. El test simula la respuesta HTTP y no usa cuentas reales. |
| [Guía Login Android](https://app.claudioviajando.com/posts/guia-login) | Compose presenta el estado del ViewModel y llama al repositorio. Las integraciones quedaron separadas en `SyncPassLocal.kt` y `SyncPassRemote.kt`. El ViewModel no accede directamente a Room ni a Retrofit. Hilt, Kotlin Serialization y DataStore forman parte del ejemplo del docente. SyncPass usa inyección explícita, Gson y sesión en memoria. |
| [01 Login](https://app.claudioviajando.com/posts/01-login) | `LoginScreen` captura los datos; `LoginViewModel` valida y gestiona el estado. `SyncPassApi` establece el contrato. El APK recibe la URL de la API mediante `BuildConfig`; la dirección pública no es una credencial. |
| [02 Home](https://app.claudioviajando.com/posts/02-home) | `AuthSession` conserva la sesión durante el uso de la app y el panel presenta el perfil. La autorización de cada operación se aplica en API Gateway y Spring. |
| [03 Android Kotlin y Room](https://app.claudioviajando.com/posts/android-kotlin-y-room) | La guía corresponde a otro proyecto base. SyncPass usa Room con entidades, DAO, migraciones y pruebas de la cola sin red. Conserva su SDK y librerías compatibles con la entrega. |

Las publicaciones de JavaFX, Kafka, LocalStack y las API de la plataforma son
referencias de otros ejemplos o asignaturas. Los límites de la entrega se
mantienen en la [matriz de Prueba 3](ENTREGA_PRUEBA_3.md).

## Mapa documental según la guía Android

| Tema | Dónde se comprueba |
| --- | --- |
| README y ejecución | [README del repositorio integral](../README.md) |
| Alcance y requisitos | [Alcance](ALCANCE.md) y [RF/RNF](REQUISITOS_SYNCPASS.md) |
| Reglas de negocio | [Caso](CASO.md) y [requisitos](REQUISITOS_SYNCPASS.md) |
| Casos de uso y flujos | [Casos y flujos](ALCANCE.md) |
| Arquitectura y decisiones | [C4](ARQUITECTURA.md) y [defensa](DEFENSA_ARQUITECTURA.md) |
| Flujo de datos | [Verificación web y Android](PLAN_VERIFICACION_COMUNICACION.md) |
| Base de datos y persistencia local | [Modelo](MODELO_BASE_DATOS.md) y `mobile-kotlin/app/src/main/java/cl/duoc/syncpass/data/SyncPassLocal.kt` |
| Contratos HTTP | [Postman](postman/SyncPass-EC2.postman_collection.json) y [diseño full stack](ARQUITECTURA_FULLSTACK_MOBILE.md) |
| Diseño de interfaz | [Mockups](MOCKUPS.md), [anexo](MOCKUPS_ANEXO.md) y [comportamiento responsive](RESPONSIVE.md) |

## Prueba de navegador

Dentro de `web-react`, instalar dependencias con `npm ci`, luego ejecutar:

```powershell
npx playwright install chromium
npm run test:browser
```

El test abre Chromium sin ventana. Una ruta de gestión bloquea a la persona
sin sesión. Con una respuesta de login ficticia, el formulario guarda la
sesión y el enlace abre el panel permitido para el rol coordinador. La
[verificación en nube](VALIDACION_PRUEBA_3.md) sigue siendo independiente.
