package cl.duoc.syncpass.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duoc.syncpass.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.time.Instant
import java.util.UUID

@Entity(tableName = "talleres")
data class TallerDto(
    @PrimaryKey val id: Long, val title: String, val descripcion: String?, val area: String?,
    val unidadVecinal: String?, val comuna: String?, val direccion: String?,
    val fechaHora: String?, val docenteLider: String?, val cuposMaximos: Int,
    val inscritos: Int = 0
)

@Entity(tableName = "pending")
data class PendingRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val owner: String, val tipo: String, val tallerId: Long,
    val participante: String? = null, val rating: Int? = null,
    val comentario: String? = null, val utilidad: String? = null,
    val timestamp: String = Instant.now().toString()
)

@Dao interface PendingDao {
    @Query("SELECT * FROM pending WHERE owner = :owner ORDER BY timestamp")
    suspend fun forOwner(owner: String): List<PendingRecord>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: PendingRecord)
    @Query("DELETE FROM pending WHERE id = :id AND owner = :owner")
    suspend fun delete(id: String, owner: String)
}

@Dao interface TallerDao {
    @Query("SELECT * FROM talleres ORDER BY fechaHora")
    suspend fun all(): List<TallerDto>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(rows: List<TallerDto>)
    @Query("DELETE FROM talleres")
    suspend fun clear()
}

@Database(entities = [PendingRecord::class, TallerDto::class], version = 2, exportSchema = false)
abstract class SyncPassDb : RoomDatabase() {
    abstract fun pending(): PendingDao
    abstract fun talleres(): TallerDao
}

private val catalogMigration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `talleres` (
            `id` INTEGER NOT NULL PRIMARY KEY, `title` TEXT NOT NULL,
            `descripcion` TEXT, `area` TEXT, `unidadVecinal` TEXT, `comuna` TEXT,
            `direccion` TEXT, `fechaHora` TEXT, `docenteLider` TEXT,
            `cuposMaximos` INTEGER NOT NULL, `inscritos` INTEGER NOT NULL)""")
    }
}

data class SyncBatch(val registros: List<PendingPayload>)
data class PendingPayload(val id: String, val tipo: String, val tallerId: Long,
    val participante: String?, val rating: Int?, val comentario: String?,
    val utilidad: String?, val timestamp: String)
data class SyncResult(val acceptedIds: List<String>)

interface SyncPassApi {
    @GET("api/eventos") suspend fun talleres(): List<TallerDto>
    @POST("api/sync/batch") suspend fun sync(
        @Header("Authorization") authorization: String, @Body batch: SyncBatch): SyncResult
}

class SyncPassRepository(context: Context) {
    private val syncMutex = Mutex()
    private val db = Room.databaseBuilder(context.applicationContext, SyncPassDb::class.java, "syncpass.db")
        .addMigrations(catalogMigration).build()
    private val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().build()).addConverterFactory(GsonConverterFactory.create()).build()
        .create(SyncPassApi::class.java)

    suspend fun cachedTalleres(): List<TallerDto> = db.talleres().all()

    suspend fun talleres(): List<TallerDto> {
        val rows = api.talleres()
        db.withTransaction {
            db.talleres().clear()
            db.talleres().save(rows)
        }
        return rows
    }

    suspend fun pending(owner: String) = db.pending().forOwner(owner)
    suspend fun queue(row: PendingRecord) = db.pending().save(row)
    suspend fun sync(owner: String, accessToken: String): Int = syncMutex.withLock {
        var total = 0
        for (batch in db.pending().forOwner(owner).chunked(100)) {
            val result = api.sync("Bearer $accessToken", SyncBatch(batch.map {
                PendingPayload(it.id, it.tipo, it.tallerId, it.participante, it.rating,
                    it.comentario, it.utilidad, it.timestamp)
            }))
            val accepted = result.acceptedIds.toSet()
            for (row in batch) if (row.id in accepted) {
                db.pending().delete(row.id, owner)
                total++
            }
        }
        total
    }
}
