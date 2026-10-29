# -*- coding: utf-8 -*-
"""
Genera los dibujos de los modelos de datos de SyncPass con aspecto de draw.io:
tablas con cabecera de color, claves PK/FK, conectores ortogonales y patas de
cuervo para la cardinalidad.

Salida:
  img/dibujo-modelo-relacional.svg   (modelo transaccional, 8 tablas)
  img/dibujo-modelo-estrella.svg     (data warehouse, esquema en estrella)
  img/dibujo-modelo-copo-nieve.svg   (dimensiones normalizadas en jerarquias)

Las columnas son las mismas de database/schema.sql y database/star_schema.sql.

Uso: python tools/generar_diagramas_bd.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from drawio import Lienzo, SUAVE, TINTA, P  # noqa: E402

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SALIDA = os.path.join(RAIZ, "img")

AZUL, VERDE, NARANJA, MORADO, ROJO = "#6c8ebf", "#82b366", "#d79b00", "#9673a6", "#b85450"


# ----------------------------------------------------------------------
# 1. Modelo relacional (OLTP)
# ----------------------------------------------------------------------
def modelo_relacional():
    W, H = 1530, 760
    L = Lienzo(W, H)
    L.titulo_hoja(
        "SyncPass · Modelo relacional de la base de datos transaccional",
        "8 tablas en tercera forma normal · acreditación por QR, encuestas y sincronización offline-first",
        "OLTP")

    ancho_t = 248
    roles = L.tabla_entidad(40, 84, ancho_t, "roles", [
        ("PK", "id_rol", "INT"),
        ("", "nombre_rol", "VARCHAR(50)"),
        ("", "descripcion", "VARCHAR(255)"),
        ("", "creado_en", "TIMESTAMP"),
    ], color="gris")

    usuarios = L.tabla_entidad(40, 218, ancho_t, "usuarios", [
        ("PK", "id_usuario", "INT"),
        ("FK", "id_rol", "INT"),
        ("", "cognito_sub", "VARCHAR(64)"),
        ("", "nombre_completo", "VARCHAR(120)"),
        ("", "correo", "VARCHAR(150)"),
        ("", "telefono", "VARCHAR(20)"),
        ("", "activo", "BOOLEAN"),
        ("", "creado_en", "TIMESTAMP"),
    ], color="azul", subtitulo="espejo local del user pool de Cognito")

    unidades = L.tabla_entidad(336, 84, ancho_t, "unidades_vecinales", [
        ("PK", "id_unidad", "INT"),
        ("", "nombre_unidad", "VARCHAR(120)"),
        ("", "comuna", "VARCHAR(80)"),
        ("", "direccion", "VARCHAR(200)"),
        ("", "latitud_referencial", "DECIMAL"),
        ("", "longitud_referencial", "DECIMAL"),
        ("", "contacto_comunitario", "VARCHAR(120)"),
    ], color="verde")

    proyectos = L.tabla_entidad(336, 300, ancho_t, "proyectos_comunitarios", [
        ("PK", "id_proyecto", "INT"),
        ("FK", "id_coordinador", "INT"),
        ("FK", "id_unidad", "INT"),
        ("", "titulo", "VARCHAR(160)"),
        ("", "area_tematica", "ENUM"),
        ("", "fecha_inicio", "DATE"),
        ("", "fecha_termino", "DATE"),
        ("", "estado", "ENUM"),
    ], color="verde")

    eventos = L.tabla_entidad(632, 196, ancho_t, "eventos_talleres", [
        ("PK", "id_evento", "INT"),
        ("FK", "id_proyecto", "INT"),
        ("", "nombre_actividad", "VARCHAR(160)"),
        ("", "fecha_hora", "DATETIME"),
        ("", "cupos_maximos", "INT"),
        ("", "recinto", "VARCHAR(150)"),
        ("", "latitud", "DECIMAL"),
        ("", "longitud", "DECIMAL"),
        ("", "estado", "ENUM"),
    ], color="naranja", subtitulo="cada sesión que se hace en terreno")

    asistencia = L.tabla_entidad(928, 84, ancho_t, "asistencia_marcaciones", [
        ("PK", "id_asistencia", "INT"),
        ("FK", "id_evento", "INT"),
        ("FK", "id_participante", "INT"),
        ("FK", "id_monitor", "INT"),
        ("", "timestamp_marcacion", "TIMESTAMP"),
        ("", "latitud_marcacion", "DECIMAL"),
        ("", "longitud_marcacion", "DECIMAL"),
        ("", "estado_validacion", "ENUM"),
        ("", "is_synced", "BOOLEAN"),
        ("", "synced_at", "TIMESTAMP"),
    ], color="morado", subtitulo="una fila por lectura de QR")

    encuestas = L.tabla_entidad(928, 366, ancho_t, "encuestas_satisfaccion", [
        ("PK", "id_encuesta", "INT"),
        ("FK", "id_evento", "INT"),
        ("FK", "id_participante", "INT"),
        ("", "satisfaccion_estrellas", "TINYINT"),
        ("", "utilidad_taller", "ENUM"),
        ("", "comentario", "TEXT"),
        ("", "fecha_envio", "TIMESTAMP"),
        ("", "is_synced", "BOOLEAN"),
        ("", "synced_at", "TIMESTAMP"),
    ], color="morado", subtitulo="1 a 5 estrellas, respondida en terreno")

    participantes = L.tabla_entidad(1244, 214, ancho_t, "participantes", [
        ("PK", "id_participante", "INT"),
        ("", "identificador_anonimo", "VARCHAR(64)"),
        ("", "nombre_ficticio", "VARCHAR(120)"),
        ("", "tipo_participante", "ENUM"),
        ("", "codigo_qr", "VARCHAR(255)"),
        ("", "estado_inscripcion", "ENUM"),
    ], color="rojo", subtitulo="datos anónimos (restricción 5.1 del caso)")

    def dcha(t):
        return t[0] + t[2]

    def medio_y(t, frac=0.5):
        return t[1] + t[3] * frac

    # roles 1---N usuarios
    L.relacion([(roles[0] + ancho_t / 2, roles[1] + roles[3]),
                (usuarios[0] + ancho_t / 2, usuarios[1])], etiqueta="tiene")
    # usuarios 1---N proyectos (coordinador)
    L.relacion([(dcha(usuarios), medio_y(usuarios, 0.62)),
                (308, medio_y(usuarios, 0.62)), (308, medio_y(proyectos, 0.3)),
                (proyectos[0], medio_y(proyectos, 0.3))], etiqueta="coordina")
    # unidades 1---N proyectos
    L.relacion([(unidades[0] + ancho_t / 2, unidades[1] + unidades[3]),
                (proyectos[0] + ancho_t / 2, proyectos[1])], etiqueta="aloja")
    # proyectos 1---N eventos
    L.relacion([(dcha(proyectos), medio_y(proyectos, 0.25)),
                (604, medio_y(proyectos, 0.25)), (604, medio_y(eventos, 0.75)),
                (eventos[0], medio_y(eventos, 0.75))], etiqueta="agenda")
    # eventos 1---N asistencia
    L.relacion([(dcha(eventos), medio_y(eventos, 0.25)),
                (900, medio_y(eventos, 0.25)), (900, medio_y(asistencia, 0.72)),
                (asistencia[0], medio_y(asistencia, 0.72))], etiqueta="registra",
               etiqueta_xy=(900, medio_y(eventos, 0.25) - 12))
    # eventos 1---N encuestas
    L.relacion([(dcha(eventos), medio_y(eventos, 0.78)),
                (884, medio_y(eventos, 0.78)), (884, medio_y(encuestas, 0.3)),
                (encuestas[0], medio_y(encuestas, 0.3))], etiqueta="evalúa",
               etiqueta_xy=(884, medio_y(encuestas, 0.3) - 14))
    # participantes 1---N asistencia
    L.relacion([(participantes[0], medio_y(participantes, 0.2)),
                (1206, medio_y(participantes, 0.2)), (1206, medio_y(asistencia, 0.35)),
                (dcha(asistencia), medio_y(asistencia, 0.35))], etiqueta="acredita",
               etiqueta_xy=(1206, medio_y(asistencia, 0.35) + 34))
    # participantes 1---N encuestas
    L.relacion([(participantes[0], medio_y(participantes, 0.8)),
                (1212, medio_y(participantes, 0.8)), (1212, medio_y(encuestas, 0.6)),
                (dcha(encuestas), medio_y(encuestas, 0.6))], etiqueta="responde",
               etiqueta_xy=(1212, medio_y(encuestas, 0.6) - 60))
    # usuarios (monitor) 1---N asistencia, por el carril superior
    L.relacion([(usuarios[0] + 40, usuarios[1]), (usuarios[0] + 40, 66),
                (asistencia[0] + 60, 66), (asistencia[0] + 60, asistencia[1])],
               color=AZUL, dash="6 3", etiqueta="monitor que escanea")

    # Leyenda y notas en el espacio libre inferior izquierdo
    L.leyenda(40, 452, [("gris", "Catálogo"), ("azul", "Personas del equipo"),
                        ("verde", "Territorio y proyectos"), ("naranja", "Actividad en terreno"),
                        ("morado", "Captura en terreno"), ("rojo", "Participantes (anónimos)")],
              titulo="Qué guarda cada grupo de tablas")

    L.nota(280, 600, 300, [
        "is_synced = 0  →  el registro está solo",
        "en el teléfono (Room / SQLite).",
        "is_synced = 1  →  ya viajó al servidor",
        "central y synced_at guarda cuándo.",
    ], titulo="Las dos columnas que hacen el offline")

    L.nota(610, 600, 300, [
        "UNIQUE (id_evento, id_participante)",
        "en asistencia y en encuestas:",
        "nadie se acredita dos veces ni",
        "responde dos veces el mismo taller.",
    ], titulo="Reglas que impiden duplicados")

    L.nota(940, 600, 320, [
        "PK: clave primaria   FK: clave foránea",
        "La barra ( | ) marca el lado 1 y la pata",
        "de cuervo el lado N de cada relación.",
    ], titulo="Cómo leer el diagrama", color="gris")
    return L, "dibujo-modelo-relacional.svg", "Modelo relacional de SyncPass"


# ----------------------------------------------------------------------
# 2. Esquema en estrella
# ----------------------------------------------------------------------
def modelo_estrella():
    W, H = 1340, 880
    L = Lienzo(W, H)
    L.titulo_hoja(
        "SyncPass · Data warehouse en esquema de estrella",
        "Una tabla de hechos al centro y cinco dimensiones directas, sin normalizar",
        "Estrella")

    at = 236
    fact = L.tabla_entidad(530, 360, 290, "fact_asistencia_impacto", [
        ("PK", "id_hecho", "INT"),
        ("FK", "id_tiempo", "INT"),
        ("FK", "id_proyecto_taller", "INT"),
        ("FK", "id_territorio", "INT"),
        ("FK", "id_participante", "INT"),
        ("FK", "id_conectividad", "INT"),
        ("M", "asistencias_efectivas", "INT"),
        ("M", "tiempo_acreditacion_seg", "DECIMAL"),
        ("M", "calificacion_satisfaccion", "TINYINT"),
        ("M", "horas_participacion", "DECIMAL"),
    ], color="naranja", subtitulo="tabla de hechos · grano: una acreditación")

    tiempo = L.tabla_entidad(552, 96, at, "dim_tiempo", [
        ("PK", "id_tiempo", "INT"),
        ("", "fecha_completa", "DATE"),
        ("", "nombre_mes", "VARCHAR(20)"),
        ("", "semestre", "SMALLINT"),
        ("", "anio", "SMALLINT"),
        ("", "dia_semana", "VARCHAR(15)"),
    ], color="azul")

    territorio = L.tabla_entidad(80, 300, at, "dim_territorio", [
        ("PK", "id_territorio", "INT"),
        ("", "unidad_vecinal", "VARCHAR(120)"),
        ("", "comuna", "VARCHAR(80)"),
        ("", "region", "VARCHAR(80)"),
        ("", "tipo_recinto", "VARCHAR(80)"),
    ], color="azul")

    taller = L.tabla_entidad(1020, 300, at, "dim_proyecto_taller", [
        ("PK", "id_proyecto_taller", "INT"),
        ("", "nombre_proyecto", "VARCHAR(160)"),
        ("", "area_tematica", "VARCHAR(80)"),
        ("", "nombre_actividad", "VARCHAR(160)"),
        ("", "docente_coordinador", "VARCHAR(120)"),
    ], color="azul")

    participante = L.tabla_entidad(180, 640, at, "dim_participante", [
        ("PK", "id_participante", "INT"),
        ("", "tipo_participante", "VARCHAR(50)"),
        ("", "estado_inscripcion", "VARCHAR(50)"),
        ("", "rango_edad_referencial", "VARCHAR(50)"),
    ], color="azul")

    conectividad = L.tabla_entidad(920, 640, at, "dim_conectividad", [
        ("PK", "id_conectividad", "INT"),
        ("", "modo_registro", "VARCHAR(50)"),
        ("", "protocolo_sync", "VARCHAR(50)"),
    ], color="azul")

    fx, fy, fw, fh = fact
    # Radios de la estrella: de cada dimensión al borde más cercano de los hechos.
    L.relacion([(tiempo[0] + at / 2, tiempo[1] + tiempo[3]), (fx + fw / 2, fy)],
               etiqueta="ocurre en", lado_uno="ini")
    L.relacion([(territorio[0] + at, territorio[1] + territorio[3] / 2),
                (fx, fy + fh * 0.3)], etiqueta="se realiza en", lado_uno="ini")
    L.relacion([(taller[0], taller[1] + taller[3] / 2), (fx + fw, fy + fh * 0.3)],
               etiqueta="pertenece a", lado_uno="ini")
    L.relacion([(participante[0] + at / 2, participante[1]), (fx + fw * 0.28, fy + fh)],
               etiqueta="acredita a", lado_uno="ini")
    L.relacion([(conectividad[0] + at / 2, conectividad[1]), (fx + fw * 0.72, fy + fh)],
               etiqueta="registrado vía", lado_uno="ini")

    L.nota(80, 96, 300, [
        "Las dimensiones describen el contexto",
        "(cuándo, dónde, de quién, cómo) y la",
        "tabla de hechos guarda lo que se mide.",
        "Una sola unión para cada pregunta:",
        "por eso la consulta es rápida.",
    ], titulo="Cómo se lee la estrella")

    L.nota(1020, 96, 288, [
        "¿Cuántas personas acreditamos por",
        "comuna y mes?",
        "¿Cuánto demora la acreditación por",
        "modo de registro (con o sin señal)?",
        "¿Qué satisfacción promedio tiene",
        "cada área temática?",
    ], titulo="Preguntas que responde", color="verde")

    L.leyenda(80, 790, [("naranja", "Tabla de hechos (medidas)"), ("azul", "Dimensión")],
              titulo="Leyenda")
    L.txt(560, 806, "M = medida numérica  ·  PK = clave primaria  ·  FK = clave foránea",
          size=11, fill=SUAVE)
    L.txt(560, 826, "Grano del hecho: una fila por participante acreditado en un taller.",
          size=11, fill=SUAVE)
    return L, "dibujo-modelo-estrella.svg", "Esquema en estrella de SyncPass"


# ----------------------------------------------------------------------
# 3. Esquema en copo de nieve
# ----------------------------------------------------------------------
def modelo_copo_nieve():
    W, H = 1520, 940
    L = Lienzo(W, H)
    L.titulo_hoja(
        "SyncPass · Data warehouse en copo de nieve",
        "Las mismas dimensiones de la estrella, pero divididas en sus jerarquías naturales",
        "Copo de nieve")

    at = 214
    fact = L.tabla_entidad(620, 420, 280, "fact_asistencia_impacto", [
        ("PK", "id_hecho", "INT"),
        ("FK", "id_tiempo", "INT"),
        ("FK", "id_taller", "INT"),
        ("FK", "id_territorio", "INT"),
        ("FK", "id_participante", "INT"),
        ("FK", "id_conectividad", "INT"),
        ("M", "asistencias_efectivas", "INT"),
        ("M", "tiempo_acreditacion_seg", "DECIMAL"),
        ("M", "calificacion_satisfaccion", "TINYINT"),
    ], color="naranja", subtitulo="tabla de hechos")
    fx, fy, fw, fh = fact

    # --- Jerarquía temporal: tiempo -> mes -> semestre -> año ---------
    tiempo = L.tabla_entidad(648, 230, at, "dim_tiempo", [
        ("PK", "id_tiempo", "INT"),
        ("FK", "id_mes", "INT"),
        ("", "fecha_completa", "DATE"),
        ("", "dia_semana", "VARCHAR(15)"),
    ], color="azul")
    mes = L.tabla_entidad(648, 112, at, "dim_mes", [
        ("PK", "id_mes", "INT"),
        ("FK", "id_semestre", "INT"),
        ("", "nombre_mes", "VARCHAR(20)"),
    ], color="morado")
    semestre = L.tabla_entidad(906, 112, at, "dim_semestre", [
        ("PK", "id_semestre", "INT"),
        ("FK", "id_anio", "INT"),
        ("", "numero_semestre", "SMALLINT"),
    ], color="morado")
    anio = L.tabla_entidad(1164, 112, at, "dim_anio", [
        ("PK", "id_anio", "INT"),
        ("", "anio", "SMALLINT"),
    ], color="morado")

    L.relacion([(tiempo[0] + at / 2, tiempo[1] + tiempo[3]), (fx + fw / 2, fy)], lado_uno="ini")
    L.relacion([(mes[0] + at / 2, mes[1] + mes[3]), (tiempo[0] + at / 2, tiempo[1])], lado_uno="ini")
    L.relacion([(semestre[0], semestre[1] + 40), (mes[0] + at, mes[1] + 40)], lado_uno="ini")
    L.relacion([(anio[0], anio[1] + 40), (semestre[0] + at, semestre[1] + 40)], lado_uno="ini")

    # --- Jerarquía territorial: territorio -> comuna -> región --------
    territorio = L.tabla_entidad(330, 420, at, "dim_territorio", [
        ("PK", "id_territorio", "INT"),
        ("FK", "id_comuna", "INT"),
        ("", "unidad_vecinal", "VARCHAR(120)"),
        ("", "tipo_recinto", "VARCHAR(80)"),
    ], color="azul")
    comuna = L.tabla_entidad(70, 420, at, "dim_comuna", [
        ("PK", "id_comuna", "INT"),
        ("FK", "id_region", "INT"),
        ("", "nombre_comuna", "VARCHAR(80)"),
    ], color="morado")
    region = L.tabla_entidad(70, 285, at, "dim_region", [
        ("PK", "id_region", "INT"),
        ("", "nombre_region", "VARCHAR(80)"),
    ], color="morado")

    L.relacion([(territorio[0] + at, territorio[1] + 40), (fx, fy + 40)], lado_uno="ini")
    L.relacion([(comuna[0] + at, comuna[1] + 40), (territorio[0], territorio[1] + 40)], lado_uno="ini")
    L.relacion([(region[0] + at / 2, region[1] + region[3]), (comuna[0] + at / 2, comuna[1])],
               lado_uno="ini")

    # --- Jerarquía académica: taller -> proyecto -> área --------------
    taller = L.tabla_entidad(1024, 420, at, "dim_taller", [
        ("PK", "id_taller", "INT"),
        ("FK", "id_proyecto", "INT"),
        ("", "nombre_actividad", "VARCHAR(160)"),
        ("", "cupos_maximos", "INT"),
    ], color="azul")
    proyecto = L.tabla_entidad(1282, 420, at, "dim_proyecto", [
        ("PK", "id_proyecto", "INT"),
        ("FK", "id_area", "INT"),
        ("", "nombre_proyecto", "VARCHAR(160)"),
        ("", "docente_coordinador", "VARCHAR(120)"),
    ], color="morado")
    area = L.tabla_entidad(1282, 285, at, "dim_area_tematica", [
        ("PK", "id_area", "INT"),
        ("", "area_tematica", "VARCHAR(80)"),
    ], color="morado")

    L.relacion([(taller[0], taller[1] + 40), (fx + fw, fy + 40)], lado_uno="ini")
    L.relacion([(proyecto[0], proyecto[1] + 40), (taller[0] + at, taller[1] + 40)], lado_uno="ini")
    L.relacion([(area[0] + at / 2, area[1] + area[3]), (proyecto[0] + at / 2, proyecto[1])],
               lado_uno="ini")

    # --- Dimensiones que no se normalizan -----------------------------
    participante = L.tabla_entidad(330, 680, at, "dim_participante", [
        ("PK", "id_participante", "INT"),
        ("", "tipo_participante", "VARCHAR(50)"),
        ("", "estado_inscripcion", "VARCHAR(50)"),
        ("", "rango_edad_referencial", "VARCHAR(50)"),
    ], color="azul")
    conectividad = L.tabla_entidad(1024, 680, at, "dim_conectividad", [
        ("PK", "id_conectividad", "INT"),
        ("", "modo_registro", "VARCHAR(50)"),
        ("", "protocolo_sync", "VARCHAR(50)"),
    ], color="azul")

    L.relacion([(participante[0] + at, participante[1] + 30), (fx + fw * 0.3, fy + fh)],
               lado_uno="ini")
    L.relacion([(conectividad[0], conectividad[1] + 30), (fx + fw * 0.7, fy + fh)],
               lado_uno="ini")

    L.nota(70, 112, 300, [
        "Cada jerarquía se separa en tablas:",
        "territorio → comuna → región,",
        "taller → proyecto → área temática,",
        "tiempo → mes → semestre → año.",
        "Se repite menos texto, pero cada",
        "consulta necesita más uniones.",
    ], titulo="Qué cambia respecto a la estrella")

    L.nota(620, 790, 330, [
        "Estrella: consultas más simples y rápidas,",
        "con datos repetidos en las dimensiones.",
        "Copo de nieve: menos repetición y mejor",
        "mantenimiento, con más JOIN por consulta.",
        "Para los informes de Vinculación con el",
        "Medio basta con la estrella.",
    ], titulo="Cuál conviene", color="verde")

    L.leyenda(70, 790, [("naranja", "Tabla de hechos"), ("azul", "Dimensión principal"),
                        ("morado", "Subdimensión de la jerarquía")], titulo="Leyenda")
    L.txt(1024, 806, "Las flechas van de la tabla padre (1)", size=11, fill=SUAVE)
    L.txt(1024, 824, "a la tabla hija (N), igual que las", size=11, fill=SUAVE)
    L.txt(1024, 842, "claves foráneas del script SQL.", size=11, fill=SUAVE)
    return L, "dibujo-modelo-copo-nieve.svg", "Esquema en copo de nieve de SyncPass"


# ----------------------------------------------------------------------
# 4. Despliegue de la API en EC2
# ----------------------------------------------------------------------
def despliegue_ec2():
    W, H = 1400, 800
    L = Lienzo(W, H)
    L.titulo_hoja(
        "SyncPass \u00b7 Despliegue de la API en Amazon EC2",
        "Clientes, autenticaci\u00f3n con Cognito y API REST con su base de datos dentro de la VPC",
        "Infraestructura")

    L.caja(40, 150, 230, 230, None, color="gris", rx=3, dash="5 3")
    L.txt(155, 174, "Clientes", size=12, peso=700, anchor="middle")
    L.caja(60, 194, 190, 70, ["Navegador", "React + Vite", "(Vercel)"], color="azul", rx=3, size=10.5)
    L.caja(60, 282, 190, 70, ["App Android", "Kotlin + Compose", "(base local Room)"],
           color="azul", rx=3, size=10.5)

    L.caja(540, 92, 280, 74, ["Amazon Cognito \u00b7 user pool", "grupos: admin, coordinador,",
                              "monitor, participante"], color="naranja", rx=3, size=10.5)
    L.txt(680, 186, "Hosted UI + tokens JWT", size=10, fill=SUAVE, anchor="middle")

    L.caja(400, 220, 700, 330, None, color="blanco", rx=6, dash="6 4", stroke="#6c8ebf")
    L.txt(420, 246, "VPC 10.0.0.0/16 \u00b7 regi\u00f3n sa-east-1", size=11.5, peso=700, fill=SUAVE)

    L.caja(424, 262, 330, 270, None, color="gris", rx=4, dash="4 3")
    L.txt(440, 284, "Subred p\u00fablica 10.0.1.0/24", size=10, peso=700, fill=SUAVE)
    L.caja(444, 298, 290, 130, ["Instancia EC2 t3.micro", "Ubuntu 22.04",
                                "Nginx (443) \u2192 Node/Express", "PM2 mantiene el proceso"],
           color="verde", rx=3, size=10.5)
    L.caja(444, 440, 290, 70, ["Security group web", "entrada 443 y 22",
                               "salida a la base de datos"], color="amarillo", rx=3, size=10)

    L.caja(778, 262, 300, 270, None, color="gris", rx=4, dash="4 3")
    L.txt(794, 284, "Subred privada 10.0.2.0/24", size=10, peso=700, fill=SUAVE)
    L.caja(798, 298, 260, 100, ["Amazon RDS \u00b7 MySQL 8", "base syncpass", "sin IP p\u00fablica"],
           color="morado", rx=3, size=10.5)
    L.caja(798, 410, 260, 70, ["Security group base", "entrada 3306 solo desde",
                               "el grupo de la EC2"], color="amarillo", rx=3, size=10)

    L.conector([(250, 214), (322, 214), (322, 118), (540, 118)], etiqueta="1. login",
               size=10, etiqueta_xy=(430, 112))
    L.conector([(540, 150), (300, 150), (300, 244), (250, 244)], color="#d79b00",
               etiqueta="2. tokens JWT", size=10, etiqueta_xy=(300, 196))
    L.conector([(250, 317), (444, 317)], etiqueta="3. HTTPS + token", size=10)
    L.conector([(590, 298), (590, 166)], color="#d79b00", etiqueta="4. valida la firma (JWKS)",
               size=10, etiqueta_xy=(700, 236))
    L.conector([(734, 348), (798, 348)], etiqueta="5. SQL", size=10)

    L.nota(40, 420, 330, [
        "1. El cliente abre la Hosted UI de Cognito.",
        "2. Cognito devuelve los tokens al cliente.",
        "3. Cada llamada a la API lleva el token.",
        "4. La API valida la firma contra Cognito.",
        "5. Reci\u00e9n ah\u00ed se consulta la base de datos.",
    ], titulo="Orden de una petici\u00f3n")

    L.nota(400, 580, 330, [
        "git pull en la instancia",
        "npm ci (solo dependencias de producción)",
        "pm2 restart syncpass-api",
        "Nginx hace de proxy inverso al puerto 3000",
        "y resuelve el certificado TLS.",
    ], titulo="Qu\u00e9 se corre al desplegar", color="verde")

    L.nota(760, 580, 340, [
        "Las credenciales de la base y el id del user",
        "pool van en variables de entorno de la",
        "instancia, nunca en el repositorio.",
        "La base solo acepta conexiones desde el",
        "security group de la EC2.",
    ], titulo="Lo que no viaja al repositorio", color="amarillo")

    L.leyenda(1120, 150, [("azul", "Cliente"), ("naranja", "Servicio de AWS"),
                          ("verde", "Servidor de la API"), ("morado", "Base de datos"),
                          ("amarillo", "Regla de seguridad")], titulo="Leyenda")
    return L, "dibujo-despliegue-ec2.svg", "Despliegue de la API de SyncPass en EC2"


# ----------------------------------------------------------------------
# 5. Base de datos local del dispositivo (CRUD sin conexi\u00f3n)
# ----------------------------------------------------------------------
def base_local():
    W, H = 1400, 760
    L = Lienzo(W, H)
    L.titulo_hoja(
        "SyncPass \u00b7 Base de datos local del dispositivo",
        "Room/SQLite en Android e IndexedDB en el navegador: donde vive el CRUD sin conexi\u00f3n",
        "Local")

    borrador = L.tabla_entidad(60, 110, 290, "borrador_encuesta", [
        ("PK", "id_local", "INTEGER"),
        ("", "id_evento", "INTEGER"),
        ("", "id_participante", "TEXT"),
        ("", "satisfaccion_estrellas", "INTEGER"),
        ("", "utilidad_taller", "TEXT"),
        ("", "comentario", "TEXT"),
        ("", "estado", "TEXT"),
        ("", "actualizado_en", "TEXT"),
        ("", "is_synced", "INTEGER"),
    ], color="azul", subtitulo="sobre esta tabla trabaja el CRUD local")

    marcacion = L.tabla_entidad(430, 110, 290, "marcacion_local", [
        ("PK", "id_local", "INTEGER"),
        ("", "id_evento", "INTEGER"),
        ("", "id_participante", "TEXT"),
        ("", "timestamp_marcacion", "TEXT"),
        ("", "latitud", "REAL"),
        ("", "longitud", "REAL"),
        ("", "is_synced", "INTEGER"),
    ], color="azul", subtitulo="lecturas de QR hechas sin se\u00f1al")

    cola = L.tabla_entidad(230, 400, 290, "cola_sync", [
        ("PK", "id_cola", "INTEGER"),
        ("", "tabla_origen", "TEXT"),
        ("", "id_local", "INTEGER"),
        ("", "operacion", "TEXT"),
        ("", "intentos", "INTEGER"),
        ("", "ultimo_error", "TEXT"),
    ], color="naranja", subtitulo="qu\u00e9 falta enviar y cu\u00e1ntos intentos lleva")

    bb = borrador[1] + borrador[3]
    mb = marcacion[1] + marcacion[3]
    L.relacion([(205, bb), (205, 366), (300, 366), (300, 400)], lado_uno="ini",
               etiqueta="encola", etiqueta_xy=(252, 360))
    L.relacion([(575, mb), (575, 366), (450, 366), (450, 400)], lado_uno="ini",
               etiqueta="encola", etiqueta_xy=(512, 360))

    L.caja(700, 404, 280, 74, ["API REST en EC2", "POST /api/sync/lote"], color="verde", rx=3,
           size=11, peso=700)
    L.conector([(520, 441), (700, 441)], color="#82b366",
               etiqueta="env\u00edo por lote al volver la se\u00f1al", size=10, etiqueta_xy=(610, 428))
    L.conector([(760, 478), (760, 600), (120, 600), (120, bb)], color="#82b366", dash="5 3",
               etiqueta="respuesta del servidor: is_synced = 1", size=10, etiqueta_xy=(430, 592))

    L.nota(1010, 110, 330, [
        "Crear, leer, editar y borrar un borrador",
        "ocurre solo aqu\u00ed: no hay red de por medio.",
        "El participante puede corregir su respuesta",
        "las veces que quiera antes del env\u00edo.",
    ], titulo="El CRUD local")

    L.nota(1010, 260, 330, [
        "Un borrador enviado conserva su id_local",
        "y recibe el id del servidor en la respuesta.",
        "Si el env\u00edo falla, la fila sigue en cola_sync",
        "con un intento m\u00e1s y el \u00faltimo error.",
    ], titulo="Qu\u00e9 pasa despu\u00e9s del env\u00edo", color="verde")

    L.nota(1010, 410, 330, [
        "borrador_encuesta \u2192 encuestas_satisfaccion",
        "marcacion_local \u2192 asistencia_marcaciones",
        "El id del dispositivo no viaja: el servidor",
        "crea el suyo y lo devuelve.",
    ], titulo="A qu\u00e9 tabla del servidor corresponde", color="gris")

    L.leyenda(60, 640, [("azul", "Tabla de datos del dispositivo"),
                        ("naranja", "Control de env\u00edo"), ("verde", "Servidor")],
              titulo="Leyenda")
    return L, "dibujo-base-local.svg", "Base de datos local del dispositivo"


def main():
    for constructor in (modelo_relacional, modelo_estrella, modelo_copo_nieve,
                        despliegue_ec2, base_local):
        L, nombre, titulo = constructor()
        L.guardar(os.path.join(SALIDA, nombre), titulo)
        print("  img/%s  (%d x %d)" % (nombre, L.w, L.h))


if __name__ == "__main__":
    main()
