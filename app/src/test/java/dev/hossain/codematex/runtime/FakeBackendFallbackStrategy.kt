package dev.hossain.codematex.runtime

class FakeBackendFallbackStrategy : BackendFallbackStrategy {
    private val unsupportedBackends = mutableSetOf<LlmEngine.Backend>()
    var resolveStartBackendInvocations = mutableListOf<LlmEngine.Backend>()
        private set
    var nextBackendInvocations = mutableListOf<LlmEngine.Backend>()
        private set
    var markUnsupportedInvocations = mutableListOf<LlmEngine.Backend>()
        private set

    override fun resolveStartBackend(preferred: LlmEngine.Backend): LlmEngine.Backend {
        resolveStartBackendInvocations.add(preferred)
        return if (unsupportedBackends.contains(preferred)) {
            LlmEngine.Backend.CPU
        } else {
            preferred
        }
    }

    override fun nextBackend(current: LlmEngine.Backend): LlmEngine.Backend? {
        nextBackendInvocations.add(current)
        return when (current) {
            LlmEngine.Backend.NPU -> LlmEngine.Backend.GPU
            LlmEngine.Backend.GPU -> LlmEngine.Backend.CPU
            LlmEngine.Backend.CPU -> null
        }
    }

    override fun markUnsupported(backend: LlmEngine.Backend) {
        markUnsupportedInvocations.add(backend)
        unsupportedBackends.add(backend)
    }

    override fun isUnsupported(backend: LlmEngine.Backend): Boolean = unsupportedBackends.contains(backend)

    fun setUnsupported(vararg backends: LlmEngine.Backend) {
        unsupportedBackends.addAll(backends)
    }

    fun clear() {
        unsupportedBackends.clear()
        resolveStartBackendInvocations.clear()
        nextBackendInvocations.clear()
        markUnsupportedInvocations.clear()
    }
}
