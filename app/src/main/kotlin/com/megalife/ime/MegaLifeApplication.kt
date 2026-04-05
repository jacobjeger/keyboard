package com.megalife.ime

import android.app.Application
import com.megalife.ime.db.MegaLifeDatabase
import com.megalife.ime.language.LanguageRegistry

class MegaLifeApplication : Application() {

    lateinit var database: MegaLifeDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        _instance = this
        database = MegaLifeDatabase.getInstance(this)
        LanguageRegistry.initialize(this)
    }

    companion object {
        private var _instance: MegaLifeApplication? = null

        val instance: MegaLifeApplication
            get() = _instance
                ?: throw IllegalStateException("MegaLifeApplication not initialized")
    }
}
