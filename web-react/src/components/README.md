# Convención de componentes (Atomic Design)

- `atoms/`: piezas visuales mínimas y sin lógica de negocio (`Button`, `Badge`).
- `molecules/`: combinan átomos entre sí o con primitivas de routing para formar
  una unidad con sentido propio (`NavLinkItem` = `Link` + `Badge`).
- `organisms/`: componentes completos de layout que se insertan directo en
  `App.tsx` (`Navbar`, `Footer`), compuestos a partir de átomos y moléculas.

El resto de `src/components` (p. ej. `RequireAuth`, `GeofenceNote`) son
componentes de propósito específico que no encajan en esta jerarquía visual.
