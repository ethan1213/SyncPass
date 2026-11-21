package cl.duoc.syncpass.ui.screens

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassDb
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.TallerDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EntryViewModelTest {
    private lateinit var context: Context
    private lateinit var repository: SyncPassRepository
    private lateinit var viewModel: EntryViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        context.getSharedPreferences("syncpass_guest", Context.MODE_PRIVATE).edit().clear().commit()
        AuthSession.signOut()
        repository = SyncPassRepository(context)
        viewModel = EntryViewModel(repository)
    }

    @After fun cleanup() {
        context.deleteDatabase("syncpass.db")
        AuthSession.signOut()
        Dispatchers.resetMain()
    }

    private fun seedTaller(taller: TallerDto) = runBlocking {
        val seedDb = Room.databaseBuilder(context, SyncPassDb::class.java, "syncpass.db").build()
        seedDb.talleres().save(listOf(taller))
        seedDb.close()
    }

    @Test fun openGuestInvalidIdIsNoOp() {
        viewModel.openGuest(0, onFound = {}, onNotFound = {})
        assertEquals(false, viewModel.busy)
        assertNull(viewModel.selectedTaller)
    }

    @Test fun openGuestNotFoundOfflineReportsCacheMessage() = runBlocking {
        var notFound = false
        withTimeout(5_000) {
            viewModel.openGuest(123, onFound = {}, onNotFound = { notFound = true })
            while (!notFound && viewModel.error.isBlank()) kotlinx.coroutines.yield()
        }
        assertTrue(notFound)
        assertTrue(viewModel.error.contains("Sin conexión"))
    }

    @Test fun openGuestFoundOfflineSelectsTallerFromCache() = runBlocking {
        seedTaller(TallerDto(id = 7, title = "Compostaje", descripcion = null, area = null,
            unidadVecinal = null, comuna = null, direccion = null, fechaHora = null,
            docenteLider = null, cuposMaximos = 15))
        var found = false
        withTimeout(5_000) {
            viewModel.openGuest(7, onFound = { found = true }, onNotFound = {})
            while (!found) kotlinx.coroutines.yield()
        }
        assertTrue(found)
        assertEquals("Compostaje", viewModel.selectedTaller?.title)
        assertTrue(viewModel.offline)
    }

    @Test fun joinGuestWithoutSelectedTallerIsNoOp() {
        viewModel.joinGuest("Nombre")
        assertEquals("", viewModel.attendance)
    }

    @Test fun joinGuestRequiresNonBlankName() = runBlocking {
        seedTaller(TallerDto(id = 8, title = "Arte", descripcion = null, area = null,
            unidadVecinal = null, comuna = null, direccion = null, fechaHora = null,
            docenteLider = null, cuposMaximos = 15))
        withTimeout(5_000) {
            var found = false
            viewModel.openGuest(8, onFound = { found = true }, onNotFound = {})
            while (!found) kotlinx.coroutines.yield()
        }
        viewModel.joinGuest("   ")
        assertTrue(viewModel.error.contains("Escribe tu nombre"))
    }

    @Test fun joinGuestSucceedsOfflineAndQueuesAttendance() = runBlocking {
        seedTaller(TallerDto(id = 9, title = "Danza", descripcion = null, area = null,
            unidadVecinal = null, comuna = null, direccion = null, fechaHora = null,
            docenteLider = null, cuposMaximos = 15))
        withTimeout(5_000) {
            var found = false
            viewModel.openGuest(9, onFound = { found = true }, onNotFound = {})
            while (!found) kotlinx.coroutines.yield()
        }
        withTimeout(5_000) {
            viewModel.joinGuest("Invitada Uno")
            while (viewModel.attendance.isBlank()) kotlinx.coroutines.yield()
        }
        assertTrue(viewModel.attendance.contains("Sin conexión"))
        assertEquals(1, repository.pendingEntriesFor(9))
    }

    @Test fun joinAccountOfflineQueuesAttendance() = runBlocking {
        withTimeout(5_000) {
            viewModel.joinAccount(11, "token")
            while (viewModel.attendance.isBlank()) kotlinx.coroutines.yield()
        }
        assertTrue(viewModel.attendance.contains("Sin conexión"))
        assertEquals(1, repository.pendingEntriesFor(11))
    }

    @Test fun clearErrorResetsErrorField() = runBlocking {
        withTimeout(5_000) {
            var notFound = false
            viewModel.openGuest(999, onFound = {}, onNotFound = { notFound = true })
            while (!notFound) kotlinx.coroutines.yield()
        }
        assertTrue(viewModel.error.isNotBlank())
        viewModel.clearError()
        assertEquals("", viewModel.error)
    }

    @Test fun guestNameReflectsRepositoryPreference() = runBlocking {
        assertEquals("", viewModel.guestName)
        repository.joinGuest(1, "Ana")
        assertEquals("Ana", viewModel.guestName)
    }
}
