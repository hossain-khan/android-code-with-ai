package dev.hossain.codematex.domain.runner

/**
 * Fake implementation of [PlaygroundCodeRunner] for unit testing.
 */
class FakePlaygroundCodeRunner(
    var supportedLanguages: Set<String> = setOf("rust", "rs"),
    var resultToReturn: PlaygroundExecutionResult = PlaygroundExecutionResult.Success("Executed successfully"),
) : PlaygroundCodeRunner {
    var lastCode: String? = null
    var lastLanguage: String? = null

    override fun supports(language: String): Boolean = supportedLanguages.any { it.equals(language, ignoreCase = true) }

    override suspend fun runSnippet(
        code: String,
        language: String,
    ): PlaygroundExecutionResult {
        lastCode = code
        lastLanguage = language
        return resultToReturn
    }
}
