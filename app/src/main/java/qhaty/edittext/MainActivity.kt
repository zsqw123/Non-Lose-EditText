package qhaty.edittext

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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
        val daoTest = getDao("test")
        val nbEdit = NBEdit(main_tv, daoTest, this) {
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
            toast("复制成功")
        }
        bt_del.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("输入\"删除\"确认删除")
            val edit = EditText(this)
            dialog.setView(edit)
            dialog.setPositiveButton("删除") { _, _ ->
                if (edit.text.toString() != "删除" && edit.text.toString() != "shanchu") {
                    toast("请输入\"删除\"来确认删除")
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        toast("开始删除")
                        dialog.setCancelable(false)
                        withContext(Dispatchers.IO) {
                            daoTest.delAllRedo()
                            daoTest.delAllUndo()
                        }
                        toast("删除完毕")
                        dialog.setCancelable(true)
                    }
                }
            }
            dialog.show()
        }
    }
}
