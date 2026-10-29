# -*- coding: utf-8 -*-
"""
Primitivas de dibujo con el aspecto de draw.io (diagrams.net).

Se usa desde generar_mockups.py y generar_diagramas_bd.py. La idea es dibujar
como se dibuja a mano en draw.io: cajas de borde fino, conectores ortogonales
con punta de flecha, notas amarillas y tipografia Helvetica.
"""
import math

# Paleta por defecto de draw.io (relleno / borde).
P = {
    "azul":   ("#dae8fc", "#6c8ebf"),
    "verde":  ("#d5e8d4", "#82b366"),
    "naranja": ("#ffe6cc", "#d79b00"),
    "rojo":   ("#f8cecc", "#b85450"),
    "morado": ("#e1d5e7", "#9673a6"),
    "amarillo": ("#fff2cc", "#d6b656"),
    "gris":   ("#f5f5f5", "#666666"),
    "blanco": ("#ffffff", "#666666"),
    "oscuro": ("#647687", "#314354"),
}
TINTA = "#1a1a1a"
SUAVE = "#5d6673"
FUENTE = "Helvetica, 'Helvetica Neue', Arial, sans-serif"
MONO = "Consolas, 'Courier New', monospace"


def esc(t):
    return (str(t).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def ancho(t, size, peso=400):
    """Ancho aproximado de un texto en Helvetica."""
    k = 0.54 if peso < 600 else 0.58
    return len(str(t)) * size * k


class Lienzo:
    def __init__(self, w, h, fondo="#ffffff"):
        self.w, self.h = w, h
        self.p = []
        self.fondo = fondo

    # ------------------------------------------------------------------
    # Primitivas
    # ------------------------------------------------------------------
    def rect(self, x, y, w, h, fill="none", stroke=None, rx=0, sw=1, dash=None, op=1):
        a = '<rect x="%.1f" y="%.1f" width="%.1f" height="%.1f" rx="%s" fill="%s"' % (
            x, y, max(w, 0), max(h, 0), rx, fill)
        if stroke:
            a += ' stroke="%s" stroke-width="%s"' % (stroke, sw)
        if dash:
            a += ' stroke-dasharray="%s"' % dash
        if op != 1:
            a += ' opacity="%s"' % op
        self.p.append(a + "/>")

    def circ(self, cx, cy, r, fill="none", stroke=None, sw=1, op=1):
        a = '<circle cx="%.2f" cy="%.2f" r="%.2f" fill="%s"' % (cx, cy, r, fill)
        if stroke:
            a += ' stroke="%s" stroke-width="%s"' % (stroke, sw)
        if op != 1:
            a += ' opacity="%s"' % op
        self.p.append(a + "/>")

    def path(self, d, fill="none", stroke=None, sw=1.2, dash=None, cap="butt", op=1):
        a = '<path d="%s" fill="%s"' % (d, fill)
        if stroke:
            a += ' stroke="%s" stroke-width="%s" stroke-linecap="%s" stroke-linejoin="miter"' % (
                stroke, sw, cap)
        if dash:
            a += ' stroke-dasharray="%s"' % dash
        if op != 1:
            a += ' opacity="%s"' % op
        self.p.append(a + "/>")

    def linea(self, x1, y1, x2, y2, stroke=None, sw=1, dash=None, op=1):
        a = '<line x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f" stroke="%s" stroke-width="%s"' % (
            x1, y1, x2, y2, stroke or "#666666", sw)
        if dash:
            a += ' stroke-dasharray="%s"' % dash
        if op != 1:
            a += ' opacity="%s"' % op
        self.p.append(a + "/>")

    def poly(self, pts, fill="none", stroke=None, sw=1):
        s = " ".join("%.2f,%.2f" % p for p in pts)
        a = '<polygon points="%s" fill="%s"' % (s, fill)
        if stroke:
            a += ' stroke="%s" stroke-width="%s"' % (stroke, sw)
        self.p.append(a + "/>")

    def txt(self, x, y, t, size=12, fill=TINTA, peso=400, anchor="start", mono=False, op=1):
        a = '<text x="%.1f" y="%.1f" font-size="%s" fill="%s" font-weight="%s"' % (
            x, y, size, fill, peso)
        if anchor != "start":
            a += ' text-anchor="%s"' % anchor
        if mono:
            a += ' font-family="%s"' % MONO
        if op != 1:
            a += ' opacity="%s"' % op
        self.p.append(a + ">" + esc(t) + "</text>")

    # ------------------------------------------------------------------
    # Cajas y etiquetas
    # ------------------------------------------------------------------
    def caja(self, x, y, w, h, etiqueta=None, color="blanco", rx=0, size=12,
             peso=400, dash=None, sw=1, fill=None, stroke=None, alineacion="centro",
             col_texto=None):
        """Caja estandar de draw.io. `etiqueta` puede ser str o lista de lineas."""
        f, s = P.get(color, P["blanco"])
        self.rect(x, y, w, h, fill=fill or f, stroke=stroke or s, rx=rx, sw=sw, dash=dash)
        if etiqueta is None:
            return
        lineas = etiqueta if isinstance(etiqueta, (list, tuple)) else [etiqueta]
        alto = len(lineas) * (size + 3) - 3
        y0 = y + h / 2 - alto / 2 + size * 0.86
        for i, ln in enumerate(lineas):
            if alineacion == "centro":
                self.txt(x + w / 2, y0 + i * (size + 3), ln, size=size, peso=peso,
                         anchor="middle", fill=col_texto or TINTA)
            else:
                self.txt(x + 8, y0 + i * (size + 3), ln, size=size, peso=peso,
                         fill=col_texto or TINTA)

    def nota(self, x, y, w, lineas, size=11, titulo=None, color="amarillo"):
        """Nota adhesiva con la esquina doblada, como la forma 'Note' de draw.io."""
        f, s = P[color]
        h = 14 + (len(lineas) + (1 if titulo else 0)) * (size + 5)
        dob = 12
        self.path("M %.1f %.1f H %.1f L %.1f %.1f V %.1f H %.1f Z" % (
            x, y, x + w - dob, x + w, y + dob, y + h, x), fill=f, stroke=s, sw=1)
        self.path("M %.1f %.1f V %.1f H %.1f Z" % (x + w - dob, y, y + dob, x + w),
                  fill="#ffffff", stroke=s, sw=1, op=0.85)
        ty = y + 10 + size
        if titulo:
            self.txt(x + 9, ty, titulo, size=size, peso=700)
            ty += size + 5
        for ln in lineas:
            self.txt(x + 9, ty, ln, size=size, fill=TINTA)
            ty += size + 5
        return h

    def titulo_hoja(self, titulo, subtitulo=None, etiqueta=None):
        """Cartela superior del diagrama (titulo + subtitulo + chip opcional)."""
        self.txt(24, 32, titulo, size=17, peso=700)
        if subtitulo:
            self.txt(24, 52, subtitulo, size=11.5, fill=SUAVE)
        if etiqueta:
            w = ancho(etiqueta, 11, 700) + 22
            self.caja(self.w - w - 24, 16, w, 24, etiqueta, color="azul", rx=3, size=11, peso=700)

    def leyenda(self, x, y, items, size=10.5, titulo="Leyenda"):
        """Caja de leyenda: lista de (color, texto)."""
        w = 10 + max([ancho(t, size) for _, t in items]) + 42
        h = 16 + (len(items) + 1) * 17
        self.rect(x, y, w, h, fill="#ffffff", stroke="#9aa5b1", rx=3, dash="4 3")
        self.txt(x + 10, y + 18, titulo, size=size, peso=700, fill=SUAVE)
        for i, (col, t) in enumerate(items):
            f, s = P[col] if col in P else (col, "#666666")
            yy = y + 28 + i * 17
            self.rect(x + 10, yy, 18, 11, fill=f, stroke=s, rx=2)
            self.txt(x + 34, yy + 9.5, t, size=size)
        return w, h

    # ------------------------------------------------------------------
    # Conectores ortogonales
    # ------------------------------------------------------------------
    def _punta(self, x, y, direccion, color, largo=9, ancho_p=7, hueca=False):
        """Punta de flecha tipo 'block' de draw.io."""
        dx, dy = {"der": (1, 0), "izq": (-1, 0), "abajo": (0, 1), "arriba": (0, -1)}[direccion]
        bx, by = x - dx * largo, y - dy * largo
        px, py = -dy, dx
        pts = [(x, y), (bx + px * ancho_p / 2, by + py * ancho_p / 2),
               (bx - px * ancho_p / 2, by - py * ancho_p / 2)]
        self.poly(pts, fill="#ffffff" if hueca else color, stroke=color, sw=1)

    def conector(self, puntos, color="#6c8ebf", sw=1.2, dash=None, flecha_fin=True,
                 flecha_ini=False, etiqueta=None, etiqueta_pos=0.5, etiqueta_dy=-5,
                 size=10.5, hueca=False, etiqueta_xy=None):
        """Conector ortogonal por waypoints [(x,y), ...]."""
        d = "M " + " L ".join("%.1f %.1f" % p for p in puntos)
        self.path(d, stroke=color, sw=sw, dash=dash)

        def dir_entre(a, b):
            if abs(b[0] - a[0]) >= abs(b[1] - a[1]):
                return "der" if b[0] > a[0] else "izq"
            return "abajo" if b[1] > a[1] else "arriba"

        if flecha_fin:
            self._punta(puntos[-1][0], puntos[-1][1], dir_entre(puntos[-2], puntos[-1]),
                        color, hueca=hueca)
        if flecha_ini:
            self._punta(puntos[0][0], puntos[0][1], dir_entre(puntos[1], puntos[0]),
                        color, hueca=hueca)
        if etiqueta and etiqueta_xy:
            mx, my = etiqueta_xy
            w = ancho(etiqueta, size, 600) + 10
            self.rect(mx - w / 2, my - size * 0.78, w, size + 5, fill="#ffffff", rx=2, op=0.95)
            self.txt(mx, my + size * 0.1, etiqueta, size=size, peso=600, anchor="middle", fill=SUAVE)
        elif etiqueta:
            # Se coloca sobre el tramo mas largo.
            mejor, largo = 0, -1
            for i in range(len(puntos) - 1):
                l = abs(puntos[i + 1][0] - puntos[i][0]) + abs(puntos[i + 1][1] - puntos[i][1])
                if l > largo:
                    mejor, largo = i, l
            a, b = puntos[mejor], puntos[mejor + 1]
            mx = a[0] + (b[0] - a[0]) * etiqueta_pos
            my = a[1] + (b[1] - a[1]) * etiqueta_pos
            w = ancho(etiqueta, size, 600) + 10
            self.rect(mx - w / 2, my + etiqueta_dy - size * 0.78, w, size + 5,
                      fill="#ffffff", rx=2, op=0.95)
            self.txt(mx, my + etiqueta_dy + size * 0.1, etiqueta, size=size, peso=600,
                     anchor="middle", fill=SUAVE)

    def guia(self, puntos, color="#9aa5b1"):
        """Linea guia punteada sin flecha (para unir una nota con su zona)."""
        self.conector(puntos, color=color, sw=1, dash="3 3", flecha_fin=False)

    def marca(self, n, cx, cy, r=11, color="naranja"):
        """Circulo numerado de referencia (callout)."""
        f, s = P[color]
        self.circ(cx, cy, r, fill=f, stroke=s, sw=1.2)
        self.txt(cx, cy + 4, n, size=12, peso=700, anchor="middle")

    # ------------------------------------------------------------------
    # Tablas de entidad estilo ER de draw.io
    # ------------------------------------------------------------------
    def tabla_entidad(self, x, y, w, titulo, filas, color="azul", fh=19, size=10.5,
                      subtitulo=None):
        """
        Tabla de base de datos al estilo draw.io.
        `filas`: lista de (clave, nombre, tipo) donde clave in "", "PK", "FK", "PK,FK", "M".
        Devuelve (x, y, w, h).
        """
        f, s = P[color]
        th = 26 + (12 if subtitulo else 0)
        h = th + len(filas) * fh
        # El ancho crece si el nombre mas largo y su tipo no caben lado a lado.
        if filas:
            mono = 0.60
            necesario = max(44 + len(n) * size * mono + 12 + len(t) * 9 * mono + 7
                            for _, n, t in filas)
            cab = ancho(titulo, 11.5, 700) + 24
            w = max(w, necesario, cab)
        self.rect(x, y, w, h, fill="#ffffff", stroke=s, sw=1.2)
        self.rect(x, y, w, th, fill=f, stroke=s, sw=1.2)
        self.txt(x + w / 2, y + (17 if not subtitulo else 15), titulo, size=11.5, peso=700,
                 anchor="middle")
        if subtitulo:
            self.txt(x + w / 2, y + 28, subtitulo, size=9, fill=SUAVE, anchor="middle")
        for i, fila in enumerate(filas):
            clave, nombre, tipo = fila
            fy = y + th + i * fh
            if i:
                self.linea(x, fy, x + w, fy, stroke="#dde3ea", sw=1)
            if clave:
                col = {"PK": "#b85450", "FK": "#6c8ebf", "PK,FK": "#9673a6", "M": "#82b366"}.get(clave, SUAVE)
                self.txt(x + 7, fy + fh / 2 + 3.5, clave, size=8.5, peso=700, fill=col, mono=True)
            self.txt(x + 44, fy + fh / 2 + 3.5, nombre, size=size, mono=True,
                     peso=700 if clave in ("PK", "PK,FK") else 400)
            self.txt(x + w - 7, fy + fh / 2 + 3.5, tipo, size=9, fill=SUAVE, anchor="end", mono=True)
        return (x, y, w, h)

    def pata_cuervo(self, x, y, direccion, color="#6c8ebf", largo=10, sep=5):
        """Pata de cuervo (N) apuntando hacia dentro de la entidad."""
        dx, dy = {"der": (1, 0), "izq": (-1, 0), "abajo": (0, 1), "arriba": (0, -1)}[direccion]
        bx, by = x - dx * largo, y - dy * largo
        px, py = -dy, dx
        for k in (-1, 0, 1):
            self.linea(x, y, bx + px * sep * k, by + py * sep * k, stroke=color, sw=1.2)

    def uno(self, x, y, direccion, color="#6c8ebf", largo=9, ancho_b=9):
        """Marca de cardinalidad 1 (barra perpendicular)."""
        dx, dy = {"der": (1, 0), "izq": (-1, 0), "abajo": (0, 1), "arriba": (0, -1)}[direccion]
        bx, by = x - dx * largo, y - dy * largo
        px, py = -dy, dx
        self.linea(bx + px * ancho_b / 2, by + py * ancho_b / 2,
                   bx - px * ancho_b / 2, by - py * ancho_b / 2, stroke=color, sw=1.4)

    def relacion(self, puntos, color="#6c8ebf", etiqueta=None, lado_uno="ini",
                 dash=None, size=10, etiqueta_xy=None):
        """
        Conector ER: barra '1' en un extremo y pata de cuervo 'N' en el otro.
        `lado_uno`: 'ini' o 'fin'.
        """
        d = "M " + " L ".join("%.1f %.1f" % p for p in puntos)
        self.path(d, stroke=color, sw=1.2, dash=dash)

        def dir_entre(a, b):
            if abs(b[0] - a[0]) >= abs(b[1] - a[1]):
                return "der" if b[0] > a[0] else "izq"
            return "abajo" if b[1] > a[1] else "arriba"

        d_fin = dir_entre(puntos[-2], puntos[-1])
        d_ini = dir_entre(puntos[1], puntos[0])
        if lado_uno == "ini":
            self.uno(puntos[0][0], puntos[0][1], d_ini, color)
            self.pata_cuervo(puntos[-1][0], puntos[-1][1], d_fin, color)
        else:
            self.pata_cuervo(puntos[0][0], puntos[0][1], d_ini, color)
            self.uno(puntos[-1][0], puntos[-1][1], d_fin, color)
        if etiqueta:
            if etiqueta_xy:
                mx, my = etiqueta_xy
            else:
                mejor, largo = 0, -1
                for i in range(len(puntos) - 1):
                    l = abs(puntos[i + 1][0] - puntos[i][0]) + abs(puntos[i + 1][1] - puntos[i][1])
                    if l > largo:
                        mejor, largo = i, l
                a, b = puntos[mejor], puntos[mejor + 1]
                mx, my = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
            w = ancho(etiqueta, size, 600) + 8
            self.rect(mx - w / 2, my - size, w, size + 4, fill="#ffffff", rx=2, op=0.95)
            self.txt(mx, my + 1, etiqueta, size=size, peso=600, anchor="middle", fill=SUAVE)

    # ------------------------------------------------------------------
    # Piezas de wireframe (estilo mockup de draw.io)
    # ------------------------------------------------------------------
    def ph_texto(self, x, y, w, n=1, h=7, sep=11, ultimo=0.65, color="#c9d1d9"):
        """Lineas grises que representan texto corrido."""
        for i in range(n):
            aw = w * (ultimo if (i == n - 1 and n > 1) else 1.0)
            self.rect(x, y + i * sep, aw, h, fill=color, rx=1.5)

    def ph_imagen(self, x, y, w, h, etiqueta=None):
        """Marco de imagen con la cruz diagonal clasica del wireframe."""
        self.rect(x, y, w, h, fill="#fafbfc", stroke="#9aa5b1", sw=1)
        self.linea(x, y, x + w, y + h, stroke="#c2cbd4", sw=1)
        self.linea(x + w, y, x, y + h, stroke="#c2cbd4", sw=1)
        if etiqueta:
            w2 = ancho(etiqueta, 10) + 12
            self.rect(x + w / 2 - w2 / 2, y + h / 2 - 9, w2, 18, fill="#ffffff", stroke="#9aa5b1", rx=2)
            self.txt(x + w / 2, y + h / 2 + 4, etiqueta, size=10, anchor="middle", fill=SUAVE)

    def boton_wf(self, x, y, w, h, etiqueta, color="azul", size=11, peso=700):
        self.caja(x, y, w, h, etiqueta, color=color, rx=3, size=size, peso=peso)

    def campo_wf(self, x, y, w, h, etiqueta=None, valor=None, size=10.5, icono_txt=None):
        """Campo de formulario: etiqueta arriba, caja con el valor dentro."""
        if etiqueta:
            self.txt(x, y - 5, etiqueta, size=9.5, peso=700, fill=SUAVE)
        self.rect(x, y, w, h, fill="#ffffff", stroke="#9aa5b1", sw=1, rx=2)
        if valor:
            self.txt(x + 8, y + h / 2 + 3.5, valor, size=size, fill=TINTA)
        if icono_txt:
            self.txt(x + w - 9, y + h / 2 + 3.5, icono_txt, size=size, fill=SUAVE, anchor="end")

    def barra_progreso(self, x, y, w, pct, h=9, color="#6c8ebf"):
        self.rect(x, y, w, h, fill="#eceff3", stroke="#c2cbd4", sw=1, rx=2)
        self.rect(x, y, w * max(0.0, min(1.0, pct)), h, fill=color, rx=2)

    def barras(self, x, y, w, h, valores, etiquetas, color="#dae8fc", borde="#6c8ebf"):
        """Gráfico de barras verticales con eje base y valor sobre cada barra."""
        maximo = max(valores) * 1.2 or 1
        paso = w / len(valores)
        bw = min(46, paso * 0.5)
        for i, v in enumerate(valores):
            bh = (v / maximo) * h
            bx = x + i * paso + (paso - bw) / 2
            self.rect(bx, y + h - bh, bw, bh, fill=color, stroke=borde, sw=1)
            self.txt(bx + bw / 2, y + h - bh - 6, v, size=10, peso=700, anchor="middle")
            self.txt(bx + bw / 2, y + h + 15, etiquetas[i], size=9.5, fill=SUAVE, anchor="middle")
        self.linea(x, y + h, x + w, y + h, stroke="#9aa5b1")

    def estrella(self, cx, cy, r, llena=True, color="#d6b656", relleno="#ffd966"):
        pts = []
        for k in range(10):
            a = math.radians(-90 + k * 36)
            rr = r if k % 2 == 0 else r * 0.45
            pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
        self.poly(pts, fill=relleno if llena else "#ffffff", stroke=color, sw=1.2)

    def qr(self, x, y, lado, semilla=7, modulos=21, color="#1a1a1a"):
        """Codigo QR dibujado modulo a modulo (patron determinista, no aleatorio)."""
        m = lado / modulos
        self.rect(x, y, lado, lado, fill="#ffffff", stroke="#666666", sw=1)
        estado = semilla * 7919 + 13

        def siguiente():
            nonlocal estado
            estado = (estado * 1103515245 + 12345) & 0x7FFFFFFF
            return estado / 0x7FFFFFFF

        def localizador(i, j):
            return (i < 7 and j < 7) or (i < 7 and j >= modulos - 7) or (i >= modulos - 7 and j < 7)

        for i in range(modulos):
            for j in range(modulos):
                if localizador(i, j):
                    continue
                pinta = ((i + j) % 2 == 0) if (i == 6 or j == 6) else (siguiente() < 0.47)
                if pinta:
                    self.rect(x + j * m, y + i * m, m, m, fill=color)
        for (oi, oj) in [(0, 0), (0, modulos - 7), (modulos - 7, 0)]:
            self.rect(x + oj * m, y + oi * m, 7 * m, 7 * m, fill=color)
            self.rect(x + (oj + 1) * m, y + (oi + 1) * m, 5 * m, 5 * m, fill="#ffffff")
            self.rect(x + (oj + 2) * m, y + (oi + 2) * m, 3 * m, 3 * m, fill=color)

    def icono_lineas(self, x, y, s, color=SUAVE):
        """Icono de menu (tres lineas)."""
        for i in range(3):
            self.linea(x, y + i * (s / 3), x + s, y + i * (s / 3), stroke=color, sw=1.6)

    # ------------------------------------------------------------------
    # Marcos de dispositivo
    # ------------------------------------------------------------------
    def marco_navegador(self, x, y, w, h, url, etiqueta=None, color="verde"):
        """Ventana de navegador. Devuelve (x, y, w, h) del area de contenido."""
        bh = 30
        self.rect(x, y, w, h, fill="#ffffff", stroke="#666666", sw=1.4, rx=3)
        self.rect(x, y, w, bh, fill="#f0f2f5", stroke="#666666", sw=1.4)
        for i in range(3):
            self.circ(x + 16 + i * 14, y + bh / 2, 4.5, fill="#ffffff", stroke="#9aa5b1", sw=1)
        bw = w - 150
        self.rect(x + 62, y + 7, bw, bh - 14, fill="#ffffff", stroke="#9aa5b1", sw=1, rx=2)
        self.txt(x + 70, y + bh / 2 + 3.5, url, size=10, fill=SUAVE, mono=True)
        if etiqueta:
            ew = ancho(etiqueta, 9.5, 700) + 14
            self.caja(x + w - ew - 10, y + 7, ew, bh - 14, etiqueta, color=color, rx=2,
                      size=9.5, peso=700)
        return (x, y + bh, w, h - bh)

    def marco_tablet(self, x, y, w, h, etiqueta="Tablet 10\" · 820 x 1180"):
        b = 16
        self.rect(x, y, w, h, fill="#f0f2f5", stroke="#666666", sw=1.4, rx=16)
        self.circ(x + w / 2, y + b / 2 + 1, 3, fill="#ffffff", stroke="#9aa5b1", sw=1)
        self.rect(x + b, y + b, w - 2 * b, h - 2 * b, fill="#ffffff", stroke="#9aa5b1", sw=1)
        if etiqueta:
            self.txt(x + w / 2, y + h + 16, etiqueta, size=10, fill=SUAVE, anchor="middle")
        return (x + b, y + b, w - 2 * b, h - 2 * b)

    def marco_telefono(self, x, y, w, h, etiqueta="Android · 390 x 844 dp", offline=False):
        """Telefono. Devuelve (x, y, w, h) del area util bajo la barra de estado."""
        b = 10
        self.rect(x, y, w, h, fill="#f0f2f5", stroke="#666666", sw=1.4, rx=22)
        self.rect(x + b, y + b, w - 2 * b, h - 2 * b, fill="#ffffff", stroke="#9aa5b1", sw=1, rx=12)
        ux, uy, uw = x + b, y + b, w - 2 * b
        # Barra de estado
        self.rect(ux, uy, uw, 20, fill="#f7f8fa", stroke="#c2cbd4", sw=1)
        self.txt(ux + 8, uy + 14, "09:41", size=9, peso=700, fill=SUAVE)
        self.txt(ux + uw - 8, uy + 14, "SIN SENAL  85%" if offline else "5G  85%",
                 size=9, peso=700, fill="#b85450" if offline else SUAVE, anchor="end")
        if etiqueta:
            self.txt(x + w / 2, y + h + 16, etiqueta, size=10, fill=SUAVE, anchor="middle")
        return (ux, uy + 20, uw, h - 2 * b - 20)

    def barra_app(self, x, y, w, titulo, subtitulo=None, atras=False, chip=None, h=38,
                  color="oscuro"):
        """Barra superior de la app movil."""
        f, s = P[color]
        self.rect(x, y, w, h, fill=f, stroke=s, sw=1)
        tx = x + 10
        if atras:
            self.txt(x + 11, y + h / 2 + 4, "<", size=14, peso=700, fill="#ffffff")
            tx = x + 26
        self.txt(tx, y + (h / 2 + 4 if not subtitulo else h / 2 - 1), titulo, size=11.5,
                 peso=700, fill="#ffffff")
        if subtitulo:
            self.txt(tx, y + h / 2 + 12, subtitulo, size=9, fill="#d7dee8")
        if chip:
            etiqueta, ok = chip
            cw = ancho(etiqueta, 9, 700) + 14
            self.caja(x + w - cw - 8, y + h / 2 - 9, cw, 18, etiqueta,
                      color="verde" if ok else "rojo", rx=9, size=9, peso=700)
        return y + h

    def nav_inferior(self, x, y, w, items, activo=0, h=40):
        """Barra de navegacion inferior: lista de etiquetas."""
        self.rect(x, y, w, h, fill="#f7f8fa", stroke="#9aa5b1", sw=1)
        paso = w / len(items)
        for i, it in enumerate(items):
            cx = x + paso * i + paso / 2
            if i:
                self.linea(x + paso * i, y + 6, x + paso * i, y + h - 6, stroke="#dde3ea", sw=1)
            if i == activo:
                self.rect(x + paso * i + 6, y + 5, paso - 12, h - 10, fill=P["azul"][0],
                          stroke=P["azul"][1], sw=1, rx=3)
            self.txt(cx, y + h / 2 + 4, it, size=9.5, peso=700 if i == activo else 400,
                     anchor="middle", fill=TINTA if i == activo else SUAVE)

    # ------------------------------------------------------------------
    def svg(self, titulo):
        cab = ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" '
               'height="%d" role="img" aria-label="%s" font-family="%s">\n<title>%s</title>\n'
               '<rect width="%d" height="%d" fill="%s"/>\n'
               % (self.w, self.h, self.w, self.h, esc(titulo), FUENTE, esc(titulo),
                  self.w, self.h, self.fondo))
        return cab + "\n".join(self.p) + "\n</svg>\n"

    def guardar(self, ruta, titulo):
        with open(ruta, "w", encoding="utf-8") as fh:
            fh.write(self.svg(titulo))
        return ruta


def panel_notas(L, x, y, w, items, titulo="Referencias del dibujo"):
    """
    Columna lateral de notas numeradas, como las anotaciones de un mockup
    en draw.io. `items`: lista de (n, texto) o (n, texto, [detalle, ...]).
    """
    L.txt(x, y, titulo, size=12, peso=700, fill=SUAVE)
    yy = y + 14
    for it in items:
        n, texto = it[0], it[1]
        detalles = it[2] if len(it) > 2 else []
        L.marca(n, x + 11, yy + 11, r=10)
        L.txt(x + 28, yy + 9, texto, size=10.5, peso=700)
        dy = yy + 24
        for d in detalles:
            L.txt(x + 28, dy, d, size=10, fill=SUAVE)
            dy += 13
        yy = max(dy, yy + 28) + 4
    return yy
