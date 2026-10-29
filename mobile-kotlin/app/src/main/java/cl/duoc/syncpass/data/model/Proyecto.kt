package cl.duoc.syncpass.data.model

data class Proyecto(
    val id: Int,
    val titulo: String,
    val categoria: String,
    val descripcionCorta: String,
    val descripcionLarga: String,
    val precioClp: Int,
    val precioFormateado: String,
    val tecnologias: List<String>,
    val calificacion: Double,
    val imagenRes: String
)

val proyectosEjemplo = listOf(
    Proyecto(
        id = 1,
        titulo = "Portafolio Personal",
        categoria = "Portafolios",
        descripcionCorta = "Página para mostrar información personal y trabajos realizados.",
        descripcionLarga = "Solución moderna y adaptable para profesionales que buscan proyectar su marca personal.",
        precioClp = 60000,
        precioFormateado = "$60.000 CLP",
        tecnologias = listOf("HTML5", "CSS3", "JavaScript", "Bulma"),
        calificacion = 4.9,
        imagenRes = "portafolio"
    ),
    Proyecto(
        id = 2,
        titulo = "Catálogo de Productos",
        categoria = "Comercio Electrónico",
        descripcionCorta = "Página para mostrar productos y precios con categorías.",
        descripcionLarga = "Plataforma digital para exhibir inventario de productos de forma visual y categorizada.",
        precioClp = 300000,
        precioFormateado = "$300.000 CLP",
        tecnologias = listOf("React", "Kotlin", "Vercel"),
        calificacion = 5.0,
        imagenRes = "catalogo"
    ),
    Proyecto(
        id = 3,
        titulo = "Página de Negocio",
        categoria = "Sitios Corporativos",
        descripcionCorta = "Página con los servicios y datos de contacto de un negocio.",
        descripcionLarga = "Diseñada para empresas de servicios, consultoras o pymes con video corporativo integrado.",
        precioClp = 250000,
        precioFormateado = "$250.000 CLP",
        tecnologias = listOf("HTML5", "CSS3", "JavaScript", "Bulma"),
        calificacion = 4.8,
        imagenRes = "negocio"
    )
)
