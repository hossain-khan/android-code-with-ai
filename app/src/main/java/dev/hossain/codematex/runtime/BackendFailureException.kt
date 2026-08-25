package dev.hossain.codematex.runtime

/**
 * Exception indicating that inference failed on a specific hardware backend.
 *
 * The failure may be recoverable by falling back to a less capable backend
 * (e.g. GPU -> CPU). Non-backend errors such as programming mistakes or invalid
 * state should not be wrapped in this type.
 */
class BackendFailureException(
    val failedBackend: LlmEngine.Backend,
    cause: Throwable,
) : Exception("Inference failed on backend $failedBackend", cause)
