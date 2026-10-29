package cl.duoc.syncpass.ui.navigation

/**
 * Destinos de navegacion de SyncPass Movil.
 * Cada destino corresponde a una de las vistas documentadas en docs/MOCKUPS.md.
 */
sealed class Screen(val route: String, val title: String) {
    /** Panel de talleres en terreno del dia. */
    object Home : Screen("home", "Talleres en terreno")

    /** Acceso por roles con Cognito a traves de la API. */
    object Login : Screen("login", "Iniciar sesión")

    /** Inscripcion en terreno de un participante sin registro previo. */
    object Register : Screen("register", "Registro de participante")

    /** Escaner QR de acreditacion: objetivo menos de 5 segundos por persona. */
    object Scanner : Screen("scanner", "Acreditar por QR")

    /** Encuesta de satisfaccion en terreno (1 a 5 estrellas). */
    object Survey : Screen("survey", "Encuesta de satisfacción")

    /** Centro de sincronizacion de la cola local (is_synced = 0). */
    object Sync : Screen("sync", "Sincronización")

    /** Quorum en vivo del taller seleccionado. */
    object TallerDetail : Screen("taller/{tallerId}", "Detalle del taller") {
        fun createRoute(tallerId: Int) = "taller/$tallerId"
    }
}
