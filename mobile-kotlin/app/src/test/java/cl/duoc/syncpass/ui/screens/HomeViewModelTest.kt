package cl.duoc.syncpass.ui.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cl.duoc.syncpass.data.SyncPassRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Sin red disponible (comportamiento por defecto de Robolectric), HomeViewModel
 * debe quedarse con el catalogo de Room y avisar que trabaja fuera de linea.
 *
 * El Main dispatcher real se reemplaza por uno de prueba para no depender del
 * Looper (pausado por defecto en Robolectric) al retomar una corrutina
 * despues de una consulta suspendida a Room.
 */
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    private lateinit var context: Context
    private lateinit var repository: SyncPassRepository
    private lateinit var viewModel: HomeViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        context.getSharedPreferences("syncpass_guest", Context.MODE_PRIVATE).edit().clear().commit()
        repository = SyncPassRepository(context)
        viewModel = HomeViewModel(repository)
    }

    @After fun cleanup() {
        context.deleteDatabase("syncpass.db")
        Dispatchers.resetMain()
    }

    @Test fun refreshCatalogFallsBackToCacheWithoutNetwork() = runBlocking {
        viewModel.refreshCatalog()
        val estado = withTimeout(5_000) {
            viewModel.state.first { it.offline }
        }
        assertTrue(estado.offline)
        assertTrue(estado.message.isNotBlank())
    }

    @Test fun refreshPendingReportsZeroWhenQueueIsEmpty() = runBlocking {
        viewModel.refreshPending(null)
        val estado = withTimeout(5_000) {
            viewModel.state.first { true }
        }
        assertEquals(0, estado.pending)
    }

    @Test fun syncWithoutNetworkKeepsWaitingMessage() = runBlocking {
        viewModel.sync()
        val estado = withTimeout(5_000) {
            viewModel.state.first { it.waiting }
        }
        assertTrue(estado.waiting)
        assertTrue(estado.message.contains("Sin conexi"))
    }

    @Test fun refreshPendingCountsQueuedRecordsForOwner() = runBlocking {
        repository.queue(cl.duoc.syncpass.data.PendingRecord(
            owner = "ana", tipo = "encuesta", tallerId = 1, rating = 5))
        viewModel.refreshPending("ana")
        val estado = withTimeout(5_000) { viewModel.state.first { it.pending == 1 } }
        assertEquals(1, estado.pending)
    }
}
