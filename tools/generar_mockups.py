# -*- coding: utf-8 -*-
"""
Genera los dibujos de los mockups de SyncPass en SVG, con el aspecto de un
diagrama hecho en draw.io: cajas de borde fino, conectores ortogonales,
notas amarillas y numeros de referencia.

Salida: img/mockups/<vista>-<formato>.svg
  vistas : panel, login, escaner, encuesta, quorum, sync
  formato: web (escritorio), tablet, movil

Cada funcion de vista devuelve (notas, alto_usado) para que la hoja se ajuste
al contenido y no queden huecos vacios.

Uso: python tools/generar_mockups.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from drawio import Lienzo, panel_notas, P, TINTA, SUAVE, ancho  # noqa: E402
from vistas_roles import (MENUS, GRUPOS, PIES, PERFILES, web_rol_admin, web_rol_coordinador,  # noqa: E402
                          web_rol_monitor, mov_rol_monitor, web_rol_participante,
                          mov_rol_participante, web_crud_talleres, web_crud_local,
                          mov_crud_local)

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SALIDA = os.path.join(RAIZ, "img", "mockups")

NAV = MENUS["general"]
NOTAS_W = 330


# ----------------------------------------------------------------------
# Cromo compartido de la aplicacion web
# ----------------------------------------------------------------------
def cabecera_app(L, x, y, w, activo, compacta=False):
    """Cabecera: logo, contexto y usuario. La navegación vive en el menú lateral."""
    h = 46
    L.rect(x, y, w, h, fill="#ffffff", stroke="#9aa5b1", sw=1)
    L.caja(x + 12, y + 11, 24, 24, "QR", color="azul", rx=3, size=9, peso=700)
    L.txt(x + 44, y + 21, "SyncPass", size=13, peso=700)
    L.txt(x + 44, y + 34, "CITT Plaza Vespucio", size=8.5, fill=SUAVE)
    if compacta:
        L.icono_lineas(x + w - 150, y + 17, 14)
    else:
        L.txt(x + 180, y + 28, "Vinculación con el Medio · período 2026-2", size=10, fill=SUAVE)
    L.caja(x + w - 130, y + 11, 24, 24, "?", color="blanco", rx=3, size=10, stroke="#c2cbd4")
    L.caja(x + w - 98, y + 11, 86, 24, "M. Vega", color="gris", rx=12, size=9.5)
    L.linea(x, y + h, x + w, y + h, stroke="#9aa5b1", sw=1)
    return y + h


def menu_lateral(L, x, y, w, h, activo, items=None, pie=None):
    """Menú lateral de la versión escritorio."""
    L.rect(x, y, w, h, fill="#fafbfc", stroke="#9aa5b1", sw=1)
    iy = y + 14
    for it in (items or NAV):
        act = it == activo
        L.caja(x + 10, iy, w - 20, 30, it, color="azul" if act else "blanco", rx=3,
               size=10.5, peso=700 if act else 400,
               stroke=None if act else "#c2cbd4")
        iy += 36
    titulo, l1, l2, barra = pie or PIES["general"]
    cy = y + h - 82
    L.caja(x + 10, cy, w - 20, 68, None, color="amarillo" if barra else "gris", rx=3)
    L.txt(x + 20, cy + 17, titulo, size=9.5, peso=700)
    L.txt(x + 20, cy + 32, l1, size=9, fill=SUAVE)
    if barra:
        L.barra_progreso(x + 20, cy + 39, w - 40, barra, h=7, color=P["naranja"][1])
    L.txt(x + 20, cy + 60, l2, size=8.5, fill=SUAVE)


def tabs_compactas(L, x, y, w, activo, items=None):
    """Navegación horizontal que reemplaza el menú lateral en tablet."""
    L.rect(x, y, w, 32, fill="#fafbfc", stroke="#9aa5b1", sw=1)
    tx = x + 10
    for it in (items or NAV):
        aw = ancho(it, 10, 700) + 18
        act = it == activo
        L.caja(tx, y + 5, aw, 22, it, color="azul" if act else "blanco", rx=3, size=10,
               peso=700 if act else 400, stroke=None if act else "#c2cbd4")
        tx += aw + 6
    return y + 32


# ----------------------------------------------------------------------
# Vistas web / tablet  ->  (notas, alto)
# ----------------------------------------------------------------------
def web_panel(L, x, y, w, compacta):
    cols = 2 if compacta else 3
    L.txt(x, y + 14, "Panel de talleres en terreno", size=14, peso=700)
    L.txt(x, y + 30, "Actividades asignadas al monitor · sábado 18 de octubre", size=10, fill=SUAVE)
    L.caja(x + w - 140, y + 2, 140, 26, "+ Nuevo taller", color="verde", rx=3, size=10.5, peso=700)
    L.marca(1, x + w - 148, y + 2)

    ky = y + 48
    kw = (w - (cols - 1) * 14) / cols
    kpis = [("Tiempo medio de acreditación", "3,2 s", "meta del caso: bajo 5 s", "verde"),
            ("Respuesta de encuestas", "76 %", "antes, por correo: 15 %", "azul"),
            ("Planillas en papel", "0", "100 % digital, sin listas perdidas", "naranja")]
    for i, (tit, val, pie, col) in enumerate(kpis[:cols]):
        bx = x + i * (kw + 14)
        L.caja(bx, ky, kw, 74, None, color="blanco", rx=3)
        L.rect(bx, ky, 4, 74, fill=P[col][1])
        L.txt(bx + 14, ky + 18, tit, size=9.5, peso=700, fill=SUAVE)
        L.txt(bx + 14, ky + 44, val, size=22, peso=700)
        L.txt(bx + 14, ky + 60, pie, size=9, fill=SUAVE)
    L.marca(2, x - 10, ky + 37)

    fy = ky + 92
    L.txt(x, fy + 15, "Talleres asignados", size=12, peso=700)
    chips = ["Todos (6)", "Hoy (2)", "Sin sincronizar (1)"]
    cx = x + w
    for ch in reversed(chips):
        aw = ancho(ch, 9.5, 700) + 18
        cx -= aw + 6
        L.caja(cx, fy + 2, aw, 22, ch, color="azul" if ch.startswith("Todos") else "blanco",
               rx=11, size=9.5, peso=700, stroke=None if ch.startswith("Todos") else "#c2cbd4")
    L.marca(3, x + w + 10, fy + 13)

    ty = fy + 34
    talleres = [("Alfabetización digital para personas mayores", "UV 12 Los Castaños · La Florida",
                 "sáb 18 oct · 10:00", 24, 35, "En curso", "verde"),
                ("Taller de huerto comunitario", "UV 7 El Parrón · Puente Alto",
                 "sáb 18 oct · 15:30", 11, 30, "Programado", "azul"),
                ("Ciberseguridad para dirigentes vecinales", "Sede CITT · La Florida",
                 "dom 19 oct · 11:00", 0, 25, "Sin señal en terreno", "naranja")]
    n = 3 if not compacta else 2
    for i, (nom, lugar, fecha, act, cup, est, col) in enumerate(talleres[:n]):
        by = ty + i * 86
        L.caja(x, by, w, 76, None, color="blanco", rx=3)
        L.ph_imagen(x + 10, by + 10, 74, 56)
        L.txt(x + 96, by + 26, nom, size=11.5, peso=700)
        L.txt(x + 96, by + 42, lugar, size=9.5, fill=SUAVE)
        L.txt(x + 96, by + 58, fecha, size=9.5, fill=SUAVE)
        aw = ancho(est, 9, 700) + 16
        L.caja(x + w - aw - 12, by + 10, aw, 18, est, color=col, rx=9, size=9, peso=700)
        L.txt(x + w - 12, by + 44, "Quórum %d / %d" % (act, cup), size=9.5, peso=700, anchor="end")
        L.barra_progreso(x + w - 160, by + 50, 148, act / cup, h=8)
        L.caja(x + w - 262, by + 46, 88, 22, "Ver taller", color="azul", rx=3, size=9.5, peso=700)
    L.marca(4, x - 10, ty + 38)
    L.marca(5, x + w + 10, ty + 54)

    fin = ty + n * 86 + 6
    notas = [
        (1, "Alta de taller", ["Visible solo para los roles admin", "y coordinator (Auth0)."]),
        (2, "Indicadores del caso", ["Los tres problemas del enunciado:", "demora, encuestas y papel."]),
        (3, "Filtros rápidos", ["'Sin sincronizar' deja a la vista", "lo capturado sin señal."]),
        (4, "Tarjeta de taller", ["Nombre, unidad vecinal, comuna", "y horario. Una por actividad."]),
        (5, "Barra de quórum", ["Acreditados sobre cupos.", "Sube con cada lectura de QR."]),
    ]
    return notas, fin - y


def web_login(L, x, y, w, compacta):
    pw = 420 if not compacta else min(440, w - 40)
    px = x + (w - pw) / 2
    py = y + 20
    L.caja(px, py, pw, 300, None, color="blanco", rx=3)
    L.caja(px + pw / 2 - 18, py + 20, 36, 36, "QR", color="azul", rx=4, size=11, peso=700)
    L.txt(px + pw / 2, py + 78, "Entrar a SyncPass", size=15, peso=700, anchor="middle")
    L.txt(px + pw / 2, py + 96, "El acceso lo gestiona Amazon Cognito", size=10, fill=SUAVE,
          anchor="middle")
    L.caja(px + 24, py + 114, pw - 48, 36, "Continuar con Cognito", color="azul", rx=3,
           size=11.5, peso=700)
    L.marca(1, px - 10, py + 132)
    L.linea(px + 24, py + 166, px + pw - 24, py + 166, stroke="#dde3ea")
    L.rect(px + pw / 2 - 10, py + 158, 20, 16, fill="#ffffff")
    L.txt(px + pw / 2, py + 170, "o", size=9.5, fill=SUAVE, anchor="middle")
    L.caja(px + 24, py + 182, pw - 48, 32, "Crear una cuenta de participante", color="blanco",
           rx=3, size=11, stroke="#6c8ebf")
    L.txt(px + 24, py + 236, "Grupos del user pool que definen la vista:", size=9.5, peso=700,
          fill=SUAVE)
    for i, (rol, col) in enumerate(GRUPOS):
        gy = py + 244 + (i % 2) * 20
        gx = px + 24 + (i // 2) * (pw - 48) / 2
        aw = ancho(rol, 9, 700) + 16
        L.caja(gx, gy, aw, 18, rol, color=col, rx=9, size=9, peso=700)
        L.txt(gx + aw + 6, gy + 13, PERFILES[rol], size=8.5, fill=SUAVE)
    L.txt(px + 24, py + 290, "Recuperar contraseña y MFA los resuelve Cognito.", size=9,
          fill=SUAVE)
    L.marca(2, px + pw + 10, py + 253)

    fy = py + 358
    L.txt(x, fy - 14, "Flujo de autenticación (OAuth 2.0 con Amazon Cognito)", size=11, peso=700,
          fill=SUAVE)
    nota_w = 0 if compacta else 206
    disponible = w - (nota_w + 18 if nota_w else 0)
    gap = 30
    bw = (disponible - 3 * gap) / 4
    pasos = [("SyncPass", "redirige al login"), ("Cognito", "Hosted UI del user pool"),
             ("Callback", "entrega los tokens JWT"), ("SyncPass", "lee cognito:groups")]
    for i, (t1, t2) in enumerate(pasos):
        bx = x + i * (bw + gap)
        L.caja(bx, fy, bw, 48, [t1, t2], color="azul" if i % 2 == 0 else "morado", rx=3, size=10)
        if i:
            L.conector([(bx - gap, fy + 24), (bx, fy + 24)])
    L.marca(3, x + bw + gap / 2, fy + 66)
    L.guia([(x + bw + gap / 2, fy + 56), (x + bw + gap / 2, fy + 48)])
    if nota_w:
        L.nota(x + w - nota_w, fy - 10, nota_w,
               ["SyncPass no almacena claves.", "Las cuentas viven en el user", "pool y la app solo recibe",
                "un token firmado."], titulo="Restricción del caso")
    return [
        (1, "Un solo botón de acceso", ["Abre la Hosted UI de Cognito.", "La app nunca pide la contraseña."]),
        (2, "Cuatro grupos, cuatro vistas", ["admin, coordinador, monitor y", "participante."]),
        (3, "El rol llega en el token", ["La app arma el menú con el", "claim cognito:groups."]),
    ], (fy + 80) - y


def web_escaner(L, x, y, w, compacta):
    L.txt(x, y + 14, "Acreditación por código QR", size=14, peso=700)
    L.txt(x, y + 30, "Taller en curso: Alfabetización digital · UV 12 Los Castaños", size=10, fill=SUAVE)
    L.caja(x + w - 150, y + 2, 150, 26, "Cambiar de taller", color="blanco", rx=3, size=10, stroke="#c2cbd4")

    izq_w = w * (0.5 if compacta else 0.46)
    cy = y + 48
    L.caja(x, cy, izq_w, 300, None, color="blanco", rx=3)
    L.txt(x + 12, cy + 20, "Cámara del dispositivo", size=10.5, peso=700)
    vx, vy, vw, vh = x + 12, cy + 30, izq_w - 24, 212
    L.rect(vx, vy, vw, vh, fill="#2f3b4a", stroke="#314354", sw=1)
    lado = min(vw * 0.5, vh * 0.72)
    L.qr(vx + vw / 2 - lado / 2, vy + vh / 2 - lado / 2, lado, semilla=3)
    e = 22
    for (ex, ey, sx, sy) in [(vx + 16, vy + 16, 1, 1), (vx + vw - 16, vy + 16, -1, 1),
                             (vx + 16, vy + vh - 16, 1, -1), (vx + vw - 16, vy + vh - 16, -1, -1)]:
        L.linea(ex, ey, ex + e * sx, ey, stroke="#ffffff", sw=2)
        L.linea(ex, ey, ex, ey + e * sy, stroke="#ffffff", sw=2)
    L.linea(vx + 16, vy + vh / 2, vx + vw - 16, vy + vh / 2, stroke="#82b366", sw=1.4, dash="6 4")
    L.marca(1, x - 10, vy + 18)
    pasos = "1. Enfocar el QR   ·   2. Confirmar el nombre   ·   3. Siguiente persona"
    L.txt(x + 12, cy + 262, pasos, size=9.5, fill=SUAVE)
    L.txt(x + 12, cy + 284, "Sin cámara disponible: búsqueda manual por RUT", size=9.5, fill=SUAVE)

    dx = x + izq_w + 18
    dw = w - izq_w - 18
    L.caja(dx, cy, dw, 122, None, color="verde", rx=3)
    L.txt(dx + 14, cy + 26, "Asistente acreditado", size=12, peso=700)
    L.txt(dx + 14, cy + 46, "PART-LF-0241 · Vecina participante B", size=10.5, mono=True)
    L.txt(dx + 14, cy + 64, "Marcación 10:04:18 · lectura en 2,1 s", size=10, fill=SUAVE)
    L.caja(dx + 14, cy + 78, 160, 24, "Guardado en Room local", color="blanco", rx=3, size=9.5, peso=700)
    L.caja(dx + 182, cy + 78, 110, 24, "is_synced = 0", color="naranja", rx=3, size=9.5, peso=700)
    L.marca(2, dx + 300, cy + 90)
    L.guia([(dx + 292, cy + 90), (dx + 289, cy + 90)])
    L.caja(dx, cy + 134, dw, 32, "Siguiente asistente", color="azul", rx=3, size=11, peso=700)

    ty = cy + 186
    L.txt(dx, ty, "Últimas lecturas del taller", size=11, peso=700)
    filas = [("10:04:18", "PART-LF-0241", "Local", "naranja"),
             ("10:03:02", "PART-LF-0188", "Enviado", "verde"),
             ("10:02:15", "PART-LF-0132", "Enviado", "verde"),
             ("10:01:04", "PART-LF-0098", "Enviado", "verde")]
    for i, (hora, pid, est, col) in enumerate(filas):
        fy = ty + 10 + i * 26
        L.rect(dx, fy, dw, 24, fill="#ffffff" if i % 2 else "#fafbfc", stroke="#dde3ea", sw=1)
        L.txt(dx + 10, fy + 16, hora, size=9.5, mono=True)
        L.txt(dx + 76, fy + 16, pid, size=9.5, mono=True)
        aw = ancho(est, 9, 700) + 14
        L.caja(dx + dw - aw - 8, fy + 3, aw, 18, est, color=col, rx=9, size=9, peso=700)
    L.marca(3, dx + dw + 10, ty + 22)
    fin = max(cy + 300, ty + 10 + 4 * 26)
    return [
        (1, "Visor con marco de enfoque", ["CameraX + ML Kit en Android.", "El marco guía al monitor."]),
        (2, "Primero se guarda local", ["Se escribe en Room/SQLite con", "is_synced = 0 y se envía después."]),
        (3, "Estado por lectura", ["'Local' = todavía en el equipo.", "'Enviado' = ya está en el servidor."]),
    ], fin - y


def web_encuesta(L, x, y, w, compacta):
    L.txt(x, y + 14, "Encuesta de satisfacción en terreno", size=14, peso=700)
    L.txt(x, y + 30, "Se responde al cerrar el taller, en el mismo recinto", size=10, fill=SUAVE)

    pw = w if compacta else w * 0.58
    py = y + 48
    L.caja(x, py, pw, 304, None, color="blanco", rx=3)
    L.txt(x + 16, py + 26, "1. ¿Cómo evalúas el taller?", size=11.5, peso=700)
    for i in range(5):
        L.estrella(x + 40 + i * 46, py + 64, 18, llena=i < 4)
        L.txt(x + 40 + i * 46, py + 94, str(i + 1), size=9, fill=SUAVE, anchor="middle")
    L.txt(x + 266, py + 70, "4 de 5 · escala Likert", size=10, fill=SUAVE)
    L.marca(1, x - 10, py + 64)

    L.linea(x + 16, py + 108, x + pw - 16, py + 108, stroke="#dde3ea")
    L.txt(x + 16, py + 132, "2. ¿Te sirve para tu vida diaria?", size=11.5, peso=700)
    ox = x + 16
    for i, op in enumerate(["Mucho", "Bastante", "Algo", "Poco", "Nada"]):
        aw = ancho(op, 9.5, 700) + 20
        L.caja(ox, py + 144, aw, 24, op, color="azul" if i == 0 else "blanco", rx=12,
               size=9.5, peso=700, stroke=None if i == 0 else "#c2cbd4")
        ox += aw + 8
    L.marca(2, x - 10, py + 156)

    L.txt(x + 16, py + 196, "3. Comentario (opcional)", size=11.5, peso=700)
    L.campo_wf(x + 16, py + 206, pw - 32, 56, None, "Las clases fueron claras y pude practicar…")
    L.caja(x + 16, py + 272, pw - 32, 30, "Guardar respuesta", color="azul", rx=3, size=11, peso=700)
    L.txt(x + pw - 16, py + 198, "30 segundos por persona", size=9, fill=SUAVE, anchor="end")

    fin = py + 304
    if not compacta:
        rx0 = x + pw + 18
        rw = w - pw - 18
        L.caja(rx0, py, rw, 154, None, color="blanco", rx=3)
        L.txt(rx0 + 14, py + 24, "Avance de la encuesta", size=11, peso=700)
        L.txt(rx0 + 14, py + 52, "27 de 35 asistentes", size=16, peso=700)
        L.barra_progreso(rx0 + 14, py + 62, rw - 28, 27 / 35, h=9)
        L.txt(rx0 + 14, py + 88, "77 % de respuesta en terreno", size=9.5, fill=SUAVE)
        L.linea(rx0 + 14, py + 100, rx0 + rw - 14, py + 100, stroke="#dde3ea")
        L.txt(rx0 + 14, py + 120, "Histórico por correo: 15 %", size=9.5, fill=SUAVE)
        L.txt(rx0 + 14, py + 140, "Meta del caso: sobre 65 %", size=9.5, peso=700, fill=P["verde"][1])
        L.marca(3, rx0 + rw + 10, py + 62)
        L.nota(rx0, py + 174, rw, ["La respuesta también queda en", "la base local si el recinto", "no tiene señal."],
               titulo="Modo sin conexión")
    return [
        (1, "Cinco estrellas", ["Área táctil de 48 dp: se responde", "de pie, sin teclado."]),
        (2, "Pregunta de utilidad", ["Opción única. Alimenta el", "indicador de impacto comunitario."]),
        (3, "Avance en vivo", ["El monitor ve cuánta gente", "falta por responder."]),
    ], fin - y


def web_quorum(L, x, y, w, compacta):
    L.txt(x, y + 14, "Quórum en vivo · Alfabetización digital", size=14, peso=700)
    L.txt(x, y + 30, "UV 12 Los Castaños · La Florida · sáb 18 oct, 10:00", size=10, fill=SUAVE)
    L.caja(x + w - 120, y + 2, 120, 26, "Exportar CSV", color="blanco", rx=3, size=10, stroke="#6c8ebf")

    ky = y + 48
    bw = (w - 28) / 3 if not compacta else (w - 14) / 2
    L.caja(x, ky, bw, 76, None, color="blanco", rx=3)
    L.txt(x + 14, ky + 20, "Acreditados", size=10, peso=700, fill=SUAVE)
    L.txt(x + 14, ky + 46, "24 / 35", size=20, peso=700)
    L.barra_progreso(x + 14, ky + 56, bw - 28, 24 / 35, h=8)
    L.marca(1, x - 10, ky + 38)

    L.caja(x + bw + 14, ky, bw, 76, None, color="blanco", rx=3)
    L.txt(x + bw + 28, ky + 20, "Quórum mínimo", size=10, peso=700, fill=SUAVE)
    L.txt(x + bw + 28, ky + 46, "68,5 %", size=20, peso=700)
    L.caja(x + bw + 28, ky + 54, 96, 18, "Cumplido", color="verde", rx=9, size=9, peso=700)

    if not compacta:
        tx3 = x + 2 * (bw + 14)
        L.caja(tx3, ky, bw, 76, None, color="blanco", rx=3)
        L.txt(tx3 + 14, ky + 20, "Pendiente de envío", size=10, peso=700, fill=SUAVE)
        L.txt(tx3 + 14, ky + 46, "1 marcación", size=20, peso=700)
        L.txt(tx3 + 14, ky + 64, "capturada sin señal", size=9, fill=SUAVE)

    ty = ky + 96
    L.txt(x, ty, "Detalle de marcaciones", size=12, peso=700)
    cols = [("ID participante", 1.5), ("Hora", 0.9), ("Método", 1.0),
            ("GPS del recinto", 1.3), ("Sincronización", 1.2)]
    filas = [
        ["PART-LF-0132", "10:02:15", "QR", ("Válido", "verde"), ("Enviado", "verde")],
        ["PART-LF-0188", "10:03:02", "QR", ("Válido", "verde"), ("Enviado", "verde")],
        ["PART-LF-0241", "10:04:18", "QR", ("Válido", "verde"), ("Local", "naranja")],
        ["PART-LF-0255", "10:06:40", "Manual", ("Fuera de rango", "rojo"), ("Revisar", "rojo")],
    ]
    th = 26
    L.rect(x, ty + 10, w, th, fill="#eceff3", stroke="#9aa5b1", sw=1)
    total = sum(c[1] for c in cols)
    xs, acc = [], x
    for _, pe in cols:
        xs.append(acc)
        acc += w * pe / total
    for i, (tit, _) in enumerate(cols):
        L.txt(xs[i] + 10, ty + 27, tit.upper(), size=8.5, peso=700, fill=SUAVE)
    for r, fila in enumerate(filas):
        fy = ty + 10 + th * (r + 1)
        L.rect(x, fy, w, 26, fill="#ffffff" if r % 2 else "#fafbfc", stroke="#dde3ea", sw=1)
        for i, celda in enumerate(fila):
            if isinstance(celda, tuple):
                aw = ancho(celda[0], 9, 700) + 14
                L.caja(xs[i] + 10, fy + 4, aw, 18, celda[0], color=celda[1], rx=9, size=9, peso=700)
            else:
                L.txt(xs[i] + 10, fy + 17, celda, size=9.5, mono=(i < 2), peso=700 if i == 0 else 400)
    L.marca(2, (x - 10) if compacta else (xs[3] - 8), ty + 10 + th * 4 + 13)
    L.marca(3, x + w + 10, ty + 10 + th * 3 + 13)
    pie = ty + 10 + th * 5
    L.txt(x, pie + 22, "Mostrando 4 de 24 marcaciones", size=9.5, fill=SUAVE)
    L.caja(x + w - 150, pie + 8, 70, 22, "Anterior", color="blanco", rx=3, size=9.5, stroke="#c2cbd4")
    L.caja(x + w - 74, pie + 8, 74, 22, "Siguiente", color="azul", rx=3, size=9.5, peso=700)
    return [
        (1, "Quórum sobre cupos", ["El coordinador ve el mismo dato", "desde la oficina, en vivo."]),
        (2, "Validación por GPS", ["Compara la marcación con las", "coordenadas del recinto."]),
        (3, "Estado por fila", ["Enviado, Local o Revisar: se", "sabe qué falta por subir."]),
    ], (pie + 36) - y


def web_sync(L, x, y, w, compacta):
    L.txt(x, y + 14, "Centro de sincronización", size=14, peso=700)
    L.txt(x, y + 30, "Arquitectura offline-first: primero se guarda local, después se envía", size=10, fill=SUAVE)
    L.caja(x + w - 128, y + 2, 128, 26, "Conectado", color="verde", rx=3, size=10.5, peso=700)

    dy = y + 56
    alto_dia = 150 if not compacta else 216
    L.caja(x, dy, w, alto_dia, None, color="blanco", rx=3)
    L.txt(x + 14, dy + 22, "Camino de un registro capturado en terreno", size=11, peso=700)
    cajas = [("App móvil", "Kotlin · Compose", "azul"),
             ("Room / SQLite", "is_synced = 0", "naranja"),
             ("API REST", "POST /sync/lote", "morado"),
             ("Base central", "is_synced = 1", "verde")]
    gap = 46 if not compacta else 30
    if compacta:
        cw2 = (w - 40 - gap) / 2
    else:
        cw2 = (w - 40 - 3 * gap) / 4
    pos = []
    for i, (t, sub, col) in enumerate(cajas):
        if compacta:
            bx = x + 20 + (i % 2) * (cw2 + gap)
            by = dy + 48 + (i // 2) * 76
        else:
            bx = x + 20 + i * (cw2 + gap)
            by = dy + 58
        L.caja(bx, by, cw2, 56, [t, sub], color=col, rx=3, size=10, peso=700)
        pos.append((bx, by, cw2))
    etiquetas = ["guarda", "encola", "confirma"]
    if compacta:
        L.conector([(pos[0][0] + cw2, pos[0][1] + 28), (pos[1][0], pos[1][1] + 28)], etiqueta="guarda")
        L.conector([(pos[1][0] + cw2 / 2, pos[1][1] + 56), (pos[1][0] + cw2 / 2, pos[2][1] + 28),
                    (pos[2][0] + cw2, pos[2][1] + 28)], etiqueta="encola")
        L.conector([(pos[2][0], pos[2][1] + 28), (pos[3][0] + cw2, pos[3][1] + 28)], etiqueta="confirma")
    else:
        for i in range(3):
            bx, by, bw2 = pos[i]
            L.conector([(bx + bw2, by + 28), (bx + bw2 + gap, by + 28)],
                       etiqueta=etiquetas[i], etiqueta_dy=-11, size=9.5)
        L.conector([(pos[3][0] + cw2 / 2, pos[3][1]), (pos[3][0] + cw2 / 2, dy + 42),
                    (pos[1][0] + cw2 / 2, dy + 42), (pos[1][0] + cw2 / 2, pos[1][1])],
                   color="#82b366", dash="5 3", size=9.5, etiqueta_dy=-9,
                   etiqueta="el servidor confirma y marca los registros como enviados")
    L.marca(1, x - 10, pos[1][1] + 28)

    ty = dy + alto_dia + 26
    L.txt(x, ty, "Cola local pendiente de envío", size=12, peso=700)
    filas = [("Marcación QR", "PART-LF-0241 · 10:04:18", "UV 12 Los Castaños"),
             ("Encuesta 1-5", "PART-LF-0188 · 11:02:41", "UV 12 Los Castaños"),
             ("Marcación QR", "PART-PA-0012 · 15:48:10", "UV 7 El Parrón")]
    for i, (tipo, ref, lugar) in enumerate(filas):
        fy = ty + 12 + i * 34
        L.caja(x, fy, w, 30, None, color="blanco", rx=3)
        L.caja(x + 8, fy + 6, 96, 18, tipo, color="naranja", rx=3, size=9, peso=700)
        L.txt(x + 116, fy + 20, ref, size=9.5, mono=True)
        L.txt(x + w - 12, fy + 20, lugar, size=9.5, fill=SUAVE, anchor="end")
    by2 = ty + 12 + 3 * 34 + 10
    L.caja(x, by2, w if compacta else w * 0.56, 34, "Sincronizar 3 registros ahora",
           color="azul", rx=3, size=11.5, peso=700)
    if not compacta:
        L.txt(x + w * 0.56 + 18, by2 + 15, "Reintento automático cada 15 minutos", size=9.5, fill=SUAVE)
        L.txt(x + w * 0.56 + 18, by2 + 29, "mientras haya conexión", size=9.5, fill=SUAVE)
    L.marca(2, x - 10, by2 + 17)
    return [
        (1, "Nada se pierde", ["El registro vive en el equipo", "hasta que el servidor confirma."]),
        (2, "Envío por lote", ["Un solo viaje con todo lo", "acumulado al volver la señal."]),
    ], (by2 + 44) - y


# ----------------------------------------------------------------------
# Vistas móviles  ->  (notas, alto)
# ----------------------------------------------------------------------
def mov_panel(L, x, y, w):
    y0 = y
    y = L.barra_app(x, y, w, "SyncPass", "Monitor M. Vega", chip=("SIN SEÑAL", False))
    L.caja(x + 10, y + 10, w - 20, 72, None, color="amarillo", rx=3)
    L.txt(x + 20, y + 28, "Taller de hoy", size=9.5, peso=700, fill=SUAVE)
    L.txt(x + 20, y + 46, "Alfabetización digital", size=11.5, peso=700)
    L.txt(x + 20, y + 60, "UV 12 Los Castaños · 10:00", size=9, fill=SUAVE)
    L.txt(x + w - 20, y + 46, "24/35", size=12, peso=700, anchor="end")
    L.barra_progreso(x + 20, y + 68, w - 40, 24 / 35, h=7)
    L.marca(1, x + w + 10, y + 18)

    L.caja(x + 10, y + 94, w - 20, 54, ["Escanear código QR", "acreditación en menos de 5 s"],
           color="azul", rx=3, size=11, peso=700)
    L.marca(2, x - 10, y + 121)
    L.caja(x + 10, y + 158, (w - 28) / 2, 44, ["Encuesta", "1 a 5 estrellas"], color="blanco", rx=3, size=9.5)
    L.caja(x + 18 + (w - 28) / 2, y + 158, (w - 28) / 2, 44, ["Cola local", "3 pendientes"],
           color="naranja", rx=3, size=9.5)

    L.txt(x + 14, y + 228, "Mis talleres asignados", size=10.5, peso=700)
    for i, (nom, dato) in enumerate([("Alfabetización digital", "hoy 10:00 · 24/35"),
                                     ("Huerto comunitario", "hoy 15:30 · 11/30"),
                                     ("Ciberseguridad vecinal", "dom 11:00 · 0/25")]):
        by = y + 238 + i * 58
        L.caja(x + 10, by, w - 20, 50, None, color="blanco", rx=3)
        L.ph_imagen(x + 18, by + 8, 34, 34)
        L.txt(x + 60, by + 22, nom, size=10, peso=700)
        L.txt(x + 60, by + 36, dato, size=9, fill=SUAVE)
        L.txt(x + w - 18, by + 30, "›", size=13, peso=700, fill=SUAVE, anchor="end")
    L.marca(3, x - 10, y + 238 + 2 * 58 + 25)

    fin = y + 238 + 3 * 58 + 10
    L.caja(x + 10, fin, w - 20, 62, None, color="blanco", rx=3)
    L.txt(x + 20, fin + 20, "Resumen de la jornada", size=10, peso=700)
    L.txt(x + 20, fin + 38, "38 acreditaciones · 29 encuestas", size=9.5, fill=SUAVE)
    L.txt(x + 20, fin + 52, "3 registros esperando señal", size=9.5, fill=SUAVE)
    return [
        (1, "Estado de la conexión", ["Siempre visible: avisa que se", "está trabajando sin señal."]),
        (2, "Botón principal grande", ["48 dp de alto, al alcance del", "pulgar para usar con una mano."]),
        (3, "Lista en una columna", ["Una tarjeta por taller, sin", "tablas que se desborden."]),
    ], (fin + 72) - y0


def mov_login(L, x, y, w):
    y0 = y
    y = L.barra_app(x, y, w, "Acceso", None)
    L.caja(x + w / 2 - 20, y + 36, 40, 40, "QR", color="azul", rx=4, size=11, peso=700)
    L.txt(x + w / 2, y + 100, "SyncPass", size=15, peso=700, anchor="middle")
    L.txt(x + w / 2, y + 118, "CITT Plaza Vespucio", size=9.5, fill=SUAVE, anchor="middle")
    L.caja(x + 24, y + 144, w - 48, 46, "Continuar con Cognito", color="azul", rx=3,
           size=11, peso=700)
    L.marca(1, x - 10, y + 167)
    L.txt(x + w / 2, y + 210, "o", size=9.5, fill=SUAVE, anchor="middle")
    L.caja(x + 24, y + 222, w - 48, 40, "Crear cuenta de participante", color="blanco", rx=3,
           size=10, stroke="#6c8ebf")
    L.caja(x + 24, y + 284, w - 48, 62, None, color="amarillo", rx=3)
    L.txt(x + 34, y + 304, "Sesión persistente", size=10, peso=700)
    L.txt(x + 34, y + 320, "El refresh token queda en el equipo", size=9, fill=SUAVE)
    L.txt(x + 34, y + 336, "para entrar sin señal en terreno", size=9, fill=SUAVE)
    L.marca(2, x - 10, y + 315)
    L.txt(x + 24, y + 378, "Grupos del user pool:", size=9.5, peso=700, fill=SUAVE)
    rx0 = x + 24
    for rol, col in GRUPOS[:3]:
        aw = ancho(rol, 9, 700) + 16
        L.caja(rx0, y + 386, aw, 18, rol, color=col, rx=9, size=9, peso=700)
        rx0 += aw + 6
    return [
        (1, "El mismo Cognito de la web", ["Se abre la Hosted UI en el", "navegador del sistema."]),
        (2, "Sesión guardada", ["Sin esto el monitor no podría", "entrar en una zona sin cobertura."]),
    ], (y + 420) - y0


def mov_escaner(L, x, y, w):
    y0 = y
    y = L.barra_app(x, y, w, "Acreditar", "Alfabetización digital", atras=True,
                    chip=("SIN SEÑAL", False))
    vx, vy, vw, vh = x + 10, y + 10, w - 20, 290
    L.rect(vx, vy, vw, vh, fill="#2f3b4a", stroke="#314354", sw=1)
    lado = vw * 0.56
    L.qr(vx + vw / 2 - lado / 2, vy + 76, lado, semilla=5)
    e = 26
    for (ex, ey, sx, sy) in [(vx + 18, vy + 18, 1, 1), (vx + vw - 18, vy + 18, -1, 1),
                             (vx + 18, vy + vh - 18, 1, -1), (vx + vw - 18, vy + vh - 18, -1, -1)]:
        L.linea(ex, ey, ex + e * sx, ey, stroke="#ffffff", sw=2)
        L.linea(ex, ey, ex, ey + e * sy, stroke="#ffffff", sw=2)
    L.txt(vx + vw / 2, vy + 48, "Apunta al QR del participante", size=10, fill="#ffffff", anchor="middle")
    L.marca(1, x + w + 10, vy + 20)

    ry = vy + vh + 12
    L.caja(x + 10, ry, w - 20, 88, None, color="verde", rx=3)
    L.txt(x + 22, ry + 24, "Acreditado en 2,1 s", size=11.5, peso=700)
    L.txt(x + 22, ry + 44, "PART-LF-0241", size=10, mono=True)
    L.txt(x + 22, ry + 62, "Guardado local · is_synced = 0", size=9, fill=SUAVE)
    L.caja(x + w - 98, ry + 22, 80, 24, "Deshacer", color="blanco", rx=3, size=9.5, stroke="#9aa5b1")
    L.marca(2, x - 10, ry + 44)
    L.caja(x + 10, ry + 100, w - 20, 44, "Siguiente asistente", color="azul", rx=3,
           size=11, peso=700)
    return [
        (1, "Cámara casi a pantalla completa", ["Marco de enfoque grande para", "leer de pie y con prisa."]),
        (2, "Confirmación inmediata", ["Identificador, tiempo de lectura", "y botón para deshacer un error."]),
    ], (ry + 154) - y0


def mov_encuesta(L, x, y, w):
    y0 = y
    y = L.barra_app(x, y, w, "Encuesta", "Cierre del taller", atras=True)
    L.txt(x + 16, y + 32, "¿Cómo evalúas el taller?", size=11.5, peso=700)
    for i in range(5):
        L.estrella(x + 40 + i * 58, y + 80, 23, llena=i < 4)
    L.txt(x + w / 2, y + 118, "4 de 5 · Bueno", size=10, fill=SUAVE, anchor="middle")
    L.marca(1, x - 10, y + 80)
    L.linea(x + 16, y + 132, x + w - 16, y + 132, stroke="#dde3ea")
    L.txt(x + 16, y + 156, "¿Te sirve para tu vida diaria?", size=11, peso=700)
    for i, op in enumerate(["Mucho", "Bastante", "Algo", "Poco"]):
        bx = x + 16 + (i % 2) * ((w - 40) / 2 + 8)
        by = y + 168 + (i // 2) * 42
        L.caja(bx, by, (w - 40) / 2, 34, op, color="azul" if i == 0 else "blanco", rx=3,
               size=10, peso=700, stroke=None if i == 0 else "#c2cbd4")
    L.marca(2, x - 10, y + 185)
    L.txt(x + 16, y + 274, "Comentario (opcional)", size=10.5, peso=700)
    L.campo_wf(x + 16, y + 284, w - 32, 54, None, "Las clases fueron claras…")
    L.caja(x + 16, y + 352, w - 32, 46, "Enviar respuesta", color="azul", rx=3, size=11.5, peso=700)
    L.caja(x + 16, y + 410, w - 32, 38, "Se guarda local si no hay señal", color="amarillo",
           rx=3, size=9.5)
    L.marca(3, x - 10, y + 429)
    return [
        (1, "Estrellas de 44 dp", ["Se responde con el pulgar,", "sin abrir el teclado."]),
        (2, "Opciones en dos columnas", ["Caben en pantalla sin", "desplazarse."]),
        (3, "También funciona sin señal", ["La respuesta entra en la misma", "cola de sincronización."]),
    ], (y + 462) - y0


def mov_quorum(L, x, y, w):
    y0 = y
    y = L.barra_app(x, y, w, "Quórum", "Alfabetización digital", atras=True)
    L.caja(x + 10, y + 12, w - 20, 96, None, color="blanco", rx=3)
    L.txt(x + 22, y + 34, "Acreditados", size=10, peso=700, fill=SUAVE)
    L.txt(x + 22, y + 64, "24 / 35", size=22, peso=700)
    L.barra_progreso(x + 22, y + 76, w - 44, 24 / 35, h=9)
    L.txt(x + 22, y + 100, "68,5 % · quórum cumplido", size=9.5, fill=SUAVE)
    L.marca(1, x + w + 10, y + 76)
    L.txt(x + 16, y + 136, "Últimas marcaciones", size=10.5, peso=700)
    filas = [("10:04:18", "PART-LF-0241", "Local", "naranja"),
             ("10:03:02", "PART-LF-0188", "Enviado", "verde"),
             ("10:02:15", "PART-LF-0132", "Enviado", "verde"),
             ("10:01:04", "PART-LF-0098", "Enviado", "verde"),
             ("09:58:22", "PART-LF-0077", "Enviado", "verde")]
    for i, (hora, pid, est, col) in enumerate(filas):
        by = y + 146 + i * 44
        L.caja(x + 10, by, w - 20, 38, None, color="blanco", rx=3)
        L.txt(x + 20, by + 17, pid, size=9.5, mono=True, peso=700)
        L.txt(x + 20, by + 31, "QR · " + hora, size=9, fill=SUAVE)
        aw = ancho(est, 9, 700) + 14
        L.caja(x + w - aw - 18, by + 10, aw, 18, est, color=col, rx=9, size=9, peso=700)
    L.marca(2, x - 10, y + 165)
    fin = y + 146 + 5 * 44 + 6
    L.caja(x + 10, fin, w - 20, 38, "Ver las 24 marcaciones", color="blanco",
           rx=3, size=10, stroke="#6c8ebf")
    return [
        (1, "El quórum, arriba de todo", ["Es lo primero que necesita", "saber el monitor en terreno."]),
        (2, "Tarjetas en vez de tabla", ["La tabla de escritorio se", "apila en una columna."]),
    ], (fin + 48) - y0


def mov_sync(L, x, y, w):
    y0 = y
    y = L.barra_app(x, y, w, "Sincronizar", "3 registros en cola", atras=True,
                    chip=("CONECTADO", True))
    L.caja(x + 10, y + 12, w - 20, 68, None, color="verde", rx=3)
    L.txt(x + 22, y + 34, "Señal recuperada", size=11.5, peso=700)
    L.txt(x + 22, y + 52, "Se puede enviar lo capturado", size=9.5, fill=SUAVE)
    L.txt(x + 22, y + 68, "en la UV 12 Los Castaños", size=9.5, fill=SUAVE)
    L.marca(1, x - 10, y + 46)
    L.txt(x + 16, y + 108, "Cola local (Room / SQLite)", size=10.5, peso=700)
    filas = [("Marcación QR", "PART-LF-0241 · 10:04"), ("Encuesta 1-5", "PART-LF-0188 · 11:02"),
             ("Marcación QR", "PART-PA-0012 · 15:48")]
    for i, (tipo, ref) in enumerate(filas):
        by = y + 118 + i * 48
        L.caja(x + 10, by, w - 20, 42, None, color="blanco", rx=3)
        L.caja(x + 18, by + 11, 90, 18, tipo, color="naranja", rx=3, size=9, peso=700)
        L.txt(x + 118, by + 18, ref, size=9, mono=True)
        L.txt(x + 118, by + 32, "is_synced = 0", size=8.5, fill=SUAVE, mono=True)
    L.marca(2, x + w + 10, y + 139)
    by2 = y + 118 + 3 * 48 + 10
    L.caja(x + 10, by2, w - 20, 46, "Enviar lote ahora", color="azul", rx=3, size=11.5, peso=700)
    L.caja(x + 10, by2 + 58, w - 20, 58, None, color="blanco", rx=3)
    L.txt(x + 22, by2 + 78, "Último envío correcto", size=9.5, peso=700, fill=SUAVE)
    L.txt(x + 22, by2 + 96, "09:12 · 14 registros · sin errores", size=9.5)
    L.marca(3, x - 10, by2 + 87)
    return [
        (1, "Aviso de reconexión", ["Aparece solo cuando vuelve", "la señal."]),
        (2, "Se ve qué hay en cola", ["Tipo de registro y la marca", "is_synced = 0 de la tabla local."]),
        (3, "Historial de envíos", ["Deja constancia para la", "auditoría del proyecto."]),
    ], (by2 + 126) - y0


# ----------------------------------------------------------------------
VISTAS = [
    dict(slug="panel", titulo="Vista 1 · Panel de talleres", activo="Panel", menu="general",
         web=web_panel, mov=mov_panel, url="syncpass.duocuc.cl/panel", nav=0),
    dict(slug="login", titulo="Vista 2 · Inicio de sesión con Amazon Cognito", activo="Panel",
         menu="general", web=web_login, mov=mov_login, url="syncpass.duocuc.cl/login", nav=0),
    dict(slug="escaner", titulo="Vista 3 · Acreditación por código QR", activo="Acreditar",
         menu="general", web=web_escaner, mov=mov_escaner, url="syncpass.duocuc.cl/acreditar", nav=1),
    dict(slug="encuesta", titulo="Vista 4 · Encuesta de satisfacción", activo="Encuestas",
         menu="general", web=web_encuesta, mov=mov_encuesta, url="syncpass.duocuc.cl/encuestas", nav=2),
    dict(slug="quorum", titulo="Vista 5 · Quórum en vivo del taller", activo="Quórum",
         menu="general", web=web_quorum, mov=mov_quorum,
         url="syncpass.duocuc.cl/talleres/18/quorum", nav=0),
    dict(slug="sync", titulo="Vista 6 · Centro de sincronización", activo="Sincronizar",
         menu="general", web=web_sync, mov=mov_sync, url="syncpass.duocuc.cl/sincronizar", nav=3),

    # Una vista por rol: el grupo de Cognito decide menú, ruta y acciones.
    dict(slug="rol-admin", titulo="Rol 1 · Administrador (equipo administrativo)", activo="Panel", menu="admin",
         web=web_rol_admin, mov=None, url="syncpass.duocuc.cl/admin",
         formatos=("web", "tablet")),
    dict(slug="rol-coordinador", titulo="Rol 2 · Docente / Coordinador(a) de proyecto", activo="Panel",
         menu="coordinador", web=web_rol_coordinador, mov=None,
         url="syncpass.duocuc.cl/coordinacion", formatos=("web", "tablet")),
    dict(slug="rol-monitor", titulo="Rol 3 · Estudiante voluntario / Monitor", activo="Terreno", menu="monitor",
         web=web_rol_monitor, mov=mov_rol_monitor, url="syncpass.duocuc.cl/terreno",
         nav=0, nav_items=["Terreno", "Acreditar", "Encuesta", "Cola"]),
    dict(slug="rol-participante", titulo="Rol 4 · Beneficiario comunitario / Asistente", activo="Mis talleres",
         menu="participante", web=web_rol_participante, mov=mov_rol_participante,
         url="syncpass.duocuc.cl/mi-cuenta", nav=0,
         nav_items=["Mis talleres", "Mi QR", "Encuestas"]),

    # Los dos CRUD de negocio.
    dict(slug="crud-talleres", titulo="CRUD por API · Talleres", activo="Talleres",
         menu="coordinador", web=web_crud_talleres, mov=None,
         url="syncpass.duocuc.cl/talleres", formatos=("web", "tablet")),
    dict(slug="crud-local", titulo="CRUD local · Borradores de encuesta", activo="Encuestas",
         menu="monitor", web=web_crud_local, mov=mov_crud_local,
         url="syncpass.duocuc.cl/encuestas/borradores", nav=2, badge=("Sin conexión", False),
         nav_items=["Terreno", "Acreditar", "Encuesta", "Cola"]),
]


def medir_web(dibujo, cw, rw, pad, compacta):
    """Primera pasada en un lienzo de descarte, para ajustar el alto a su contenido."""
    tmp = Lienzo(10, 10)
    notas, alto = dibujo(tmp, rw + pad, 0, cw - (rw + 2 * pad), compacta)
    return notas, alto


def alto_notas(notas):
    return 20 + sum(28 + 13 * len(n[2]) if len(n) > 2 else 32 for n in notas)


def hoja_web(v):
    W = 1420
    sx, sy = 24, 76
    sw = W - NOTAS_W - 56
    rw, pad = 150, 18
    menu = MENUS[v["menu"]]
    notas, alto = medir_web(v["web"], sw, rw, pad, False)
    ch = max(46 + alto + 28, 46 + 60 + 36 * len(menu))
    sh = 30 + ch
    H = max(sy + sh + 56, 110 + alto_notas(notas) + 150)
    L = Lienzo(W, H)
    L.titulo_hoja(v["titulo"], "Versión escritorio · 1280 px o más · React + Vite",
                  "Mockup")
    badge, ok = v.get("badge", ("Conectado", True))
    cx, cy, cw, _ = L.marco_navegador(sx, sy, sw, sh, v["url"], badge,
                                      color="verde" if ok else "rojo")
    hy = cabecera_app(L, cx, cy, cw, v["activo"])
    menu_lateral(L, cx, hy, rw, ch - 46, v["activo"], menu, PIES[v["menu"]])
    v["web"](L, cx + rw + pad, hy + 14, cw - rw - 2 * pad, False)
    L.txt(sx, sy + sh + 20, "Escritorio · 1280 × 800 px", size=10, fill=SUAVE)
    fin = panel_notas(L, W - NOTAS_W - 8, 100, NOTAS_W, notas)
    L.leyenda(W - NOTAS_W - 8, fin + 16, [("azul", "Acción principal"), ("verde", "Estado correcto"),
                                          ("naranja", "Pendiente de envío"), ("rojo", "Requiere revisión")])
    return L


def hoja_tablet(v):
    W = 1150
    tx, ty = 24, 80
    tw, b, pad = 700, 16, 14
    uw = tw - 2 * b
    notas, alto = medir_web(v["web"], uw, 0, pad, True)
    ch = 30 + 46 + 32 + alto + 24
    th = ch + 2 * b
    H = max(ty + th + 60, 110 + alto_notas(notas) + 200)
    L = Lienzo(W, H)
    L.titulo_hoja(v["titulo"], "Versión tablet · 768 a 1024 px · la misma página, en dos columnas",
                  "Mockup")
    ux, uy, uw2, uh = L.marco_tablet(tx, ty, tw, th, 'Tablet 10" · 820 × 1180 px')
    badge, ok = v.get("badge", ("Conectado", True))
    cx, cy, cw, _ = L.marco_navegador(ux, uy, uw2, uh, v["url"], badge,
                                      color="verde" if ok else "rojo")
    hy = cabecera_app(L, cx, cy, cw, v["activo"], compacta=True)
    ty2 = tabs_compactas(L, cx, hy, cw, v["activo"], MENUS[v["menu"]])
    v["web"](L, cx + pad, ty2 + 10, cw - 2 * pad, True)
    fin = panel_notas(L, W - NOTAS_W - 8, 104, NOTAS_W, notas)
    L.nota(W - NOTAS_W - 8, fin + 16, NOTAS_W - 10,
           ["El menú lateral pasa a pestañas", "horizontales y las grillas bajan", "a dos columnas.",
            "Es la misma página, no otra."], titulo="Qué cambia respecto al escritorio")
    return L


def hoja_movil(v):
    W = 790
    px, py = 24, 80
    pw, b, nav = 390, 10, 44
    tmp = Lienzo(10, 10)
    notas, alto = v["mov"](tmp, b, 20, pw - 2 * b)
    ph = 2 * b + 20 + alto + nav + 8
    H = max(py + ph + 60, 110 + alto_notas(notas) + 190)
    L = Lienzo(W, H)
    L.titulo_hoja(v["titulo"], "Versión móvil · app Android nativa en Kotlin con Jetpack Compose",
                  "Mockup")
    ux, uy, uw, _ = L.marco_telefono(px, py, pw, ph, "Android · ancho 390 dp (recorte de la pantalla)",
                                     offline=v["slug"] in ("panel", "escaner", "rol-monitor", "crud-local"))
    v["mov"](L, ux, uy, uw)
    L.nav_inferior(ux, py + ph - b - nav, uw,
                   v.get("nav_items", ["Inicio", "Acreditar", "Encuesta", "Sync"]),
                   activo=v.get("nav", 0), h=nav)
    fin = panel_notas(L, W - NOTAS_W - 8, 104, NOTAS_W, notas)
    L.nota(W - NOTAS_W - 8, fin + 16, NOTAS_W - 10,
           ["Una sola columna, botones de", "48 dp y todo alcanzable con el", "pulgar: el monitor sostiene el",
            "teléfono con una mano."], titulo="Reglas de la versión móvil")
    return L


HOJAS = {"web": hoja_web, "tablet": hoja_tablet, "movil": hoja_movil}


def main():
    if not os.path.isdir(SALIDA):
        os.makedirs(SALIDA)
    n = 0
    for v in VISTAS:
        for formato in v.get("formatos", ("web", "tablet", "movil")):
            L = HOJAS[formato](v)
            nombre = "%s-%s.svg" % (v["slug"], formato)
            L.guardar(os.path.join(SALIDA, nombre), "%s (%s)" % (v["titulo"], formato))
            print("  img/mockups/%s  (%d x %d)" % (nombre, L.w, L.h))
            n += 1
    print("%d dibujos generados." % n)


if __name__ == "__main__":
    main()
