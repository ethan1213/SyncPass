# Verificación de SyncPass — 17 de noviembre de 2026

Se ejecutaron las pruebas sobre `feature/bitacora-innovacion`, commit inicial `7d1f8ee`, sin modificar código de la aplicación.

| Componente | Comando | Resultado |
| --- | --- | --- |
| Web (React, Vite, Vitest y React Testing Library) | `cd web-react; npm test -- --reporter=dot` | 8 archivos, 37 pruebas aprobadas |
| Web: compilación de producción | `cd web-react; npm run build` | Aprobada; Vite transformó 350 módulos |
| Web: cobertura | `cd web-react; npm run coverage -- --reporter=dot` | 90,06 % líneas, 85,51 % ramas, 82,53 % funciones en los módulos incluidos |
| API (Spring Boot, JUnit, MockMvc, H2 y Flyway) | `cd backend; .\mvnw.cmd -q test` con JDK 21 | 22 pruebas aprobadas, 0 fallos, 0 errores; seis migraciones aplicadas en H2 |
| Android (Kotlin/JUnit/Robolectric) | `cd mobile-kotlin; .\gradlew.bat :app:testDebugUnitTest :app:createDebugUnitTestCoverageReport` con JDK 21 y `ANDROID_HOME` | 4 pruebas aprobadas, 0 fallos |

## Alcance de los resultados

- El reporte Vitest instrumenta diez archivos seleccionados en `web-react/vitest.config.ts`. El 90,06 % **no es la cobertura de todo `web-react/src`**. Las pruebas incluyen clientes HTTP, sesión, roles, cola IndexedDB, QR, geocerca y lógica de los dos mantenedores.
- Las pruebas backend usan H2 y credenciales simuladas. Comprueban controladores y reglas REST, pero **no demuestran por sí solas** el login real con Cognito, PostgreSQL, API Gateway ni un despliegue EC2.
- El reporte Android de AGP mide **16/1.979 líneas = 0,81 % global**. Las cuatro pruebas pasan, pero la meta de 30 % de cobertura Android de la pauta **sigue incumplida**. Deben añadirse pruebas significativas de ViewModels y repositorio y repetir este reporte; no basta con mostrar cuatro pruebas verdes.
- La compilación web muestra una advertencia de bundle de 684,64 kB superior al umbral informativo de Vite (500 kB). La compilación concluyó correctamente; se puede revisar división de código si la pauta exige optimización de tamaño.
- No se hizo una prueba física web ↔ API ↔ teléfono en esta ejecución: ADB no mostró dispositivos y Docker Desktop no inició su daemon. Por eso no se declara verificada la sincronización en vivo ni los CRUD con una sesión real de Cognito.

## Comparación con los repositorios recientes del docente

- [`fs2-context-login`](https://github.com/docentedev/fs2-context-login) muestra `ProfileContext`, un cliente de login y rutas con Wouter. SyncPass ya tiene los equivalentes `SessionProvider`, `clients/api.ts` y `App.jsx`, con autenticación de la API en vez del servidor de demostración con credenciales fijas del ejemplo.
- [`fullstack-2-04-contextos`](https://github.com/docentedev/fullstack-2-04-contextos) ilustra contexto React para compartir una lista obtenida por `fetch`. SyncPass centraliza la sesión con Context y el catálogo en `App.jsx`; sus mantenedores consumen la API mediante el cliente común.
- [`android-consumo-datos`](https://github.com/docentedev/android-consumo-datos) y [`room-kotlin`](https://github.com/docentedev/room-kotlin) sirven para explicar las fuentes remota y local. SyncPass usa Retrofit y Room en `SyncPassRepository`, con pantallas Compose y ViewModels separados.

No se copió el login de demostración del docente: acepta una cuenta fija y no comprueba Cognito. Tampoco se añadió Kafka o DynamoDB del repositorio `ec2-stack`, pues no forman parte del flujo ni de la pauta actual de SyncPass.

## Evidencia generada

- Frontend: `web-react/coverage/` (ignorado por Git).
- Backend: `backend/target/surefire-reports/` (ignorado por Git).
- Android: `mobile-kotlin/app/build/test-results/testDebugUnitTest/` y `mobile-kotlin/app/build/reports/coverage/test/debug/report.xml` (ignorados por Git).

**Estado de entrega:** pruebas ejecutadas satisfactoriamente en los tres componentes; quedan pendientes la cobertura Android requerida y la verificación integrada con Docker, Cognito y un teléfono disponible.
