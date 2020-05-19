package qhaty.edittext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import java.util.*


//inline fun <reified T> T.logd(info: String) {
//    Log.i(T::class.java.name, info)
//}

fun <T> ArrayList<T>.pop(): T {
    val last = peek()
    removeAt(size - 1)
    return last
}

fun <T> ArrayList<T>.peek(): T {
    if (size == 0) throw EmptyStackException()
    return this[size - 1]
}

fun Context.toClipboard(text: String, label: String = "label") {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun Context.toast(string: String) = Toast.makeText(this, string, Toast.LENGTH_SHORT).show()