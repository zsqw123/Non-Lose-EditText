package qhaty.edittext

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val filesDir = getExternalFilesDir("save")
        if (!filesDir!!.exists()) filesDir.mkdirs()
        val file = File(filesDir.absolutePath, "auto")
        if (!file.exists()) file.createNewFile()
        val nbEdit = NBEdit(main_tv, getDao("test"), this) {
            val text = main_tv.text.toString()
            GlobalScope.launch(Dispatchers.IO) { file.writeText(text) }
        }
        GlobalScope.launch(Dispatchers.IO) {
            val str = file.readText()
            withContext(Dispatchers.Main) { nbEdit.setDefaultText(str) }
        }
        var lockRedo = false
        var lockUndo = false
        bt_redo.setOnClickListener {
            if (lockRedo) return@setOnClickListener
            lockRedo = true
            GlobalScope.launch(Dispatchers.Main) {
                nbEdit.redo()
                lockRedo = false
            }
        }
        bt_undo.setOnClickListener {
            if (lockUndo) return@setOnClickListener
            lockUndo = true
            GlobalScope.launch(Dispatchers.Main) {
                nbEdit.undo()
                lockUndo = false
            }
        }
        bt_copy.setOnClickListener {
            toClipboard(main_tv.text.toString())
            Toast.makeText(this, "复制成功", Toast.LENGTH_SHORT).show()
        }
    }
}
