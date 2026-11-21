package cl.duoc.syncpass.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: SyncPassRepository

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        context.getSharedPreferences("syncpass_guest", Context.MODE_PRIVATE).edit().clear().commit()
        repository = SyncPassRepository(context)
    }

    @After fun cleanup() { context.deleteDatabase("syncpass.db") }

    @Test fun guestAttendanceSurvivesOfflineAndIsNotSentWithoutNetwork() = runBlocking {
        assertFalse(repository.isOnline())
        val result = repository.joinGuest(7, "Invitado Demo")
        assertEquals(ESTADO_PENDIENTE, result.estado)
        assertTrue(result.invitado)
        assertEquals("Invitado Demo", repository.guestName())
        assertEquals(1, repository.pendingEntries())
        assertEquals(1, repository.pendingEntriesFor(7))
        assertEquals(0, repository.flushEntries(null).enviados)
        assertEquals(1, repository.pendingEntries())
    }

    @Test fun pendingRecordsAreIsolatedByOwner() = runBlocking {
        repository.queue(PendingRecord(owner = "uno", tipo = "encuesta", tallerId = 3, rating = 5))
        repository.queue(PendingRecord(owner = "dos", tipo = "encuesta", tallerId = 3, rating = 2))
        assertEquals(1, repository.pending("uno").size)
        assertEquals(1, repository.pending("dos").size)
        assertNotEquals(repository.pending("uno").first().id, repository.pending("dos").first().id)
    }
}
