package qhaty.edittext

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class IndexData(
    var str: String, var title: String,
    @PrimaryKey(autoGenerate = true) var id: Int? = null
)

@Dao
interface IndexDataDao {
    @Insert
    fun insert(vararg indexData: IndexData)

    @Delete
    fun delete(vararg indexData: IndexData)

    @Update
    fun update(vararg indexData: IndexData)

    @Query("select * from indexdata order by id")
    fun getAllIndex(): LiveData<List<IndexData>>
}

@Database(entities = [IndexData::class], version = 1, exportSchema = false)
abstract class IndexDatabase : RoomDatabase() {
    abstract val indexDataDao: IndexDataDao
}

fun Context.getIndexDao(): IndexDataDao =
    Room.databaseBuilder(this, IndexDatabase::class.java, "index").allowMainThreadQueries()
        .build().indexDataDao