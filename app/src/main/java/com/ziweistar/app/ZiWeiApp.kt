package com.ziweistar.app

import android.app.Application
import android.content.Context
import com.ziweistar.app.data.DatabaseProvider
import com.ziweistar.app.data.ZiWeiRepository
import com.ziweistar.app.engine.LocalOracleLibrary

/**
 * 应用入口。
 * 注意：本应用为隐私敏感型，不接入任何统计 / 崩溃 SDK。
 */
class ZiWeiApp : Application() {

    lateinit var repository: ZiWeiRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        LocalOracleLibrary.load(this)
        repository = ZiWeiRepository(this, DatabaseProvider.get(this))
    }

    companion object {
        @Volatile
        private var instance: ZiWeiApp? = null

        fun of(context: Context): ZiWeiApp =
            context.applicationContext as ZiWeiApp

        fun repo(context: Context): ZiWeiRepository = of(context).repository
    }
}
