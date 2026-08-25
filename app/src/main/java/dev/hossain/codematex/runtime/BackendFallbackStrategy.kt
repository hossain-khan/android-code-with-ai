package dev.hossain.codematex.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import javax.inject.Inject

/**
 * Determines the order in which LiteRT-LM hardware backends should be attempted.
 *
 * The factory maintains a set of backends that have already failed on the current
 * device (e.g. missing OpenCL drivers for GPU). Callers iterate by requesting the
 * starting backend and then repeatedly asking for the next backend until
 * [nextBackend] returns `null`.
 */
interface BackendFallbackStrategy {
    /**
     * Returns the backend to actually start with, taking previously unsupported
     * backends into account. If [preferred] is unsupported, the strategy falls
     * back to CPU immediately.
     */
    fun resolveStartBackend(preferred: LlmEngine.Backend): LlmEngine.Backend

    /**
     * Returns the next backend to try after [current], or `null` if there is no
     * further fallback.
     */
    fun nextBackend(current: LlmEngine.Backend): LlmEngine.Backend?

    /**
     * Records [backend] as unsupported so that future calls avoid it.
     */
    fun markUnsupported(backend: LlmEngine.Backend)

    /**
     * Returns true if [backend] has been marked unsupported.
     */
    fun isUnsupported(backend: LlmEngine.Backend): Boolean
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultBackendFallbackStrategy
    @Inject
    constructor() : BackendFallbackStrategy {
        private val unsupportedBackends = mutableSetOf<LlmEngine.Backend>()

        override fun resolveStartBackend(preferred: LlmEngine.Backend): LlmEngine.Backend {
            var backend = preferred
            while (unsupportedBackends.contains(backend) && backend != LlmEngine.Backend.CPU) {
                backend = nextBackend(backend) ?: LlmEngine.Backend.CPU
            }
            return backend
        }

        override fun nextBackend(current: LlmEngine.Backend): LlmEngine.Backend? =
            when (current) {
                LlmEngine.Backend.NPU -> LlmEngine.Backend.GPU
                LlmEngine.Backend.GPU -> LlmEngine.Backend.CPU
                LlmEngine.Backend.CPU -> null
            }

        override fun markUnsupported(backend: LlmEngine.Backend) {
            unsupportedBackends.add(backend)
        }

        override fun isUnsupported(backend: LlmEngine.Backend): Boolean = unsupportedBackends.contains(backend)
    }
