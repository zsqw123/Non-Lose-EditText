package qhaty.edittext

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import kotlinx.coroutines.*
import java.lang.Exception
import java.util.*

open class NBEdit(
    private val editText: EditText,
    private val dao: NBTextDao,
    private val callOnTextChanged: () -> Unit
) {
    //操作序号(一次编辑可能对应多个操作，如替换文字，就是删除+插入)
    var index = 0
    private var editable: Editable

    //撤销栈
    var historyUndo = Stack<Action>()

    //恢复栈
    var historyRedo = Stack<Action>()

    //自动操作标志，防止重复回调,导致无限撤销
    private var flag = false
    protected fun onEditableChanged() {}
    protected fun onTextChanged() {}

    /**
     * 清理记录
     */
    private fun clearHistory() {
        historyUndo.clear()
        historyRedo.clear()
    }

    /**
     * 撤销
     */
    suspend fun undo() {
        if (historyUndo.empty()) return
        //锁定操作
        flag = true
        val action = (historyUndo.pop() ?: return).apply {
            delActionFromDB(dao, true)
            insertActionToDB(dao, false)
        }
        historyRedo.push(action)
        if (action.add) {
            //撤销添加
            editable.delete(action.startCursor, action.startCursor + action.actionTarget.length)
            editText.setSelection(action.startCursor, action.startCursor)
        } else {
            //撤销删除
            editable.insert(action.startCursor, action.actionTarget)
            if (action.endCursor == action.startCursor) {
                editText.setSelection(action.startCursor + action.actionTarget.length)
            } else {
                editText.setSelection(action.startCursor, action.endCursor)
            }
        }
        callOnTextChanged.invoke()
        //释放操作
        flag = false
        //判断是否是下一个动作是否和本动作是同一个操作，直到不同为止
        if (!historyUndo.empty() && historyUndo.peek().index == action.index) {
            undo()
        }
    }

    /**
     * 恢复
     */
    suspend fun redo() {
        if (historyRedo.empty()) return
        flag = true
        val action = (historyRedo.pop() ?: return).apply {
            delActionFromDB(dao, false)
            insertActionToDB(dao, true)
        }
        historyUndo.push(action)
        if (action.add) {
            //恢复添加
            editable.insert(action.startCursor, action.actionTarget)
            if (action.endCursor == action.startCursor) {
                editText.setSelection(action.startCursor + action.actionTarget.length)
            } else {
                editText.setSelection(action.startCursor, action.endCursor)
            }
        } else {
            //恢复删除
            editable.delete(action.startCursor, action.startCursor + action.actionTarget.length)
            editText.setSelection(action.startCursor, action.startCursor)
        }
        callOnTextChanged.invoke()
        flag = false
        //判断是否是下一个动作是否和本动作是同一个操作
        if (!historyRedo.empty() && historyRedo.peek().index == action.index) redo()
    }

    /**
     * 首次设置文本
     */
    suspend fun setDefaultText(text: CharSequence) {
        withContext(Dispatchers.IO) {
            clearHistory()
            flag = true
            try {
                val redoList = async(Dispatchers.IO) { dao.getAllRedo() }
                val undoList = async(Dispatchers.IO) { dao.getAllUndo() }
                for (i in redoList.await()) {
                    val action = Action(i.actionTarget, i.startCursor, i.add)
                    historyRedo.push(action)
                }
                for (i in undoList.await()) {
                    val action = Action(i.actionTarget, i.startCursor, i.add)
                    historyUndo.push(action)
                }
            } catch (e: Exception) { //catch到异常说明找不到历史 忽视就得了
            }
        }
        editable.replace(0, editable.length, text)
        flag = false
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
            if (flag) return
            val end = start + count
            if (end > start && end <= s.length) {
                val charSequence = s.subSequence(start, end)
                //删除了文字
                if (charSequence.isNotEmpty()) {
                    val action = Action(charSequence, start, false)
                    if (count > 1) {
                        //如果一次超过一个字符，说明用户选择了，然后替换或者删除操作
                        action.setSelectCount(count)
                    } else if (count == 1 && count == after) {
                        //一个字符替换
                        action.setSelectCount(count)
                    }
                    //还有一种情况:选择一个字符,然后删除(暂时没有考虑这种情况)
                    GlobalScope.launch(Dispatchers.IO) {
                        action.insertActionToDB(dao, true)
                        dao.delAllRedo()
                    }
                    historyUndo.push(action)
                    historyRedo.clear()
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
            if (flag) return
            val end = start + count
            if (end > start) {
                val charSequence = s.subSequence(start, end)
                //添加文字
                if (charSequence.isNotEmpty()) {
                    val action = Action(charSequence, start, true)
                    GlobalScope.launch(Dispatchers.IO) {
                        action.insertActionToDB(dao, true)
                        dao.delAllRedo()
                    }
                    historyUndo.push(action)
                    historyRedo.clear()
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
            if (flag) return
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

/**
 * @param actionTarget 改变字符
 * @param startCursor 光标位置
 * @param add 标志增
 *
 */
data class Action(var actionTarget: CharSequence, var startCursor: Int, var add: Boolean) {
    var index = 0 //操作序号
    var endCursor: Int = startCursor
    fun setSelectCount(count: Int) {
        endCursor += count
    }

    suspend fun insertActionToDB(dao: NBTextDao, undo: Boolean) {
        withContext(Dispatchers.IO) {
            if (undo) {
                val text = NBTextUndo(actionTarget.toString(), startCursor, add)
                dao.insert(text)
            } else {
                val text = NBTextRedo(actionTarget.toString(), startCursor, add)
                dao.insert(text)
            }
        }
    }

    suspend fun delActionFromDB(dao: NBTextDao, undo: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                if (undo) {
                    val all = dao.getAllUndo().reversed()
                    for (i in all) {
                        if (i.actionTarget == actionTarget && i.startCursor == startCursor && i.add == add) {
                            dao.del(i)
                            break
                        }
                    }
                } else {val all = dao.getAllRedo().reversed()
                    for (i in all) {
                        if (i.actionTarget == actionTarget && i.startCursor == startCursor && i.add == add) {
                            dao.del(i)
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                logd("删除失败")
            }
        }
    }
}
