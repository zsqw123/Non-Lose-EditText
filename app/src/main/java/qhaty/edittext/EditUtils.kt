package qhaty.edittext

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class NBEdit(
    private val editText: EditText,
    private val dao: NBTextDao,
    lifecycleOwner: LifecycleOwner,
    private val callOnTextChanged: () -> Unit
) {
    //操作序号(一次编辑可能对应多个操作，如替换文字，就是删除+插入)
    var index = 0
    private var editable: Editable

    //撤销栈
    private var undoList = arrayListOf<NBTextUndo>()

    //恢复栈
    private var redoList = arrayListOf<NBTextRedo>()

    init {
        dao.getAllRedo().observe(lifecycleOwner, androidx.lifecycle.Observer {
            redoList = it as ArrayList<NBTextRedo>
        })
        dao.getAllUndo().observe(lifecycleOwner, androidx.lifecycle.Observer {
            undoList = it as ArrayList<NBTextUndo>
        })
    }

    //自动操作标志，防止重复回调,导致无限撤销
    private var lockFlag = false
    protected fun onEditableChanged() {}
    protected fun onTextChanged() {}

    /**
     * 撤销
     */
    suspend fun undo(): Boolean {
        if (undoList.isEmpty()) return false
        lockFlag = true
        val action = undoList.pop()
        dao.del(action)
        dao.insert(action.toRedo())
        if (action.add) {
            //撤销添加
            editable.delete(action.start, action.start + action.actionTarget.length)
            editText.setSelection(action.start, action.start)
        } else {
            //撤销删除
            editable.insert(action.start, action.actionTarget)
            if (action.end == action.start) {
                editText.setSelection(action.start + action.actionTarget.length)
            } else {
                editText.setSelection(action.start, action.end)
            }
        }
        callOnTextChanged.invoke()
        //释放操作
        lockFlag = false
        //判断是否是下一个动作是否和本动作是同一个操作，直到不同为止
        if (undoList.isNotEmpty() && undoList.peek().index == action.index) undo()
        return true
    }

    /**
     * 恢复
     */
    suspend fun redo(): Boolean {
        if (redoList.isEmpty()) return false
        lockFlag = true
        val action = redoList.pop()
        dao.del(action)
        dao.insert(action.toUndo())
        if (action.add) {
            //恢复添加
            editable.insert(action.start, action.actionTarget)
            if (action.end == action.start) {
                editText.setSelection(action.start + action.actionTarget.length)
            } else {
                editText.setSelection(action.start, action.end)
            }
        } else {
            //恢复删除
            editable.delete(action.start, action.start + action.actionTarget.length)
            editText.setSelection(action.start, action.start)
        }
        callOnTextChanged.invoke()
        lockFlag = false
        //判断是否是下一个动作是否和本动作是同一个操作
        if (redoList.isNotEmpty() && redoList.peek().index == action.index) redo()
        return true
    }

    /**
     * 首次设置文本
     */
    fun setDefaultText(text: CharSequence) {
        lockFlag = true
        editable.replace(0, editable.length, text)
        lockFlag = false
    }

    private inner class Watcher : TextWatcher {
        /**
         * Before text changed.
         *
         * @param start 起始光标
         * @param count 选择数量
         * @param after 替换增加的文字数
         */
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (lockFlag) return
            val end = start + count
            if (end > start && end <= s.length) {
                val charSequence = s.subSequence(start, end)
                //删除了文字
                if (charSequence.isNotEmpty()) {
//                    val action = Action(charSequence, start, false)
                    val action = NBTextUndo(charSequence.toString(), start, false)
                    if (count > 1) {
                        //如果一次超过一个字符，说明用户选择了，然后替换或者删除操作
                        action.setSelectCount(count)
                    } else if (count == 1 && count == after) {
                        //一个字符替换
                        action.setSelectCount(count)
                    }
                    //还有一种情况:选择一个字符,然后删除(暂时没有考虑这种情况)
                    GlobalScope.launch(Dispatchers.IO) {
                        dao.insert(action)
//                        action.insertActionToDB(dao, true)
                        dao.delAllRedo()
                    }
//                    historyUndo.push(action)
//                    historyRedo.clear()
                    action.index = ++index
                }
            }
        }

        /**
         * On text changed.
         *
         * @param start  起始光标
         * @param before 选择数量
         * @param count  添加的数量
         */
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (lockFlag) return
            val end = start + count
            if (end > start) {
                val charSequence = s.subSequence(start, end)
                //添加文字
                if (charSequence.isNotEmpty()) {
                    val action = NBTextUndo(charSequence.toString(), start, true)
                    GlobalScope.launch(Dispatchers.IO) {
                        dao.insert(action)
                        dao.delAllRedo()
                    }
                    if (before > 0) {
                        //文字替换（先删除再增加），删除和增加是同一个操作，所以不需要增加序号
                        action.index = index
                    } else {
                        action.index = ++index
                    }
                }
            }
            callOnTextChanged.invoke()
        }

        override fun afterTextChanged(s: Editable) {
            if (lockFlag) return
            if (s !== editable) {
                editable = s
                onEditableChanged()
            }
            this@NBEdit.onTextChanged()
        }
    }

    init {
        editable = editText.text
        editText.addTextChangedListener(Watcher())
    }
}