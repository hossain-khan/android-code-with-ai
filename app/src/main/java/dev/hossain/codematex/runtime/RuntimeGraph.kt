package dev.hossain.codematex.runtime

import android.content.Context
import dev.hossain.codematex.ui.overlay.ModelConfigStore
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface RuntimeGraph {
    @Provides
    @SingleIn(AppScope::class)
    fun provideLlmEngine(llmEngineFactory: LlmEngineFactory): LlmEngine = LlmEngineImpl(llmEngineFactory)

    @Provides
    @SingleIn(AppScope::class)
    fun provideModelConfigStore(
        @ApplicationContext context: Context,
    ): ModelConfigStore = ModelConfigStore(context)
}
