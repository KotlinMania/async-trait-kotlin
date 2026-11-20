package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.SynResult

internal class AsyncTraitParseException(
    val diagnostic: SynError,
) : IllegalArgumentException(diagnostic.message)

internal fun <T> SynResult<T>.getOrThrowParseException(): T =
    fold(
        onSuccess = { it },
        onFailure = { throw AsyncTraitParseException(it) },
    )
