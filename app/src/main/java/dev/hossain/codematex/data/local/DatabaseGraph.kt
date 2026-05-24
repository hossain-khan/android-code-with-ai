package dev.hossain.codematex.data.local

import android.content.Context
import androidx.room.Room
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface DatabaseGraph {
    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SessionDatabase =
        Room
            .databaseBuilder(context, SessionDatabase::class.java, "sessions.db")
            .build()

    @Provides
    fun provideSessionDao(db: SessionDatabase): SessionDao = db.sessionDao()
}
