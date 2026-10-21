package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.SynResult

/** Parsed or expanded tokens, or their diagnostic. */
class TokenStreamParseResult private constructor(
    val value: TokenStream?,
    val error: SynError?,
) {
    val isSuccess: Boolean get() = value != null
    val isFailure: Boolean get() = error != null

    fun getOrNull(): TokenStream? = value

    fun exceptionOrNull(): SynError? = error

    fun getOrThrow(): TokenStream =
        value ?: throw AsyncTraitParseException(checkNotNull(error))

    companion object {
        fun success(value: TokenStream): TokenStreamParseResult = TokenStreamParseResult(value, null)

        fun failure(error: SynError): TokenStreamParseResult = TokenStreamParseResult(null, error)
    }
}

internal fun SynResult<TokenStream>.toTokenStreamParseResult(): TokenStreamParseResult =
    when (this) {
        is SynResult.Success -> TokenStreamParseResult.success(value)
        is SynResult.Failure -> TokenStreamParseResult.failure(error)
    }
