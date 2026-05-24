package dev.hossain.codematex.runtime

import android.content.Context
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface RuntimeGraph {
    @Provides
    @SingleIn(AppScope::class)
    fun provideLlmEngine(
        @ApplicationContext context: Context,
    ): LlmEngine = LlmEngineImpl(context)
}
