package qhaty.edittext

import android.widget.EditText
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.properties.Delegates

class MainModel : ViewModel() {
    //        val filesDir = getExternalFilesDir("save")
//        if (!filesDir!!.exists()) filesDir.mkdirs()
//        val file = File(filesDir.absolutePath, "auto")
//        if (!file.exists()) file.createNewFile()

    var indexList = listOf<IndexData>()

    //        var currentIndex: IndexData? = null
    private val appContext = App.appContext
    val indexDao = appContext.getIndexDao()
    private var currentNBDao: NBTextDao by Delegates.observable(appContext.getStackDao("默认")) { _, _, new ->
        currentNBDao = new
    }
    val nbEdit = MutableLiveData<NBEdit>()
    fun getNBEdit(editText: EditText, lo: LifecycleOwner, callback: () -> Unit) {
        nbEdit.value = NBEdit(editText, currentNBDao, lo) {
            callback()
        }
    }

}