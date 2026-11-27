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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.util.UUID

@Entity(tableName = "talleres")
data class TallerDto(
    @PrimaryKey val id: Long, val title: String, val descripcion: String?, val area: String?,
    val unidadVecinal: String?, val comuna: String?, val direccion: String?,
    val fechaHora: String?, val docenteLider: String?, val cuposMaximos: Int,
    val latitud: Double? = null, val longitud: Double? = null, val radioMetros: Int? = null,
    val inscritos: Int = 0,
    /** CSV de roles Cognito que pueden unirse; null = abierto a todos (RF12). */
    val rolesPermitidos: String? = null,
    /** Unidad vecinal exigida para unirse; null = sin restriccion de zona (RF12). */
    val zonaRestringida: String? = null
)

@Entity(tableName = "pending")
data class PendingRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val owner: String, val tipo: String, val tallerId: Long,
    val participante: String? = null, val rating: Int? = null,
    val comentario: String? = null, val utilidad: String? = null,
    val latitud: Double? = null, val longitud: Double? = null, val precisionMetros: Float? = null,
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

/**
 * Asistencia tomada sin conexion. Se guarda tal cual en el telefono (con el GPS
 * del momento) y se reenvia a /ingreso cuando vuelve la senal; el servidor es
 * idempotente por taller+participante, asi que reintentar no duplica nada.
 */
@Entity(tableName = "ingresos")
data class PendingEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tallerId: Long, val modo: String, val dispositivoId: String,
    val nombre: String? = null,
    val latitud: Double? = null, val longitud: Double? = null, val precisionMetros: Float? = null,
    val timestamp: String = Instant.now().toString()
)

@Dao interface IngresoDao {
    @Query("SELECT * FROM ingresos ORDER BY timestamp")
    suspend fun all(): List<PendingEntry>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: PendingEntry)
    @Query("DELETE FROM ingresos WHERE id = :id")
    suspend fun delete(id: String)
    @Query("SELECT COUNT(*) FROM ingresos")
    suspend fun count(): Int
    @Query("SELECT COUNT(*) FROM ingresos WHERE tallerId = :tallerId")
    suspend fun countFor(tallerId: Long): Int
}

@Dao interface TallerDao {
    @Query("SELECT * FROM talleres ORDER BY fechaHora")
    suspend fun all(): List<TallerDto>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(rows: List<TallerDto>)
    @Query("DELETE FROM talleres")
    suspend fun clear()
}

@Database(entities = [PendingRecord::class, TallerDto::class, PendingEntry::class],
    version = 5, exportSchema = false)
abstract class SyncPassDb : RoomDatabase() {
    abstract fun pending(): PendingDao
    abstract fun talleres(): TallerDao
    abstract fun ingresos(): IngresoDao
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
private val entryQueueMigration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ingresos (
              id TEXT NOT NULL PRIMARY KEY,
              tallerId INTEGER NOT NULL,
              modo TEXT NOT NULL,
              dispositivoId TEXT NOT NULL,
              nombre TEXT,
              latitud REAL,
              longitud REAL,
              precisionMetros REAL,
              timestamp TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

private val dominioMigration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE talleres ADD COLUMN rolesPermitidos TEXT")
        db.execSQL("ALTER TABLE talleres ADD COLUMN zonaRestringida TEXT")
    }
}

private val geoMigration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE talleres ADD COLUMN latitud REAL")
        db.execSQL("ALTER TABLE talleres ADD COLUMN longitud REAL")
        db.execSQL("ALTER TABLE talleres ADD COLUMN radioMetros INTEGER")
        db.execSQL("ALTER TABLE pending ADD COLUMN latitud REAL")
        db.execSQL("ALTER TABLE pending ADD COLUMN longitud REAL")
        db.execSQL("ALTER TABLE pending ADD COLUMN precisionMetros REAL")
    }
}

internal fun abrirBaseLocal(context: Context, nombre: String): SyncPassDb =
    Room.databaseBuilder(context.applicationContext, SyncPassDb::class.java, nombre)
        .addMigrations(catalogMigration, geoMigration, entryQueueMigration, dominioMigration)
        .build()
