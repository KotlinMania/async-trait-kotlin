package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream

/** The expanded source, or the diagnostic that prevented expansion. */
class AsyncTraitExpansion internal constructor(
    val source: String?,
    val error: String?,
) {
    val isSuccess: Boolean get() = source != null
}

/** Expands an asynchronous trait or implementation from source text. */
fun expandAsyncTrait(arguments: String, source: String): AsyncTraitExpansion {
    val parsedArguments = TokenStream.fromString(arguments)
    val argumentTokens = parsedArguments.value ?: return AsyncTraitExpansion(null, parsedArguments.error)
    val parsedSource = TokenStream.fromString(source)
    val sourceTokens = parsedSource.value
    return if (sourceTokens == null) {
        AsyncTraitExpansion(null, parsedSource.error)
    } else {
        val result = asyncTrait(argumentTokens, sourceTokens)
        AsyncTraitExpansion(result.value?.toString(), result.error?.toString())
    }
}
