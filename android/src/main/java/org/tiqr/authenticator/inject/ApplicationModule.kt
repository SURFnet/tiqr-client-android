package org.tiqr.authenticator.inject

import android.content.Context

import dagger.Module
import dagger.Provides
import org.tiqr.authenticator.TiqrApplication

@Module(includes = [ServiceModule::class])
class ApplicationModule(private val application: TiqrApplication) {

    @Provides
    fun provideApplicationContext(): Context {
        return application.applicationContext
    }
}