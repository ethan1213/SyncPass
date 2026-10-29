# -*- coding: utf-8 -*-
"""
Contenido de las vistas por rol y de los dos CRUD.

Cada función dibuja solo el área de contenido: la cabecera, el menú lateral y
el marco del dispositivo los pone generar_mockups.py. Devuelven (notas, alto).
"""
from drawio import P, SUAVE, TINTA, ancho

# Menú lateral de cada rol. Un rol, un menú distinto.
MENUS = {
    "admin": ["Panel", "Usuarios", "Grupos", "Auditoría", "Reportes"],
    "coordinador": ["Panel", "Talleres", "Monitores", "Encuestas", "Reportes"],
    "monitor": ["Terreno", "Acreditar", "Encuestas", "Cola local"],
    "participante": ["Mis talleres", "Mi código QR", "Mis encuestas"],
    "general": ["Panel", "Acreditar", "Encuestas", "Quórum", "Sincronizar", "Reportes"],
}

GRUPOS = [("admin", "rojo"), ("coordinador", "naranja"),
          ("monitor", "azul"), ("participante", "verde")]

# Cómo llama el caso a cada perfil (sección 3.1 del PDF).
PERFILES = {
    "admin": "Equipo administrativo",
    "coordinador": "Docente / Coordinador(a) de proyecto",
    "monitor": "Estudiante voluntario / Monitor",
    "participante": "Beneficiario comunitario / Asistente",
}

# Recuadro inferior del menú lateral: (título, línea 1, línea 2, barra)
PIES = {
    "admin": ("Estado del sistema", "API 99,8 % · 3 errores", "Último respaldo 03:00", None),
    "coordinador": ("Período 2026-2", "6 talleres · 4 monitores", "Cierre 20 de diciembre", None),
    "monitor": ("Cola local", "3 registros sin enviar", "Último envío 09:12", 0.7),
    "participante": ("Próximo taller", "19 oct · Ciberseguridad", "Sede CITT", None),
    "general": ("Cola local", "3 registros sin enviar", "Último envío 09:12", 0.7),
}


def _cabecera(L, x, y, w, titulo, bajada, chip=None, chip_color="morado"):
    L.txt(x, y + 14, titulo, size=14, peso=700)
    L.txt(x, y + 30, bajada, size=10, fill=SUAVE)
    if chip:
        aw = ancho(chip, 10, 700) + 22
        L.caja(x + w - aw, y + 2, aw, 24, chip, color=chip_color, rx=3, size=10, peso=700)
        return aw
    return 0


def _tabla(L, x, y, w, cols, filas, th=26, fh=26):
    """Tabla de datos con cabecera gris. Devuelve (xs, y_final)."""
    total = sum(c[1] for c in cols)
    xs, acc = [], x
    for _, peso in cols:
        xs.append(acc)
        acc += w * peso / total
    L.rect(x, y, w, th, fill="#eceff3", stroke="#9aa5b1", sw=1)
    for i, (tit, _) in enumerate(cols):
        L.txt(xs[i] + 10, y + th / 2 + 4, tit.upper(), size=8.5, peso=700, fill=SUAVE)
    for r, fila in enumerate(filas):
        fy = y + th + r * fh
        L.rect(x, fy, w, fh, fill="#ffffff" if r % 2 else "#fafbfc", stroke="#dde3ea", sw=1)
        for i, celda in enumerate(fila):
            if isinstance(celda, tuple):
                etiqueta, color = celda
                aw = ancho(etiqueta, 9, 700) + 14
                L.caja(xs[i] + 10, fy + (fh - 18) / 2, aw, 18, etiqueta, color=color,
                       rx=9, size=9, peso=700)
            else:
                L.txt(xs[i] + 10, fy + fh / 2 + 4, celda, size=9.5,
                      mono=celda.startswith(("TAL-", "USR-", "PART-", "BOR-")),
                      peso=700 if i == 0 else 400)
    return xs, y + th + len(filas) * fh


# ----------------------------------------------------------------------
# Vista por rol 1: administrador
# ----------------------------------------------------------------------
def web_rol_admin(L, x, y, w, compacta):
    _cabecera(L, x, y, w, "Administración del sistema",
              "Equipo administrativo · consolidación de registros e informes",
              "Grupo: admin", "rojo")
    L.marca(1, x + w + 10, y + 14)

    cols = 2 if compacta else 4
    ky = y + 50
    kw = (w - (cols - 1) * 12) / cols
    kpis = [("Usuarios activos", "42", "azul"), ("Grupos en Cognito", "4", "morado"),
            ("Errores de sync (24 h)", "3", "naranja"), ("Disponibilidad de la API", "99,8 %", "verde")]
    for i, (tit, val, col) in enumerate(kpis[:cols]):
        bx = x + i * (kw + 12)
        L.caja(bx, ky, kw, 66, None, color="blanco", rx=3)
        L.rect(bx, ky, 4, 66, fill=P[col][1])
        L.txt(bx + 14, ky + 20, tit, size=9.5, peso=700, fill=SUAVE)
        L.txt(bx + 14, ky + 48, val, size=20, peso=700)

    ty = ky + 86
    L.txt(x, ty, "Usuarios y grupos", size=12, peso=700)
    tw = w if compacta else w * 0.62
    L.caja(x + tw - 150, ty - 16, 150, 24, "+ Invitar usuario", color="verde", rx=3,
           size=10, peso=700)
    cols_t = [("Usuario", 1.5), ("Grupo de Cognito", 1.2), ("Estado", 0.9), ("Último acceso", 1.1)]
    filas = [
        ["USR-004 · m.vega", ("monitor", "azul"), ("Activo", "verde"), "hoy 09:41"],
        ["USR-002 · p.soto", ("coordinador", "naranja"), ("Activo", "verde"), "hoy 08:12"],
        ["USR-011 · vecino.a", ("participante", "verde"), ("Activo", "verde"), "ayer 19:05"],
        ["USR-001 · admin.citt", ("admin", "rojo"), ("Activo", "verde"), "hoy 10:02"],
        ["USR-009 · j.rivas", ("monitor", "azul"), ("Suspendido", "rojo"), "12 oct"],
    ]
    xs, fin = _tabla(L, x, ty + 12, tw, cols_t, filas)
    L.marca(2, x - 10, ty + 12 + 26 + 13)
    L.txt(x, fin + 20, "El grupo de Cognito define el menú y las acciones permitidas.",
          size=9.5, fill=SUAVE)

    if not compacta:
        px = x + tw + 18
        pw = w - tw - 18
        L.caja(px, ty + 12, pw, 142, None, color="blanco", rx=3)
        L.txt(px + 14, ty + 34, "Grupos del user pool", size=11, peso=700)
        for i, (g, col) in enumerate(GRUPOS):
            gy = ty + 46 + i * 24
            aw = ancho(g, 9.5, 700) + 18
            L.caja(px + 14, gy, aw, 19, g, color=col, rx=9, size=9.5, peso=700)
            L.txt(px + 14 + aw + 10, gy + 14, PERFILES[g], size=9, fill=SUAVE)
        L.marca(3, px + pw + 10, ty + 60)
        alto_nota = L.nota(px, ty + 170, pw, ["El caso describe tres perfiles (§3.1).",
                                              "El cuarto sale de la etapa de",
                                              "consolidación, donde trabaja el",
                                              "equipo administrativo."],
                           titulo="De dónde salen los cuatro grupos")
        fin = max(fin, ty + 170 + alto_nota)
    return [
        (1, "Vista exclusiva del admin", ["Los otros tres roles no ven", "este menú ni esta ruta."]),
        (2, "Un grupo por usuario", ["El token llega con cognito:groups", "y la app arma el menú con eso."]),
        (3, "Cuatro grupos, cuatro vistas", ["admin, coordinador, monitor", "y participante."]),
    ], (fin + 34) - y


# ----------------------------------------------------------------------
# Vista por rol 2: coordinador
# ----------------------------------------------------------------------
def web_rol_coordinador(L, x, y, w, compacta):
    _cabecera(L, x, y, w, "Coordinación de talleres",
              "Docente / Coordinador(a) de proyecto · período 2026-2",
              "Grupo: coordinador", "naranja")
    L.marca(1, x + w + 10, y + 14)

    ky = y + 50
    cols = 2 if compacta else 3
    kw = (w - (cols - 1) * 14) / cols
    for i, (tit, val, pie, col) in enumerate([
            ("Talleres del período", "6", "2 en curso hoy", "azul"),
            ("Monitores asignados", "4", "1 sin taller esta semana", "morado"),
            ("Quórum promedio", "71 %", "meta institucional: 60 %", "verde")][:cols]):
        bx = x + i * (kw + 14)
        L.caja(bx, ky, kw, 70, None, color="blanco", rx=3)
        L.rect(bx, ky, 4, 70, fill=P[col][1])
        L.txt(bx + 14, ky + 20, tit, size=9.5, peso=700, fill=SUAVE)
        L.txt(bx + 14, ky + 46, val, size=20, peso=700)
        L.txt(bx + 14, ky + 62, pie, size=9, fill=SUAVE)

    ty = ky + 92
    L.txt(x, ty, "Talleres del proyecto", size=12, peso=700)
    L.caja(x + w - 196, ty - 16, 196, 24, "Administrar talleres (CRUD)", color="azul",
           rx=3, size=10, peso=700)
    L.marca(2, x + w + 10, ty - 4)
    cols_t = [("Taller", 2.2), ("Recinto", 1.6), ("Fecha", 1.0), ("Quórum", 0.9), ("Estado", 1.0)]
    filas = [
        ["TAL-018 · Alfabetización digital", "UV 12 Los Castaños", "18 oct", "24/35", ("En curso", "verde")],
        ["TAL-019 · Huerto comunitario", "UV 7 El Parrón", "18 oct", "11/30", ("Programado", "azul")],
        ["TAL-020 · Ciberseguridad vecinal", "Sede CITT", "19 oct", "0/25", ("Programado", "azul")],
    ]
    xs, fin = _tabla(L, x, ty + 12, w, cols_t, filas)

    gy = fin + 30
    L.txt(x, gy, "Asistencia por comuna", size=12, peso=700)
    gw = w if compacta else w * 0.52
    L.caja(x, gy + 10, gw, 150, None, color="blanco", rx=3)
    L.barras(x + 40, gy + 34, gw - 70, 86, [128, 74, 51], ["La Florida", "Puente Alto", "La Granja"])
    L.marca(3, x - 10, gy + 80)
    if not compacta:
        nx = x + gw + 18
        nw = w - gw - 18
        L.caja(nx, gy + 10, nw, 150, None, color="blanco", rx=3)
        L.txt(nx + 14, gy + 32, "Monitores asignados", size=11, peso=700)
        for i, (nom, taller) in enumerate([("M. Vega", "TAL-018"), ("J. Rivas", "TAL-019"),
                                           ("C. Pinto", "TAL-020"), ("Sin asignar", "—")]):
            L.txt(nx + 14, gy + 56 + i * 22, nom, size=10)
            L.txt(nx + nw - 14, gy + 56 + i * 22, taller, size=9.5, fill=SUAVE, anchor="end", mono=True)
    return [
        (1, "Vista exclusiva del coordinador", ["Ve todos los talleres de sus", "proyectos, no solo los de hoy."]),
        (2, "Entrada al CRUD por API", ["Crear, editar y eliminar talleres", "contra la API en EC2."]),
        (3, "Reportes del data warehouse", ["Los totales salen del modelo", "en estrella, no de la base OLTP."]),
    ], (gy + 172) - y


# ----------------------------------------------------------------------
# Vista por rol 3: monitor en terreno
# ----------------------------------------------------------------------
def web_rol_monitor(L, x, y, w, compacta):
    _cabecera(L, x, y, w, "Terreno · acreditación del taller",
              "Estudiante voluntario / Monitor · UV 12 Los Castaños · hoy 10:00",
              "Grupo: monitor", "azul")
    L.marca(1, x + w + 10, y + 14)

    ky = y + 50
    aw = w if compacta else w * 0.46
    L.caja(x, ky, aw, 118, None, color="blanco", rx=3)
    L.txt(x + 16, ky + 24, "Taller en curso", size=10, peso=700, fill=SUAVE)
    L.txt(x + 16, ky + 48, "24 / 35 acreditados", size=18, peso=700)
    L.barra_progreso(x + 16, ky + 60, aw - 32, 24 / 35, h=9)
    L.txt(x + 16, ky + 88, "Comenzó 10:00 · quedan 12 cupos", size=9.5, fill=SUAVE)
    L.caja(x + 16, ky + 96, 150, 0, None, color="blanco", rx=0, sw=0)

    bx = x if compacta else x + aw + 18
    by = ky + 130 if compacta else ky
    bw = w if compacta else w - aw - 18
    L.caja(bx, by, bw, 56, "Abrir escáner QR", color="azul", rx=3, size=12, peso=700)
    L.marca(2, bx - 10 if compacta else bx + bw + 10, by + 28)
    L.caja(bx, by + 66, (bw - 12) / 2, 44, ["Encuesta", "al cierre"], color="blanco", rx=3, size=10)
    L.caja(bx + (bw - 12) / 2 + 12, by + 66, (bw - 12) / 2, 44, ["Cola local", "3 pendientes"],
           color="naranja", rx=3, size=10)

    ty = (by + 130) if not compacta else (by + 130)
    L.txt(x, ty, "Últimas acreditaciones", size=12, peso=700)
    cols_t = [("Participante", 1.6), ("Hora", 0.9), ("Validación", 1.1), ("Estado", 1.0)]
    filas = [
        ["PART-LF-0241", "10:04:18", ("GPS válido", "verde"), ("Local", "naranja")],
        ["PART-LF-0188", "10:03:02", ("GPS válido", "verde"), ("Enviado", "verde")],
        ["PART-LF-0132", "10:02:15", ("GPS válido", "verde"), ("Enviado", "verde")],
    ]
    xs, fin = _tabla(L, x, ty + 12, w, cols_t, filas)
    L.marca(3, x + w + 10, ty + 12 + 26 + 13)
    return [
        (1, "Vista exclusiva del monitor", ["Solo su taller del día.", "No ve la administración."]),
        (2, "Acción única y grande", ["Todo el rol gira en torno a", "acreditar rápido."]),
        (3, "Lo local se ve distinto", ["El monitor sabe qué falta", "por sincronizar."]),
    ], (fin + 20) - y


def mov_rol_monitor(L, x, y, w):
    y0 = y
    y = L.barra_app(x, y, w, "Terreno", "Monitor · M. Vega", chip=("SIN SEÑAL", False))
    L.caja(x + 10, y + 12, w - 20, 92, None, color="blanco", rx=3)
    L.txt(x + 22, y + 32, "Alfabetización digital", size=11.5, peso=700)
    L.txt(x + 22, y + 48, "UV 12 Los Castaños · 10:00", size=9, fill=SUAVE)
    L.txt(x + 22, y + 74, "24 / 35", size=17, peso=700)
    L.barra_progreso(x + 22, y + 84, w - 44, 24 / 35, h=8)
    L.marca(1, x + w + 10, y + 20)
    L.caja(x + 10, y + 116, w - 20, 58, "Abrir escáner QR", color="azul", rx=3, size=12, peso=700)
    L.marca(2, x - 10, y + 145)
    L.caja(x + 10, y + 184, (w - 28) / 2, 46, ["Encuesta", "al cierre"], color="blanco", rx=3, size=9.5)
    L.caja(x + 18 + (w - 28) / 2, y + 184, (w - 28) / 2, 46, ["Cola local", "3 pendientes"],
           color="naranja", rx=3, size=9.5)
    L.txt(x + 16, y + 258, "Últimas acreditaciones", size=10.5, peso=700)
    for i, (pid, hora, est, col) in enumerate([("PART-LF-0241", "10:04", "Local", "naranja"),
                                               ("PART-LF-0188", "10:03", "Enviado", "verde"),
                                               ("PART-LF-0132", "10:02", "Enviado", "verde")]):
        by = y + 268 + i * 44
        L.caja(x + 10, by, w - 20, 38, None, color="blanco", rx=3)
        L.txt(x + 20, by + 17, pid, size=9.5, mono=True, peso=700)
        L.txt(x + 20, by + 31, "QR · " + hora, size=9, fill=SUAVE)
        aw = ancho(est, 9, 700) + 14
        L.caja(x + w - aw - 18, by + 10, aw, 18, est, color=col, rx=9, size=9, peso=700)
    L.marca(3, x - 10, y + 287)
    return [
        (1, "Solo su taller del día", ["El monitor no navega entre", "proyectos ni usuarios."]),
        (2, "Botón de 56 dp", ["Pensado para usar de pie", "y con una sola mano."]),
        (3, "Estado por fila", ["Local = aún en el teléfono."]),
    ], (y + 268 + 3 * 44 + 10) - y0


# ----------------------------------------------------------------------
# Vista por rol 4: participante
# ----------------------------------------------------------------------
def web_rol_participante(L, x, y, w, compacta):
    _cabecera(L, x, y, w, "Mi cuenta",
              "Beneficiario comunitario / Asistente · PART-LF-0241",
              "Grupo: participante", "verde")
    L.marca(1, x + w + 10, y + 14)

    ky = y + 50
    qw = w if compacta else w * 0.34
    L.caja(x, ky, qw, 246, None, color="blanco", rx=3)
    L.txt(x + 16, ky + 24, "Mi código QR", size=11, peso=700)
    lado = min(qw - 80, 150)
    L.qr(x + qw / 2 - lado / 2, ky + 40, lado, semilla=11)
    L.txt(x + qw / 2, ky + 40 + lado + 24, "PART-LF-0241", size=10.5, mono=True, anchor="middle")
    L.txt(x + qw / 2, ky + 40 + lado + 40, "Se muestra al monitor al llegar", size=9, fill=SUAVE,
          anchor="middle")
    L.marca(2, x - 10, ky + 110)

    tx = x if compacta else x + qw + 18
    ty = ky + 262 if compacta else ky
    tw = w if compacta else w - qw - 18
    L.txt(tx, ty + 14, "Mis talleres", size=12, peso=700)
    cols_t = [("Taller", 2.0), ("Fecha", 1.0), ("Asistencia", 1.0), ("Encuesta", 1.1)]
    filas = [
        ["Alfabetización digital", "18 oct", ("Acreditado", "verde"), ("Pendiente", "naranja")],
        ["Huerto comunitario", "11 oct", ("Acreditado", "verde"), ("Respondida", "verde")],
        ["Ciberseguridad vecinal", "19 oct", ("Inscrito", "azul"), ("—", "gris")],
    ]
    xs, fin = _tabla(L, tx, ty + 26, tw, cols_t, filas)
    L.caja(tx, fin + 16, 220, 32, "Responder encuesta pendiente", color="azul", rx=3,
           size=10.5, peso=700)
    L.marca(3, tx + 230, fin + 16)
    L.guia([(tx + 222, fin + 32), (tx + 230, fin + 32)])
    return [
        (1, "Vista exclusiva del participante", ["Solo sus propios datos:", "no ve otros asistentes."]),
        (2, "Su QR, siempre a mano", ["Es lo que lee el monitor", "para acreditarlo."]),
        (3, "Encuesta desde su cuenta", ["Si no alcanzó a responder", "en el taller."]),
    ], (max(fin + 60, ky + 262)) - y


def mov_rol_participante(L, x, y, w):
    y0 = y
    y = L.barra_app(x, y, w, "Mi cuenta", "Participante", chip=("CONECTADO", True))
    L.caja(x + 10, y + 12, w - 20, 232, None, color="blanco", rx=3)
    L.txt(x + 22, y + 32, "Mi código QR", size=10.5, peso=700)
    lado = w - 140
    L.qr(x + w / 2 - lado / 2, y + 46, lado, semilla=11)
    L.txt(x + w / 2, y + 60 + lado + 14, "PART-LF-0241", size=10, mono=True, anchor="middle")
    L.marca(1, x + w + 10, y + 120)
    L.txt(x + 16, y + 272, "Mis talleres", size=10.5, peso=700)
    for i, (nom, fecha, est, col) in enumerate([("Alfabetización digital", "18 oct", "Acreditado", "verde"),
                                                ("Huerto comunitario", "11 oct", "Respondida", "verde"),
                                                ("Ciberseguridad vecinal", "19 oct", "Inscrito", "azul")]):
        by = y + 282 + i * 48
        L.caja(x + 10, by, w - 20, 42, None, color="blanco", rx=3)
        L.txt(x + 20, by + 19, nom, size=10, peso=700)
        L.txt(x + 20, by + 33, fecha, size=9, fill=SUAVE)
        aw = ancho(est, 9, 700) + 14
        L.caja(x + w - aw - 18, by + 12, aw, 18, est, color=col, rx=9, size=9, peso=700)
    fin = y + 282 + 3 * 48 + 6
    L.caja(x + 10, fin, w - 20, 44, "Responder encuesta pendiente", color="azul", rx=3,
           size=10.5, peso=700)
    L.marca(2, x - 10, fin + 22)
    return [
        (1, "Su QR ocupa la pantalla", ["Se lee aunque haya sol", "o poca cobertura."]),
        (2, "Única acción pendiente", ["La encuesta del último taller."]),
    ], (fin + 54) - y0


# ----------------------------------------------------------------------
# CRUD de negocio contra la API (EC2): talleres
# ----------------------------------------------------------------------
def web_crud_talleres(L, x, y, w, compacta):
    _cabecera(L, x, y, w, "Talleres · altas, cambios y bajas",
              "CRUD de negocio contra la API REST desplegada en EC2",
              "Grupo: coordinador", "naranja")

    lw = w if compacta else w * 0.56
    L.caja(x + lw - 128 if not compacta else x + w - 128, y + 48, 128, 28, "+ Nuevo taller",
           color="verde", rx=3, size=10.5, peso=700)
    L.marca(1, (x + lw - 148) if not compacta else (x + w - 148), y + 48)

    cols_t = [("ID", 0.8), ("Taller", 2.0), ("Fecha", 0.9), ("Cupos", 0.7), ("Acciones", 1.2)]
    filas = [
        ["TAL-018", "Alfabetización digital", "18 oct", "35", ""],
        ["TAL-019", "Huerto comunitario", "18 oct", "30", ""],
        ["TAL-020", "Ciberseguridad vecinal", "19 oct", "25", ""],
    ]
    xs, fin = _tabla(L, x, y + 86, lw, cols_t, filas)
    for r in range(3):
        fy = y + 86 + 26 + r * 26
        L.caja(xs[4] + 8, fy + 4, 46, 18, "Editar", color="azul", rx=3, size=8.5, peso=700)
        L.caja(xs[4] + 58, fy + 4, 50, 18, "Eliminar", color="rojo", rx=3, size=8.5, peso=700)
    L.marca(2, x - 10, y + 86 + 26 + 13)
    L.marca(3, x - 10, y + 86 + 26 + 13 + 52)
    L.txt(x, fin + 20, "3 de 6 talleres · página 1", size=9.5, fill=SUAVE)

    # Formulario de alta y edición
    fx = x if compacta else x + lw + 18
    fy0 = fin + 40 if compacta else y + 48
    fw = w if compacta else w - lw - 18
    alto_form = 318
    L.caja(fx, fy0, fw, alto_form, None, color="blanco", rx=3)
    L.txt(fx + 14, fy0 + 24, "Editar taller TAL-018", size=11.5, peso=700)
    L.txt(fx + 14, fy0 + 40, "El mismo formulario sirve para crear", size=9, fill=SUAVE)
    campos = [("Nombre de la actividad", "Alfabetización digital"), ("Recinto", "UV 12 Los Castaños"),
              ("Fecha y hora", "18-10-2026 10:00"), ("Cupos máximos", "35")]
    cy = fy0 + 62
    for etiqueta, valor in campos:
        L.campo_wf(fx + 14, cy + 12, fw - 28, 30, etiqueta, valor)
        cy += 48
    L.caja(fx + 14, cy + 10, (fw - 40) / 2, 32, "Guardar", color="azul", rx=3, size=10.5, peso=700)
    L.caja(fx + 26 + (fw - 40) / 2, cy + 10, (fw - 40) / 2, 32, "Cancelar", color="blanco",
           rx=3, size=10.5, stroke="#9aa5b1")
    L.marca(4, fx + fw + 10, cy + 26)

    # Confirmación de borrado
    dy = max(fin + 48, fy0 + alto_form + 22)
    dw = min(360, w * 0.46)
    L.caja(x, dy, dw, 104, None, color="rojo", rx=3)
    L.txt(x + 14, dy + 26, "¿Eliminar TAL-020?", size=11.5, peso=700)
    L.txt(x + 14, dy + 44, "Se borra el taller y sus marcaciones.", size=9.5, fill=SUAVE)
    L.txt(x + 14, dy + 58, "No se puede deshacer.", size=9.5, fill=SUAVE)
    L.caja(x + 14, dy + 68, 96, 26, "Eliminar", color="rojo", rx=3, size=10, peso=700)
    L.caja(x + 118, dy + 68, 96, 26, "Cancelar", color="blanco", rx=3, size=10, stroke="#9aa5b1")
    L.marca(5, x - 10, dy + 81)

    # Mapa de operaciones contra la API
    ax = x if compacta else x + dw + 18
    ay = dy + 118 if compacta else dy
    aw2 = w if compacta else w - dw - 18
    L.caja(ax, ay, aw2, 104, None, color="morado", rx=3)
    L.txt(ax + 14, ay + 22, "Qué llama cada botón (API en EC2)", size=10.5, peso=700)
    ops = [("Listar", "GET /api/talleres"), ("Crear", "POST /api/talleres"),
           ("Editar", "PUT /api/talleres/:id"), ("Eliminar", "DELETE /api/talleres/:id")]
    for i, (accion, ruta) in enumerate(ops):
        oy = ay + 36 + (i % 2) * 20
        ox = ax + 14 + (i // 2) * (aw2 - 28) / 2
        L.txt(ox, oy + 10, accion, size=9.5, peso=700)
        L.txt(ox + 52, oy + 10, ruta, size=9.5, mono=True, fill=SUAVE)
    L.txt(ax + 14, ay + 92, "Todas con el token de Cognito en la cabecera Authorization.",
          size=9, fill=SUAVE)
    return [
        (1, "C de crear", ["Abre el formulario vacío y", "hace POST al guardar."]),
        (2, "R de leer", ["La tabla es un GET con", "paginado."]),
        (3, "U de actualizar", ["Editar carga la fila en el", "formulario y hace PUT."]),
        (4, "Formulario único", ["Crear y editar comparten", "campos y validaciones."]),
        (5, "D de eliminar", ["Siempre con confirmación", "porque arrastra marcaciones."]),
    ], (ay + 118) - y


# ----------------------------------------------------------------------
# CRUD local (sin conexión): borradores de encuesta
# ----------------------------------------------------------------------
def web_crud_local(L, x, y, w, compacta):
    _cabecera(L, x, y, w, "Borradores de encuesta guardados en el equipo",
              "CRUD local: se crea, edita y borra sin conexión; la API no participa",
              "Modo sin conexión", "naranja")

    L.caja(x, y + 50, w, 44, None, color="amarillo", rx=3)
    L.txt(x + 14, y + 70, "Sin señal en la UV 12 Los Castaños.", size=10, peso=700)
    L.txt(x + 14, y + 86, "Los 3 borradores viven en el navegador (IndexedDB) o en Room si es la app móvil.",
          size=9.5, fill=SUAVE)
    L.marca(1, x - 10, y + 72)

    lw = w if compacta else w * 0.58
    cols_t = [("ID local", 1.0), ("Participante", 1.3), ("Estrellas", 0.9), ("Estado", 1.0), ("Acciones", 1.2)]
    filas = [
        ["BOR-007", "PART-LF-0241", "★★★★☆", ("Borrador", "naranja"), ""],
        ["BOR-006", "PART-LF-0188", "★★★★★", ("Listo", "azul"), ""],
        ["BOR-005", "PART-LF-0132", "★★★☆☆", ("Listo", "azul"), ""],
    ]
    ty = y + 110
    xs, fin = _tabla(L, x, ty, lw, cols_t, filas)
    for r in range(3):
        fy = ty + 26 + r * 26
        L.caja(xs[4] + 8, fy + 4, 46, 18, "Editar", color="azul", rx=3, size=8.5, peso=700)
        L.caja(xs[4] + 58, fy + 4, 50, 18, "Borrar", color="rojo", rx=3, size=8.5, peso=700)
    L.marca(2, x - 10, ty + 26 + 13)

    L.caja(x, fin + 16, 208, 32, "Enviar los 3 al servidor", color="verde", rx=3, size=10.5, peso=700)
    L.txt(x + 220, fin + 36, "se habilita solo cuando vuelve la señal", size=9.5, fill=SUAVE)
    L.marca(3, x - 10, fin + 32)

    if not compacta:
        fx = x + lw + 18
        fw = w - lw - 18
        L.caja(fx, ty, fw, 212, None, color="blanco", rx=3)
        L.txt(fx + 14, ty + 24, "Editar borrador BOR-007", size=11, peso=700)
        L.txt(fx + 14, ty + 44, "Satisfacción", size=9.5, peso=700, fill=SUAVE)
        for i in range(5):
            L.estrella(fx + 28 + i * 34, ty + 68, 13, llena=i < 4)
        L.txt(fx + 14, ty + 96, "Utilidad", size=9.5, peso=700, fill=SUAVE)
        ox = fx + 14
        for i, op in enumerate(["Mucho", "Bastante", "Algo"]):
            aw = ancho(op, 9, 700) + 18
            L.caja(ox, ty + 104, aw, 22, op, color="azul" if i == 0 else "blanco",
                   rx=11, size=9, peso=700, stroke=None if i == 0 else "#c2cbd4")
            ox += aw + 8
        L.campo_wf(fx + 14, ty + 148, fw - 28, 30, "Comentario", "Muy buena atención…")
        L.caja(fx + 14, ty + 190, 90, 0, None, color="blanco", rx=0, sw=0)
        L.caja(fx + 14, ty + 172 + 10, (fw - 40) / 2, 26, "Guardar en el equipo", color="azul",
               rx=3, size=9.5, peso=700)
        L.caja(fx + 26 + (fw - 40) / 2, ty + 182, (fw - 40) / 2, 26, "Descartar", color="blanco",
               rx=3, size=9.5, stroke="#9aa5b1")
        L.marca(4, fx + fw + 10, ty + 195)
        alto_nota = L.nota(fx, ty + 228, fw, ["Mientras el borrador no se", "envía, nada de esto toca",
                                              "la base del servidor."], titulo="Solo en el equipo")
        fin = max(fin, ty + 228 + alto_nota - 70)
    return [
        (1, "Funciona sin conexión", ["Es el CRUD que no depende", "de la API ni de la red."]),
        (2, "Crear, leer, editar y borrar", ["Las cuatro operaciones ocurren", "en el almacenamiento local."]),
        (3, "El envío es otro paso", ["Recién ahí aparece la API", "y el registro pasa a is_synced = 1."]),
        (4, "Mismo formulario de siempre", ["El participante no nota que", "está trabajando en local."]),
    ], (fin + 70) - y


def mov_crud_local(L, x, y, w):
    y0 = y
    y = L.barra_app(x, y, w, "Borradores", "3 encuestas en el equipo", atras=True,
                    chip=("SIN SEÑAL", False))
    L.caja(x + 10, y + 12, w - 20, 54, None, color="amarillo", rx=3)
    L.txt(x + 22, y + 32, "Sin señal en el recinto", size=10.5, peso=700)
    L.txt(x + 22, y + 50, "Se guardan en Room y se envían después", size=9, fill=SUAVE)
    L.marca(1, x - 10, y + 39)
    for i, (bid, part, estrellas, est, col) in enumerate([
            ("BOR-007", "PART-LF-0241", 4, "Borrador", "naranja"),
            ("BOR-006", "PART-LF-0188", 5, "Listo", "azul"),
            ("BOR-005", "PART-LF-0132", 3, "Listo", "azul")]):
        by = y + 82 + i * 86
        L.caja(x + 10, by, w - 20, 76, None, color="blanco", rx=3)
        L.txt(x + 20, by + 20, bid, size=9.5, mono=True, peso=700)
        aw = ancho(est, 9, 700) + 14
        L.caja(x + w - aw - 18, by + 10, aw, 18, est, color=col, rx=9, size=9, peso=700)
        L.txt(x + 20, by + 36, part, size=9, fill=SUAVE, mono=True)
        for k in range(5):
            L.estrella(x + 26 + k * 22, by + 54, 8, llena=k < estrellas)
        L.caja(x + w - 134, by + 46, 54, 20, "Editar", color="azul", rx=3, size=8.5, peso=700)
        L.caja(x + w - 76, by + 46, 58, 20, "Borrar", color="rojo", rx=3, size=8.5, peso=700)
    L.marca(2, x + w + 10, y + 128)
    fin = y + 82 + 3 * 86
    L.caja(x + 10, fin + 8, w - 20, 46, "Enviar cuando haya señal", color="verde", rx=3,
           size=10.5, peso=700)
    L.marca(3, x - 10, fin + 31)
    return [
        (1, "Todo ocurre en el teléfono", ["Room/SQLite. La API no", "se entera todavía."]),
        (2, "Editar y borrar sin red", ["El monitor corrige errores", "en el mismo recinto."]),
        (3, "El envío es manual o automático", ["Al volver la señal pasan a", "la cola de sincronización."]),
    ], (fin + 64) - y0
