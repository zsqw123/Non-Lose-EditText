package qhaty.edittext

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class IndexData(@PrimaryKey var title: String, var str: String)

@Dao
interface IndexDataDao {
    @Insert
    fun insert(vararg indexData: IndexData)

    @Delete
    fun delete(vararg indexData: IndexData)

    @Update
    fun update(vararg indexData: IndexData)

    @Query("select * from indexdata")
    fun getAllIndex(): LiveData<List<IndexData>>
}

@Database(entities = [IndexData::class], version = 1, exportSchema = false)
abstract class IndexDatabase : RoomDatabase() {
    abstract val indexDataDao: IndexDataDao
}

fun Context.getIndexDao(): IndexDataDao =
    Room.databaseBuilder(this, IndexDatabase::class.java, "index").allowMainThreadQueries()
        .build().indexDataDao