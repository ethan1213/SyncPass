package cl.duoc.syncpass.data.model

data class TallerComunitario(
    val id: Int,
    val titulo: String,
    val areaTematica: String,
    val unidadVecinal: String,
    val comuna: String,
    val fechaHora: String,
    val cuposMaximos: Int,
    val inscritos: Int,
    val isOfflineAvailable: Boolean = true
)

val talleresSyncPass = listOf(
    TallerComunitario(
        id = 1,
        titulo = "Alfabetización Digital Personas Mayores",
        areaTematica = "Tecnología",
        unidadVecinal = "UV N° 12 Los Castaños",
        comuna = "La Florida",
        fechaHora = "18 Oct · 10:00 hrs",
        cuposMaximos = 35,
        inscritos = 24
    ),
    TallerComunitario(
        id = 2,
        titulo = "Emprendimiento y Comercio Digital",
        areaTematica = "Emprendimiento",
        unidadVecinal = "Sede Villa Cordillera",
        comuna = "Puente Alto",
        fechaHora = "20 Oct · 15:30 hrs",
        cuposMaximos = 25,
        inscritos = 15
    ),
    TallerComunitario(
        id = 3,
        titulo = "Reparación Sustentable de Hardware",
        areaTematica = "Sustentabilidad",
        unidadVecinal = "Centro Cultural La Granja Sur",
        comuna = "La Granja",
        fechaHora = "24 Oct · 11:00 hrs",
        cuposMaximos = 20,
        inscritos = 18
    )
)
