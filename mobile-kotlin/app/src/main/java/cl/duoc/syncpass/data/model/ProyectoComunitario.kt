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
