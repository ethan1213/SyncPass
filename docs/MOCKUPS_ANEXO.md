# Anexo de pantallas SyncPass

Estos wireframes complementan las [láminas SVG](MOCKUPS.md) y el
[visor por perfiles](https://web-react-nu-nine.vercel.app/mockups.html).
La navegación y los nombres corresponden al proyecto de Ethan Astorga.

## Registro

```text
┌ Registro · Web y Android ────────┐
│ Usuario, correo y apodo          │
│ Contraseña                       │
│ [Crear cuenta]                   │
│ Confirmar: [Código] [Confirmar]  │
└─────────────────────────────────┘
```

Implementado en `/registro` (React) y `RegisterScreen` (Android). Los pasos dependen de la respuesta de Cognito y permiten volver al login.

## Panel por perfil y QR personal

```text
┌ Mi panel · rol activo ──────────┐ ┌ Mi QR ─────────────────────┐
│ Acciones permitidas según rol   │ │ QR de la cuenta           │
│ [Talleres] [Bitácora / acceso]   │ │ SP1: identificador       │
│ [Mantenedores] cuando aplican   │ │ Mostrar al monitor        │
└────────────────────────────────┘ └───────────────────────────┘
```

Web usa `/mi-panel` y `/mi-qr`. Android muestra el QR en el panel Inicio y adapta las acciones. El usuario muestra su QR al equipo para acreditarse; el QR del taller abre el acceso de invitado.

## Acceso invitado Android

```text
┌ Acceder a SyncPass ──────────────┐
│ [Iniciar sesión o registrarse]   │
│ Código del taller [____________]│
│ [Continuar] [Escanear QR]        │
└─────────────────────────────────┘
                ↓
┌ Taller invitado ────────────────┐
│ Nombre, fecha y ubicación        │
│ [Registrar asistencia]          │
│ Estado de la asistencia          │
└─────────────────────────────────┘
```

Implementado con `EntryScreen`, `GuestQrScreen` y `GuestTallerScreen`. El QR abre la misma ficha que el código. Si la geocerca lo exige, se solicita ubicación.

## Preinscripción

```text
┌ Mis talleres pendientes ────────┐
│ Taller         Fecha      Estado │
│ Taller A       22/11   Pendiente │
│ Taller B       25/11     Asistió │
└─────────────────────────────────┘
```

La web enlaza el catálogo de preinscripción en `/mis-talleres` y permite marcar «voy» cuando aplica. Android consulta el estado del usuario en `MisInscripcionesScreen`. La lámina SVG dedicada queda como mejora visual.

## Bitácora y resumen

```text
┌ Bitácora del taller ────────────┐
│ Estado de la publicación        │
│ Resumen                         │
│ [Ver apuntes y fuentes]         │
│ Pregunta [___________________]  │
│ [Buscar respuesta]             │
│ Respuesta · fragmento · cita    │
└────────────────────────────────┘
```

Implementado en `/bitacora/:id` y `BitacoraScreen`. La edición y publicación están en web para los roles de gestión. El lector puede actualizar la bitácora; la transcripción cloud requiere el proveedor configurado y el JAR actualizado.

## Taller o ruta inexistente

```text
┌ Taller no disponible ───────────┐
│ El catálogo no contiene el ID.   │
│ [Volver al panel de talleres]    │
└─────────────────────────────────┘
```

La web espera a que termine la carga del catálogo antes de mostrar el estado vacío de una ficha. Android informa del código inválido o del taller ausente en el flujo de invitado.

## Elementos que forman parte de otra pantalla

| Elemento | Ubicación real |
| --- | --- |
| Sincronización y confirmaciones Android | Sección del panel Inicio; no se presenta como pantalla independiente en la aplicación actual. |
| Detalle, QR de taller y quórum web | La ficha `/taller/:id` muestra la información pública y el QR; el panel de quórum `/panel` está protegido. |
| Encuesta y borradores web | `/encuesta` y `/borradores` comparten el editor de registros locales. |
| Mantenedores de talleres y proyectos | Sus láminas están en el [inventario principal](MOCKUPS.md). Android consulta el catálogo creado en web. |
