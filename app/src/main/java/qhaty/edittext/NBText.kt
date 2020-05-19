package qhaty.edittext

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * @param actionTarget 改变字符
 * @param start 光标位置
 * @param add true为增加文本 false为减少
 *
 */
@Entity
data class NBTextUndo(
    var actionTarget: String,
    var start: Int,
    var add: Boolean,
    var end: Int = start,
    var index: Int = 0,
    @PrimaryKey(autoGenerate = true) var id: Int? = null
) {
    fun toRedo(): NBTextRedo = NBTextRedo(actionTarget, start, add, end, index)
    fun setSelectCount(count: Int) {
        end += count
    }
}

/**
 * @param actionTarget 改变字符
 * @param start 光标位置
 * @param add true为增加文本 false为减少
 *
 */
@Entity
data class NBTextRedo(
    var actionTarget: String,
    var start: Int,
    var add: Boolean,
    var end: Int = start,
    var index: Int = 0,
    @PrimaryKey(autoGenerate = true) var id: Int? = null
) {
    fun toUndo(): NBTextUndo = NBTextUndo(actionTarget, start, add, end, index)
    fun setSelectCount(count: Int) {
        end += count
    }
}

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
    fun getAllUndo(): LiveData<List<NBTextUndo>>

    @Query("select * from nbtextredo order by id")
    fun getAllRedo(): LiveData<List<NBTextRedo>>

    @Query("delete from nbtextundo")
    fun delAllUndo()

    @Query("delete from nbtextredo")
    fun delAllRedo()
}

@Database(entities = [NBTextUndo::class, NBTextRedo::class], version = 1, exportSchema = false)
abstract class NBTextDatabase : RoomDatabase() {
    abstract val nbTextDao: NBTextDao
}

fun Context.getStackDao(dbName: String): NBTextDao =
    Room.databaseBuilder(this, NBTextDatabase::class.java, dbName).allowMainThreadQueries()
        .build().nbTextDao