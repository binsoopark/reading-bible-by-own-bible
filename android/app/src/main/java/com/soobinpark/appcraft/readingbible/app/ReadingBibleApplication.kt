package com.soobinpark.appcraft.readingbible.app

import android.app.Application
import androidx.annotation.StringRes

class ReadingBibleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppText.initialize(this)
    }
}

object AppText {
    private lateinit var application: Application

    fun initialize(application: Application) {
        this.application = application
    }

    fun get(@StringRes resource: Int, vararg arguments: Any): String =
        application.getString(resource, *arguments)
}
