package cl.duoc.syncpass.ui.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cl.duoc.syncpass.data.*
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

@RunWith(RobolectricTestRunner::class)
class ApuntesViewModelTest {
    private lateinit var context: Context

    @Before fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass-apuntes-test.db")
        AuthSession.signedIn(fakeJwt("owner", listOf("participante")), null)
    }

    @After fun limpiar() {
        AuthSession.signOut()
        context.deleteDatabase("syncpass-apuntes-test.db")
        Dispatchers.resetMain()
    }

    @Test fun guardaEnRoomSinRedYRetiraSoloTrasConfirmacion() = runBlocking {
        var conectado = false
        val api = FakeSyncPassApi()
        val repository = SyncPassRepository(context, api, { conectado }, "syncpass-apuntes-test.db")
        val primeraVista = ApuntesViewModel(repository)
        primeraVista.editar("Revisar los materiales.")
        primeraVista.guardar(3)
        val local = withTimeout(5_000) { primeraVista.state.first { !it.ocupado && it.pendientes.isNotEmpty() } }
        val registro = local.pendientes.single()
        assertEquals("Revisar los materiales.", registro.comentario)
        assertTrue(repository.pending("otra-cuenta").isEmpty())
        assertTrue(api.syncCalls.isEmpty())

        val reabierta = ApuntesViewModel(repository)
        reabierta.cargar(3)
        withTimeout(5_000) { reabierta.state.first { !it.ocupado && it.pendientes.isNotEmpty() } }
        conectado = true
        api.syncResult = SyncResult(listOf(registro.id))
        api.apuntesResult = listOf(ApunteRemoto(registro.id, "Revisar los materiales."))
        reabierta.sincronizar(3)
        val remoto = withTimeout(5_000) { reabierta.state.first { !it.ocupado && it.publicados.isNotEmpty() } }
        assertTrue(remoto.pendientes.isEmpty())
        assertEquals(registro.id, remoto.publicados.single().id)
        assertEquals("apunte", api.syncCalls.single().registros.single().tipo)
    }
}
