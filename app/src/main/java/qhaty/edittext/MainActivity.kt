package qhaty.edittext

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val filesDir = getExternalFilesDir("save")
//        if (!filesDir!!.exists()) filesDir.mkdirs()
//        val file = File(filesDir.absolutePath, "auto")
//        if (!file.exists()) file.createNewFile()

        var indexList = listOf<IndexData>()
//        var currentIndex: IndexData? = null
        val indexDao = getIndexDao()
        indexDao.getAllIndex().observe(this, Observer {
            indexList = it
        })
        var nbEdit: NBEdit? = null
        fun chageNBEdit(new: NBTextDao) {
            nbEdit = NBEdit(main_tv, new, this) {
                val title = edit_title.text.toString()
                val text = main_tv.text.toString()
                GlobalScope.launch(Dispatchers.IO) {
                    for (i in indexList) {
                        if (i.title == title) {
                            indexDao.update(IndexData(title, text))
                            return@launch
                        }
                    }
                    indexDao.insert(IndexData(title, text))
                }
            }
        }

        var currentNBDao: NBTextDao by Delegates.observable(getStackDao("默认")) { _, _, new ->
            chageNBEdit(new)
        }
        chageNBEdit(currentNBDao)
        for (i in indexList) {
            if (i.title == "默认") {
                nbEdit?.setDefaultText(i.str)
            }
        }
        var lockRedo = false
        var lockUndo = false
        bt_redo.setOnClickListener {
            if (lockRedo) return@setOnClickListener
            lockRedo = true
            GlobalScope.launch(Dispatchers.Main) {
                nbEdit?.redo()
                lockRedo = false
            }
        }
        bt_undo.setOnClickListener {
            if (lockUndo) return@setOnClickListener
            lockUndo = true
            GlobalScope.launch(Dispatchers.Main) {
                nbEdit?.undo()
                lockUndo = false
            }
        }
        bt_copy.setOnClickListener {
            toClipboard(main_tv.text.toString())
            toast("复制成功")
        }
        var saveFlag = false //是否已经一次点击
        bt_save.setOnClickListener {
            var title: String = edit_title.text.toString()
            if (!saveFlag) {
                saveFlag = true
                GlobalScope.launch {
                    delay(2000L)
                    saveFlag = false
                }
            }
            if (title.isBlank()) {
                if (saveFlag) {
                    title = "默认"
                    edit_title.setText("默认")
                } else {
                    toast("标题为空 使用默认标题请再次点击")
                    return@setOnClickListener
                }
            }
            for (i in indexList) {
                if (i.title == title) {
                    currentNBDao = getStackDao(title)
                    return@setOnClickListener
                }
            }
            indexDao.insert(IndexData(title, main_tv.text.toString()))
            currentNBDao = getStackDao(title)
        }
        bt_list.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("请选择归档")
            var arrayChoice = arrayOf<String>()
            var arrayBoolean = booleanArrayOf()
            for (i in indexList) arrayChoice += i.title;arrayBoolean += false
            if (arrayChoice.isNotEmpty()) {
                dialog.setMultiChoiceItems(arrayChoice, arrayBoolean) { _, which, ischecked ->
                    arrayBoolean[which] = ischecked
                }
                dialog.setPositiveButton("打开") { _, _ ->
                    for (i in arrayBoolean.indices) {
                        if (arrayBoolean[i]) {
                            currentNBDao = getStackDao(arrayChoice[i])
                            break
                        }
                    }
                }
                dialog.setNegativeButton("删除") { _, _ ->
                    val delDialog = AlertDialog.Builder(this)
                    delDialog.setTitle("输入\"删除\"确认删除")
                    val edit = EditText(this)
                    delDialog.setView(edit)
                    delDialog.setPositiveButton("删除") { _, _ ->
                        if (edit.text.toString() != "删除" && edit.text.toString() != "shanchu") {
                            toast("请输入\"删除\"来确认删除")
                        } else {
                            GlobalScope.launch(Dispatchers.Main) {
                                toast("开始删除")
                                delDialog.setCancelable(false)
                                withContext(Dispatchers.IO) {
                                    val listDao = arrayListOf<NBTextDao>()
                                    for (i in arrayBoolean.indices) {
                                        if (arrayBoolean[i]) {
                                            listDao[i].delAllRedo()
                                            listDao[i].delAllUndo()
                                        }
                                    }

                                }
                                toast("删除完毕")
                                delDialog.setCancelable(true)
                            }
                        }
                    }
                    delDialog.show()
                }
                dialog.show()
            } else {
                dialog.setMessage("无归档")
                dialog.show()
            }
        }
    }
}
