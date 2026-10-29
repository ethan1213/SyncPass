# Guion de exposición · SyncPass · viernes 30 de octubre de 2026

## Orden de la demostración

1. **Introducción (40 s).** “En los talleres comunitarios se registra asistencia en papel. Esto produce filas, errores y encuestas tardías. SyncPass digitaliza la acreditación y conserva registros locales cuando falta señal.”
2. **Mockups antes de la aplicación (90 s).** Abrir [`../mockups.html`](../mockups.html). Mostrar Inicio, Login, Mantenedor de proyectos, Mantenedor de talleres, Borradores locales y la vista móvil. Explicar qué acción realiza cada pantalla. Los mockups de QR/GPS describen la meta de la tercera evaluación.
3. **Arquitectura (60 s).** React/Vite/JSX/Wouter en Vercel → API Gateway → Caddy y JAR Spring Boot en EC2 → Cognito para usuarios y Supabase PostgreSQL para datos. Android Kotlin consume Gateway, autentica con Cognito y usa Room local. La web usa IndexedDB.
4. **Login y dos mantenedores (2 min).** Iniciar sesión como admin/coordinador. En `/proyectos`, crear, listar, editar y eliminar un proyecto ficticio. En `/talleres`, hacer lo mismo con un taller y asociarlo a un proyecto. Mostrar que el monitor no puede entrar a estas rutas. No borrar registros que se necesiten para el resto de la exposición.
5. **CRUD local (45 s).** En `/borradores`, crear una encuesta, editarla, recargar para demostrar persistencia y eliminarla. Si se muestra modo sin conexión, preparar el taller antes de desconectarse.
6. **Móvil (60 s).** Mostrar APK Kotlin instalado, catálogo y pantalla de login. Mostrar escáner, encuesta y pendientes locales como funciones del caso; declarar exactamente cuáles se probaron en el teléfono.
7. **Pruebas y cierre (45 s).** Ejecutar `npm run build` y `./mvnw test` con resultado visible. Mostrar el reporte de cobertura **solo si existe** y citar el porcentaje real. Vitest 80% corresponde a la próxima evaluación.

## Qué abrir antes de comenzar

| Recurso | Ubicación |
| --- | --- |
| Mockups | `mockups.html` local |
| Web publicada | `https://web-react-nu-nine.vercel.app/` |
| API saludable | `https://hluamat8vg.execute-api.us-east-1.amazonaws.com/actuator/health` |
| Mantenedor 1 | `/proyectos` tras login |
| Mantenedor 2 | `/talleres` tras login |
| CRUD local | `/borradores` tras login |
| Pruebas API | `backend/target/surefire-reports/` después de Maven |

## Preguntas previsibles

- **¿Dónde está el frontend?** En Vercel; React compila con Vite y Wouter resuelve las páginas.
- **¿Dónde está el backend?** Un JAR Java 21 en EC2 detrás de API Gateway y Caddy.
- **¿Dónde están los datos?** PostgreSQL en Supabase; los pendientes locales viven en IndexedDB o Room.
- **¿Qué librerías usa?** React, Wouter, Vite, `idb`, Spring Boot, JPA, Flyway, Jetpack Compose, Retrofit, Room y CameraX/ML Kit.
- **¿Cómo se ejecuta?** `npm install && npm run dev` en `web-react/`; `./mvnw test && ./mvnw package` en `backend/`; Android Studio compila `mobile-kotlin/`.
- **¿Cómo se usó Tailwind?** No se usó. La interfaz actual tiene CSS propio. Tailwind permite aplicar utilidades CSS desde clases y se vio en el curso; no conviene atribuirlo a este código.
- **¿Es TypeScript?** No; los componentes son `.jsx`. Se puede explicar su estructura por componentes, páginas y rutas sin llamarla `.tsx`.
- **¿Cobertura 80%?** Meta de Vitest para la próxima evaluación. Indicar el porcentaje real del reporte si ya se generó; no inventarlo.

## Comprobación el mismo viernes

Iniciar el laboratorio AWS Academy, confirmar que API Gateway responda `UP` y revisar si cambió la IP pública. La EC2 del laboratorio no permanece encendida después de detenerlo. Llevar el APK ya instalado y un teléfono con depuración USB para pruebas adicionales.
