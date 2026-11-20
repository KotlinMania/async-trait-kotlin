package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.syn.SynError

/** Parsed macro arguments or the diagnostic that prevented parsing. */
class ArgsParseResult private constructor(
    val value: Args?,
    val error: SynError?,
) {
    val isSuccess: Boolean get() = value != null
    val isFailure: Boolean get() = error != null

    fun getOrNull(): Args? = value

    fun exceptionOrNull(): SynError? = error

    fun getOrThrow(): Args =
        value ?: throw AsyncTraitParseException(checkNotNull(error))

    companion object {
        fun success(value: Args): ArgsParseResult = ArgsParseResult(value, null)

        fun failure(error: SynError): ArgsParseResult = ArgsParseResult(null, error)
    }
}
