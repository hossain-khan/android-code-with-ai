package dev.hossain.codematex.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object RuntimeGraph {
    @Provides
    @SingleIn(AppScope::class)
    fun provideLlmEngine(llmEngineFactory: LlmEngineFactory): LlmEngine = LlmEngineImpl(llmEngineFactory)
}
