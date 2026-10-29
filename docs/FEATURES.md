# Funcionalidades y ramas feature

Una funcionalidad es una parte concreta del sitio. Una rama `feature/` permite trabajar esa parte antes de integrarla. No son lo mismo: tener el código en una carpeta no significa que ya tenga su rama o su commit.

El inicio y sus estilos ya están en main (`2aad236`); los formularios y validaciones siguen locales. Las imágenes, el logo y el video de muestra se retiraron; se agregarán recursos propios más adelante. Estas ramas son propuestas para revisar los cambios por partes; todavía no se han creado. No se subirá nada hasta que se indique.

## 1. Inicio y navegación

- Rama propuesta: `feature/inicio`.
- Archivos principales: `index.html` e imágenes de `img/`.
- Incluye menú, título, tres proyectos con nombre, descripción y precio, enlaces al registro y footer. El logo y las imágenes los agregará el equipo.
- Usa `header`, `nav`, `main`, `section`, `article` y `footer`.
- Revisar: cada enlace lleva al lugar correcto y las imágenes tienen texto alternativo.
- Para explicar: cada tarjeta es un `article` porque describe un proyecto independiente. El enlace «Me interesa» lleva al registro de muestra; no compra ni guarda una selección.

## 2. Formularios

- Rama propuesta: `feature/formularios`.
- Archivos: `login.html` y `register.html`.
- Login: correo y contraseña. Registro: nombre, correo, contraseña y confirmación.
- Revisar: labels conectados mediante `for` e `id`, campos obligatorios, sugerencias y `autocomplete` apropiado.
- Para explicar: el HTML define los campos; no crea una cuenta ni valida una identidad real.

## 3. Estilos con Bulma

- Rama propuesta: `feature/estilos`.
- Archivos: `css/estilos.css`, `css/vendor/` y clases de los HTML.
- Bulma se usa en `button is-link`, `input`, `label`, `field`, `card`, `card-content`, `box` y `notification`.
- El CSS propio define fuente, colores, ancho, distribución del catálogo y adaptación al móvil.
- Cada HTML enlaza un archivo propio, que importa Bulma local. No requiere internet.
- Revisar: entender qué hace cada clase usada y comprobar móvil y escritorio.
- Para explicar: la biblioteca da estilos preparados; nuestro CSS personaliza el sitio. No hace falta aprender ni usar toda la biblioteca.

## 4. Validaciones

- Rama propuesta: `feature/validaciones`.
- Archivo: `js/validaciones.js`.
- Se escucha `submit`, se frena el envío con `preventDefault()` y se revisan los campos con condiciones `if`.
- `mostrarError()` escribe el mensaje junto al campo. `textContent` coloca texto en la página.
- `esRegistro` permite usar el mismo archivo en los dos formularios.
- Revisar: todos los casos de `RUBRICA.md`, incluida confirmación distinta y espacios vacíos.
- Para explicar: `novalidate` permite mostrar los mensajes personalizados; las condiciones JavaScript siguen revisando los datos. `correo.validity.typeMismatch` aprovecha la comprobación de formato del navegador desde JS.
- No hay almacenamiento, peticiones ni backend. El resultado válido solo confirma el formato.

## 5. Video y cierre de la entrega

- Rama propuesta: `feature/video`.
- Archivos: bloque de video en `index.html` y `video/presentacion.webm`.
- Pendiente: el equipo elegirá su video. El video de muestra y su bloque HTML se retiraron.
- Revisar: reproducir y pausar el video sin internet.
- La documentación y el ZIP final se revisan después. No se prepara una entrega definitiva antes de revisar las funcionalidades.

## Orden para revisar e integrar

Revisar inicio, formularios, estilos, validaciones y video en ese orden. Para cada parte: comprender el código, probarla, ajustar lo necesario y registrar el avance real cuando se decida hacer commits. La integración entre ramas requiere un primer commit base y ramas de desarrollo existentes; no ejecutar comandos de `pull` sobre ramas que aún no existen.

No se añadirán carrito, filtros, pagos, panel de administración ni backend en esta evaluación sin un requisito nuevo. El alcance actual es una tienda de proyectos de demostración con tres páginas.
