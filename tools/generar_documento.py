# -*- coding: utf-8 -*-
"""
Inserta los dibujos generados dentro de SyncPass-Mockups-y-BD.html, como SVG
en línea, para que el documento se pueda abrir o enviar sin la carpeta img/.

Lo insertado queda entre marcas <!-- dibujo:... --> ... <!-- /dibujo --> y las
secciones nuevas entre <!-- seccion:... --> ... <!-- /seccion -->, así que el
script se puede volver a ejecutar cada vez que cambien los dibujos.

Uso: python tools/generar_documento.py [copia_adicional.html ...]
"""
import io
import base64
import os
import re
import shutil
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOC = os.path.join(RAIZ, "SyncPass-Mockups-y-BD.html")

VISTAS = ["panel", "login", "escaner", "encuesta", "quorum", "sync"]
FORMATOS = [("web", "Escritorio"), ("tablet", "Tablet"), ("movil", "Móvil Android")]

ROLES = [
    ("rol-admin", "Administrador (equipo administrativo)",
     "Usuarios, grupos de Cognito y estado del sistema. Es el perfil que consolida la información."),
    ("rol-coordinador", "Docente / Coordinador(a) de proyecto",
     "Talleres del período, monitores asignados y reportes de impacto."),
    ("rol-monitor", "Estudiante voluntario / Monitor",
     "Su taller del día: acreditar, encuestar y revisar la cola local."),
    ("rol-participante", "Beneficiario comunitario / Asistente",
     "Su código QR, sus talleres y sus encuestas pendientes."),
]

CRUD = [
    ("crud-talleres", "CRUD por API: talleres",
     "Alta, lectura, edición y baja contra la API REST desplegada en EC2."),
    ("crud-proyectos", "CRUD por API: proyectos comunitarios",
     "Alta, lectura, edición y baja de proyectos contra la API REST desplegada en EC2."),
    ("crud-local", "CRUD local: borradores de encuesta",
     "Las mismas cuatro operaciones sobre la base del dispositivo, sin conexión."),
]

INFRA = [
    ("despliegue-ec2", "dibujo-despliegue-ec2.svg", "Despliegue de la API en Amazon EC2",
     "Clientes, Cognito, la instancia EC2 con la API y la base de datos dentro de la VPC."),
    ("base-local", "dibujo-base-local.svg", "Base de datos local del dispositivo",
     "Room/SQLite e IndexedDB: las tablas sobre las que trabaja el CRUD sin conexión."),
]

CSS = """
  /* ---- Dibujos (generados con tools/generar_mockups.py) ---- */
  .dibujo { margin-top: 16px; }
  .dibujo-tabs { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 10px; }
  .dibujo-tabs button { background: var(--fondo); border: 1px solid var(--borde);
    color: var(--texto); border-radius: 6px; padding: 7px 14px; font-size: 13px;
    font-weight: 600; cursor: pointer; min-height: 40px; }
  .dibujo-tabs button[aria-selected="true"] { background: var(--azul);
    border-color: var(--azul); color: #fff; }
  .lamina { border: 1px solid var(--borde); border-radius: 8px; background: #fff;
    overflow-x: auto; padding: 10px; }
  .lamina[hidden] { display: none; }
  .lamina svg { display: block; width: 100%; height: auto; min-width: 700px; }
  .detalle { margin-top: 12px; border: 1px solid var(--borde); border-radius: 8px;
    padding: 10px 14px; background: var(--fondo); }
  .detalle summary { cursor: pointer; font-weight: 600; font-size: 14px; }
  .detalle-cols { display: grid; grid-template-columns: 1.3fr 1fr; gap: 20px;
    margin-top: 12px; }
  @media (max-width: 720px) { .detalle-cols { grid-template-columns: 1fr; } }
  .detalle ul { padding-left: 18px; font-size: 14px; }
  .detalle li { margin-bottom: 5px; }
  .detalle h4 { font-size: 13px; color: var(--suave); text-transform: uppercase;
    letter-spacing: .04em; margin-bottom: 6px; }
  @media print { .dibujo-tabs { display: none; } .lamina[hidden] { display: block; } }
"""

JS = """
  // Pestañas escritorio / tablet / móvil de cada dibujo.
  document.querySelectorAll('.dibujo').forEach(function (bloque) {
    var botones = bloque.querySelectorAll('.dibujo-tabs button');
    botones.forEach(function (boton) {
      boton.addEventListener('click', function () {
        botones.forEach(function (b) { b.setAttribute('aria-selected', String(b === boton)); });
        bloque.querySelectorAll('.lamina').forEach(function (l) {
          l.hidden = l.dataset.formato !== boton.dataset.formato;
        });
      });
    });
  });
"""


def leer_svg(ruta):
    with io.open(ruta, encoding="utf-8") as fh:
        svg = fh.read()
    svg = re.sub(r"<\?xml.*?\?>\s*", "", svg, flags=re.S)
    svg = re.sub(r'(<svg[^>]*?)\swidth="\d+"\s+height="\d+"', r"\1", svg, count=1)
    return svg.strip()


def formatos_de(slug):
    disponibles = []
    for formato, etiqueta in FORMATOS:
        if os.path.isfile(os.path.join(RAIZ, "img", "mockups", "%s-%s.svg" % (slug, formato))):
            disponibles.append((formato, etiqueta))
    return disponibles


def bloque_dibujo(slug, listas=None):
    disponibles = formatos_de(slug)
    partes = ["<!-- dibujo:%s -->" % slug, '<div class="dibujo">',
              '  <div class="dibujo-tabs" role="tablist">']
    for i, (formato, etiqueta) in enumerate(disponibles):
        partes.append('    <button type="button" role="tab" aria-selected="%s" data-formato="%s">%s</button>'
                      % ("true" if i == 0 else "false", formato, etiqueta))
    partes.append("  </div>")
    for i, (formato, _) in enumerate(disponibles):
        ruta = os.path.join(RAIZ, "img", "mockups", "%s-%s.svg" % (slug, formato))
        partes.append('  <div class="lamina" data-formato="%s"%s>' % (formato, "" if i == 0 else " hidden"))
        partes.append(leer_svg(ruta))
        partes.append("  </div>")
    partes.append("</div>")
    if listas:
        partes.append('<details class="detalle">')
        partes.append("  <summary>Qué lleva cada versión, elemento por elemento</summary>")
        partes.append('  <div class="detalle-cols">')
        for titulo, ul in listas:
            partes.append("    <div><h4>%s</h4>%s</div>" % (titulo, ul))
        partes.append("  </div>")
        partes.append("</details>")
    partes.append("<!-- /dibujo -->")
    return "\n".join(partes)


def bloque_bd(slug, archivo):
    return "\n".join(["<!-- dibujo:%s -->" % slug, '<div class="lamina">',
                      leer_svg(os.path.join(RAIZ, "img", archivo)), "</div>", "<!-- /dibujo -->"])


def seccion(id_seccion, titulo, bajada, articulos):
    partes = ['<!-- seccion:%s -->' % id_seccion, '<section id="%s" hidden>' % id_seccion,
              "  <h2>%s</h2>" % titulo, '  <p class="apoyo">%s</p>' % bajada]
    for i, (num, nombre, texto, cuerpo) in enumerate(articulos, 1):
        partes.append('  <article class="vista">')
        partes.append('    <h3><span class="num">%s</span> %s</h3>' % (num, nombre))
        partes.append('    <p class="apoyo">%s</p>' % texto)
        partes.append(cuerpo)
        partes.append("  </article>")
    partes.append("</section>")
    partes.append("<!-- /seccion -->")
    return "\n".join(partes)


def secciones_nuevas():
    roles = seccion(
        "roles", "Una vista por rol",
        "Los cuatro grupos del user pool de Cognito. Cada grupo entra a su propia vista: "
        "el menú, la ruta y las acciones cambian con el rol.",
        [(i + 1, nombre, texto, bloque_dibujo(slug))
         for i, (slug, nombre, texto) in enumerate(ROLES)])
    crud = seccion(
        "crud", "Mantenedores y CRUD local",
        "Dos mantenedores contra la API desplegada en EC2 y borradores en el dispositivo.",
        [(i + 1, nombre, texto, bloque_dibujo(slug))
         for i, (slug, nombre, texto) in enumerate(CRUD)])
    infra = seccion(
        "infra", "Despliegue y base local",
        "Dónde corre la API, cómo se autentica y qué guarda el dispositivo mientras no hay señal.",
        [(i + 1, nombre, texto, bloque_bd(slug, archivo))
         for i, (slug, archivo, nombre, texto) in enumerate(INFRA)])
    return "\n\n".join([roles, crud, infra])


def inyectar(html):
    logo_path = os.path.join(RAIZ, "web-react", "public", "img", "logo-header.svg")
    with io.open(logo_path, encoding="utf-8") as fh:
        logo = base64.b64encode(fh.read().encode("utf-8")).decode("ascii")
    html = re.sub(r'(<div class="marca">\s*)<img src="data:image/svg\+xml;base64,[^"]+" alt="[^"]+">',
                  lambda m: m.group(1) + '<img src="data:image/svg+xml;base64,%s" alt="SyncPass">' % logo,
                  html, count=1)
    html = re.sub(r"<strong>[^<]+</strong> · SyncPass", "<strong>SyncPass</strong>", html, count=1)
    # Mockups de las seis vistas
    for slug in VISTAS:
        ya = re.search(r"<!-- dibujo:%s -->.*?<!-- /dibujo -->" % slug, html, flags=re.S)
        if ya:
            listas = re.findall(r"<h4>(.*?)</h4>(<ul>.*?</ul>)", ya.group(0), flags=re.S)
            html = html[:ya.start()] + bloque_dibujo(slug, listas) + html[ya.end():]
            continue
        m = re.search(r'<div class="dos-col">.*?</div>\s*\n\s*</article>', html, flags=re.S)
        if not m:
            raise SystemExit("No se encontró el bloque de la vista %s" % slug)
        crudo = m.group(0)
        listas = list(zip(re.findall(r"<h4>(.*?)</h4>", crudo, flags=re.S),
                          re.findall(r"<ul>.*?</ul>", crudo, flags=re.S)))
        html = html[:m.start()] + bloque_dibujo(slug, listas) + "\n      </article>" + html[m.end():]

    # Modelos de datos
    if "<!-- dibujo:relacional -->" in html:
        html = re.sub(r"<!-- dibujo:relacional -->.*?<!-- /dibujo -->",
                      lambda _: bloque_bd("relacional", "dibujo-modelo-relacional.svg"),
                      html, flags=re.S)
    else:
        m = re.search(r'<div class="er">.*?</div>', html, flags=re.S)
        if m:
            html = html[:m.start()] + bloque_bd("relacional", "dibujo-modelo-relacional.svg") + html[m.end():]
    for slug, archivo in (("estrella", "dibujo-modelo-estrella.svg"),
                          ("copo", "dibujo-modelo-copo-nieve.svg")):
        if "<!-- dibujo:%s -->" % slug in html:
            html = re.sub(r"<!-- dibujo:%s -->.*?<!-- /dibujo -->" % slug,
                          lambda _, s=slug, a=archivo: bloque_bd(s, a), html, flags=re.S)
        else:
            m = re.search(r'<section id="%s"[^>]*>.*?(<div class="diagrama">.*?</svg></div>)' % slug,
                          html, flags=re.S)
            if m:
                html = html[:m.start(1)] + bloque_bd(slug, archivo) + html[m.end(1):]

    # Secciones nuevas: roles, CRUD e infraestructura
    nuevas = secciones_nuevas()
    if "<!-- seccion:roles -->" in html:
        html = re.sub(r"<!-- seccion:roles -->.*?<!-- /seccion -->\s*\n\s*<!-- seccion:crud -->"
                      r".*?<!-- /seccion -->\s*\n\s*<!-- seccion:infra -->.*?<!-- /seccion -->",
                      lambda _: nuevas, html, flags=re.S)
    else:
        html = html.replace("</main>", nuevas + "\n\n</main>", 1)

    # Pestañas de las secciones nuevas
    for id_seccion, etiqueta in (("roles", "Vistas por rol"), ("crud", "CRUD"),
                                 ("infra", "Despliegue")):
        if 'data-panel="%s"' % id_seccion not in html:
            html = html.replace(
                '<button class="tab" role="tab" aria-selected="false" data-panel="relacional">',
                '<button class="tab" role="tab" aria-selected="false" data-panel="%s">%s</button>\n'
                '      <button class="tab" role="tab" aria-selected="false" data-panel="relacional">'
                % (id_seccion, etiqueta), 1)
    html = html.replace("var paneles = ['mockups', 'relacional', 'estrella', 'copo'];",
                        "var paneles = ['mockups', 'roles', 'crud', 'relacional', 'estrella', "
                        "'copo', 'infra'];")

    # Auth0 quedó reemplazado por Cognito en todo el documento.
    html = html.replace("Acceso por roles mediante Auth0. El formulario de credenciales lo aloja Auth0, no SyncPass.",
                        "Acceso por roles con Amazon Cognito. El formulario de credenciales lo aloja "
                        "el user pool, no SyncPass.")
    html = html.replace("Auth0", "Cognito")
    html = html.replace("--azul: #2452c9; --azul-osc: #1b2b52; --azul-cl: #eef2fd; --cian: #00aeef;",
                        "--azul: #315740; --azul-osc: #20382b; --azul-cl: #edf1e8; --cian: #a76943;")
    html = html.replace("Acceso por roles con Amazon Cognito. El formulario de credenciales lo aloja el user pool, no SyncPass.",
                        "Acceso por roles con Amazon Cognito. El frontend envía las credenciales a la API y recibe la sesión; esta lámina representa el flujo conceptual.")

    if "/* ---- Dibujos" not in html:
        html = html.replace("</style>", CSS + "</style>", 1)
    if "Pestañas escritorio / tablet / móvil" not in html:
        html = html.replace("</body>", "<script>" + JS + "</script>\n</body>", 1)
    return html


def main():
    with io.open(DOC, encoding="utf-8") as fh:
        html = fh.read()
    nuevo = inyectar(html)
    with io.open(DOC, "w", encoding="utf-8") as fh:
        fh.write(nuevo)
    print("  %s  (%d KB)" % (os.path.basename(DOC), len(nuevo.encode("utf-8")) // 1024))
    for destino in sys.argv[1:]:
        shutil.copyfile(DOC, destino)
        print("  copiado a %s" % destino)


if __name__ == "__main__":
    main()
