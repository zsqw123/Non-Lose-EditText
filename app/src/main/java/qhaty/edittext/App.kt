package qhaty.edittext

import android.app.Application
import android.content.Context

class App : Application() {
    companion object {
        lateinit var appContext: Context
    }

    init {
        appContext = this
    }
}