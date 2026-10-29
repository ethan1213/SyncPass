# Diseño responsive y accesibilidad

Cómo se adapta SyncPass a teléfonos, tablets y escritorio, en las tres capas del
proyecto. Es la evidencia del indicador **IE1.1.4** de la rúbrica
(*"explicar CSS personalizado, externo y mantenible + demostración responsive"*) y del
requisito del caso §3.3 (*"adaptabilidad a distintas pantallas y áreas táctiles mínimas
de 48×48 dp"*).

---

## 1. Puntos de corte

Un solo conjunto de puntos de corte para todo el proyecto, alineado con las clases de
tamaño de ventana de Material 3 (así la web y la app Android se comportan igual):

| Formato | Ancho | Material 3 | Columnas | Navegación |
| --- | --- | --- | --- | --- |
| Teléfono angosto | ≤ 379 px | compact | 1 | Menú hamburguesa |
| Teléfono | 380–599 px | compact | 1 | Menú hamburguesa |
| Tablet vertical | 600–899 px | medium | 2 | Menú hamburguesa |
| Tablet horizontal | 900–1023 px | medium | 2 | Barra en línea |
| Escritorio | 1024–1279 px | expanded | 3 | Barra en línea |
| Escritorio amplio | ≥ 1280 px | expanded | 3 | Barra en línea |

El corte de la navegación (**900 px**) es intencionalmente distinto del corte de las
grillas (**1024 px** y **600 px**): una tablet en horizontal tiene ancho de sobra para
la barra completa, pero no para tres columnas de tarjetas.

---

## 2. Aplicación web React — `web-react/src/index.css`

Hoja única, externa y organizada en siete secciones rotuladas (tokens, reset,
layout, cabecera, componentes, módulo EP1, capa responsive).

### Tokens, no colores sueltos

Todo el color sale de variables CSS en `:root`. No hay un solo `#hexadecimal` suelto en
los componentes, y por eso cambiar de tema es una sola redefinición de tokens:

```css
:root            { --surface: #ffffff;  --text: #334155; }
@media (prefers-color-scheme: dark) {
  :root:not([data-theme='light']) { --surface: #141d31; --text: #d6ddeb; }
}
:root[data-theme='dark']          { --surface: #141d31; --text: #d6ddeb; }
```

**La web usa el tema claro por defecto**, igual que el sitio original: su
`index.html` lleva `<html lang="es" data-theme="light">`. Esa marca es justamente lo
que neutraliza la regla de `prefers-color-scheme`.

Para ver la variante oscura hay dos formas:

- cambiar el atributo a `data-theme="dark"` (lo que se usa en la presentación), o
- quitar el atributo, y entonces la página sigue el ajuste del sistema.

El modo oscuro **obligatorio** del caso §3.3 corresponde a la aplicación móvil, donde
sí sigue el ajuste del sistema desde el primer arranque. En la web es un extra.

El logo tiene el texto en azul oscuro, así que sobre fondo oscuro sería ilegible: se
publica una segunda variante (`img/logo-header-dark.svg`) y se intercambian con las
clases `.logo-claro` / `.logo-oscuro`, sin JavaScript.

### Tipografía fluida

Los títulos escalan con el viewport en lugar de saltar en cada punto de corte:

```css
.bienvenida h1 { font-size: clamp(28px, 4.2vw, 34px); }
```

### Menú hamburguesa

`Navbar.jsx` mantiene el estado `abierto` y lo cierra solo al navegar (`useEffect` sobre
`location`), porque en móvil el panel desplegado tapa la página completa. El botón
expone `aria-expanded` y `aria-controls`, y el menú se oculta con `display: none` —no
con `visibility`— para que el lector de pantalla tampoco lo recorra cuando está cerrado.

### Tablas

Las tablas de asistencia y quórum no se pueden apilar sin perder la relación entre
columnas, así que se les da **scroll horizontal propio**:

```css
.tabla-scroll { overflow-x: auto; }
.tabla-datos  { min-width: 620px; }
```

Así la página nunca desborda a 360 px, pero la tabla sigue siendo una tabla.

### Detalles que se suelen olvidar

- `font-size: 16px` en los inputs: con menos, iOS hace zoom automático al enfocar.
- `padding-inline` en `.contenedor` en lugar de `width: 90%`: en móvil aprovecha el
  ancho sin perder el margen de 16 px.
- `.grid-beneficios > :nth-child(3):last-child { grid-column: 1 / -1 }`: en tablet, la
  tercera tarjeta ocupa la fila completa y no queda un hueco.
- `@media (pointer: coarse)` aplica el mínimo de 48 px **por tipo de puntero**, no por
  ancho: una tablet grande con dedo también lo necesita.
- `@media (prefers-reduced-motion: reduce)` anula transiciones y `transform` en hover.
- `@media print` oculta cabecera, menú y pie, y libera el scroll de las tablas, para
  que los informes de cierre salgan completos.

---

## 3. Sitio EP1 — `css/estilos.css`

El sitio estático de la evaluación EP1 se conserva con su identidad visual intacta. Solo
se le agregaron puntos de corte, sin tocar los estilos existentes:

| Añadido | Qué hace |
| --- | --- |
| `@media (min-width: 761px) and (max-width: 1024px)` | Catálogo y beneficios a 2 columnas en tablet |
| `@media (max-width: 380px)` | Cumple el mínimo de 360 px que exige la pauta, sin desborde |
| `@media (pointer: coarse)` | 48 px de alto en enlaces de navegación y botones |
| `@media (prefers-reduced-motion: reduce)` | Anula las animaciones de hover |
| `img, video { max-width: 100% }` | Evita el desborde horizontal por medios anchos |

El punto de corte de teléfono original (`max-width: 760px`) no se modificó.

---

## 4. App Android — `mobile-kotlin/`

`ui/layout/AdaptiveLayout.kt` traduce `WindowWidthSizeClass` de Material 3 a un enum
`FormatoPantalla` (`TELEFONO`, `TABLET`, `ESCRITORIO`) que lleva consigo sus columnas,
su margen lateral y su ancho máximo de lectura. Se publica por `LocalFormatoPantalla`
desde `MainActivity`, así que cualquier composable lo lee sin pasarlo por parámetro.

```kotlin
val formato = LocalFormatoPantalla.current

LazyVerticalGrid(
    columns = GridCells.Fixed(formato.columnas),
    modifier = Modifier.padding(horizontal = formato.margenLateral),
)
```

- Las cabeceras ocupan `GridItemSpan(maxLineSpan)` para no quedar dentro de una columna.
- `AREA_TACTIL_MINIMA = 48.dp` se aplica con `heightIn(min = …)` en botones y con
  `sizeIn` en `IconButton`.
- El formato se recalcula al rotar y en multiventana, sin recrear la actividad.
- `AndroidManifest.xml` **no** fija `screenOrientation`: la app rota, que es lo que una
  tablet necesita.
- `res/values/themes.xml` y `res/values-night/themes.xml` evitan el destello blanco al
  abrir la app en modo oscuro, antes de que Compose se dibuje.

Detalle completo en [`mobile-kotlin/README.md`](../mobile-kotlin/README.md).

---

## 5. Accesibilidad

| Medida | Dónde |
| --- | --- |
| Enlace "saltar al contenido" para teclado | `App.jsx` + `.salto-contenido` |
| Foco visible de 3 px con color propio | `:focus-visible` en ambas hojas |
| `aria-current="page"` en el enlace activo | `Navbar.jsx` |
| `aria-expanded` / `aria-controls` en el botón de menú | `Navbar.jsx` |
| `role="progressbar"` con `aria-valuenow` en las barras de quórum | `TallerDetailPage.jsx`, `HomePage.jsx` |
| `role="status"` / `role="alert"` con `aria-live` en los avisos | `RequireAuth.jsx`, `SyncPage.jsx` |
| `<caption class="visualmente-oculto">` en cada tabla | `TallerDetailPage.jsx`, `PanelQuorumPage.jsx` |
| `scope="col"` / `scope="row"` en las cabeceras de tabla | ambas tablas |
| Sin `alert()` del navegador: todo el feedback es en línea | todo `web-react/src` |

---

## 6. Cómo comprobarlo

```bash
cd web-react
npm run dev        # http://localhost:3000
```

En las DevTools del navegador (F12 → *Toggle device toolbar*), revisar en este orden:

| Ancho | Qué debe pasar |
| --- | --- |
| 360 px | Sin scroll horizontal. Menú hamburguesa. 1 columna. |
| 390 px | Formularios a ancho completo, botones apilados. |
| 768 px | 2 columnas. Menú todavía hamburguesa. |
| 1024 px | 3 columnas. Barra de navegación en línea. |
| 1440 px | Contenedor topado en 1140 px, sin estirarse. |

Para el modo oscuro de la web: cambiar `data-theme="light"` por `data-theme="dark"`
en la etiqueta `<html>` de `web-react/index.html`. Emular
`prefers-color-scheme: dark` desde las DevTools **no** basta mientras el atributo
siga en `light`, que es justamente lo que se busca.

Para el sitio EP1, abrir `index.html` directo y repetir los mismos anchos.
