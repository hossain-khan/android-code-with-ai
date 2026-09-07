package dev.hossain.codematex.domain.runner

/**
 * Result of executing a code snippet through a playground runner.
 */
sealed interface PlaygroundExecutionResult {
    /**
     * Execution completed successfully.
     *
     * @property output The standard output produced by the program.
     */
    data class Success(
        val output: String,
    ) : PlaygroundExecutionResult

    /**
     * Compilation or syntax check failed.
     *
     * @property diagnostic The compiler error messages or diagnostics.
     */
    data class CompilationError(
        val diagnostic: String,
    ) : PlaygroundExecutionResult

    /**
     * Execution failed due to a network, timeout, or environment error.
     *
     * @property message A user-facing description of the error.
     */
    data class NetworkError(
        val message: String,
    ) : PlaygroundExecutionResult
}

/**
 * Pluggable code runner that executes code snippets via an online playground.
 */
interface PlaygroundCodeRunner {
    /**
     * Checks if this runner supports the given programming language identifier.
     */
    fun supports(language: String): Boolean

    /**
     * Executes the supplied code snippet and returns a [PlaygroundExecutionResult].
     */
    suspend fun runSnippet(
        code: String,
        language: String,
    ): PlaygroundExecutionResult
}
