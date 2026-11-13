package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

// Registra que hubo una edición descargada con éxito, aunque no tenga charlas.
@Entity(tableName = "edicion")
data class Edicion(@PrimaryKey val id: Int, val descargada: Boolean)

@Dao
abstract class CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract fun observar(): Flow<List<Charla>>

    @Query("SELECT * FROM edicion WHERE id = 1")
    abstract fun observarEdicion(): Flow<Edicion?>

    @Query("SELECT id FROM charlas WHERE favorita = 1")
    abstract suspend fun marcadas(): List<String>

    @Query("DELETE FROM charlas")
    abstract suspend fun borrarTodas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardar(fichas: List<Charla>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun registrarEdicion(edicion: Edicion)

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)

    // Las marcas se leen dentro de la transacción: una marca hecha durante la
    // petición ya está confirmada y se conserva.
    @Transaction
    open suspend fun reemplazarPrograma(nuevas: List<Charla>) {
        val marcadas = marcadas().toSet()
        borrarTodas()
        guardar(nuevas.map { it.copy(favorita = it.id in marcadas) })
        registrarEdicion(Edicion(id = 1, descargada = true))
    }
}

@Database(entities = [Charla::class, Edicion::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}

// Conserva los datos de quienes instalaron la versión 1.
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `edicion` (`id` INTEGER NOT NULL, " +
            "`descargada` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("INSERT OR IGNORE INTO edicion (id, descargada) " +
            "SELECT 1, 1 WHERE EXISTS (SELECT 1 FROM charlas)")
    }
}
