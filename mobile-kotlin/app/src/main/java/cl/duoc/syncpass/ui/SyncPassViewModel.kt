package cl.duoc.syncpass.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.syncpass.data.SyncPassRepository

/**
 * Crea un ViewModel inyectandole el repositorio.
 *
 * Es la inyeccion de dependencias del proyecto, hecha a mano: no usamos Hilt,
 * pero ninguna pantalla construye su propio origen de datos. Cada Composable
 * pide su ViewModel con una linea y Compose se encarga de conservarlo entre
 * recomposiciones y giros de pantalla.
 */
@Composable
inline fun <reified VM : ViewModel> syncPassViewModel(
    repository: SyncPassRepository,
    crossinline builder: (SyncPassRepository) -> VM
): VM {
    val factory = remember(repository) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = builder(repository) as T
        }
    }
    return viewModel(factory = factory)
}
