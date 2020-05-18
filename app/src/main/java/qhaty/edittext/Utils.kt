package qhaty.edittext

import android.util.Log

inline fun <reified T> T.logd(info: String) {
    Log.i(T::class.java.name, info)
}