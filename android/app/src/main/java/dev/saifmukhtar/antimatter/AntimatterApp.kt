package dev.saifmukhtar.antimatter

import android.app.Application
import dev.saifmukhtar.antimatter.core.ui.utils.LocalCrashHandler

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AntimatterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalCrashHandler.install(this)
    }
}
