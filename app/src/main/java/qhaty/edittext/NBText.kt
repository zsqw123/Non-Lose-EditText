package qhaty.edittext

import android.content.Context
import androidx.room.*

@Entity
data class NBTextUndo(
    var actionTarget: String, var startCursor: Int, var add: Boolean,
    @PrimaryKey(autoGenerate = true) var id: Int? = null
)

@Entity
data class NBTextRedo(
    var actionTarget: String, var startCursor: Int, var add: Boolean,
    @PrimaryKey(autoGenerate = true) var id: Int? = null
)

@Dao
interface NBTextDao {
    @Insert
    fun insert(vararg nbText: NBTextUndo)

    @Insert
    fun insert(vararg nbText: NBTextRedo)

    @Update
    fun update(vararg nbText: NBTextUndo)

    @Update
    fun update(vararg nbText: NBTextRedo)

    @Delete
    fun del(vararg nbText: NBTextUndo)

    @Delete
    fun del(vararg nbText: NBTextRedo)

    @Query("select * from nbtextundo order by id")
    fun getAllUndo(): List<NBTextUndo>

    @Query("select * from nbtextredo order by id")
    fun getAllRedo(): List<NBTextRedo>

    @Query("delete from nbtextundo")
    fun delAllUndo()

    @Query("delete from nbtextredo")
    fun delAllRedo()
}

@Database(entities = [NBTextUndo::class, NBTextRedo::class], version = 1, exportSchema = false)
abstract class NBTextDatabase : RoomDatabase() {
    abstract val nbTextDao: NBTextDao
}

fun Context.getDao(dbName: String): NBTextDao =
    Room.databaseBuilder(this, NBTextDatabase::class.java, dbName).allowMainThreadQueries()
        .build().nbTextDao