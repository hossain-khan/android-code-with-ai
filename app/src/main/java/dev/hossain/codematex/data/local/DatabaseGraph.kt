package dev.hossain.codematex.data.local

import android.content.Context
import androidx.room.Room
import dev.hossain.codematex.BuildConfig
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Build-type-specific policy for Room migrations. Release builds must never silently destroy user
 * data, while debug builds may opt into destructive fallback to speed up active schema iteration.
 */
interface DatabaseMigrationPolicy {
    val allowDestructiveMigration: Boolean
}

@ContributesTo(AppScope::class)
interface DatabaseGraph {
    @Provides
    @SingleIn(AppScope::class)
    fun provideMigrationPolicy(): DatabaseMigrationPolicy =
        object : DatabaseMigrationPolicy {
            override val allowDestructiveMigration: Boolean = BuildConfig.DEBUG
        }

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(
        @ApplicationContext context: Context,
        policy: DatabaseMigrationPolicy,
    ): SessionDatabase {
        val builder =
            Room
                .databaseBuilder(context, SessionDatabase::class.java, "sessions.db")
                .addMigrations(
                    SessionDatabase.MIGRATION_1_2,
                    SessionDatabase.MIGRATION_2_3,
                )
        if (policy.allowDestructiveMigration) {
            // Debug-only safety net: recreate the database when there is no matching migration.
            builder.fallbackToDestructiveMigration(true)
        }
        return builder.build()
    }

    @Provides
    fun provideSessionDao(db: SessionDatabase): SessionDao = db.sessionDao()
}
