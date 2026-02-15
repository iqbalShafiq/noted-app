package id.usecase.noted

import android.app.Application
import id.usecase.noted.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class NotedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@NotedApplication)
            modules(appModule)
        }
    }
}
