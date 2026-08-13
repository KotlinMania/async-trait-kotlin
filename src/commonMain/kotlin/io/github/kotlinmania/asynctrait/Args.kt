// port-lint: source args.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.syn.ParseStream
import io.github.kotlinmania.syn.QuestionParse
import io.github.kotlinmania.syn.QuestionPeek
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.customKeyword

/**
 * Parsed arguments to the `#[async_trait]` attribute macro.
 *
 * The macro accepts either no arguments (`#[async_trait]`) or a single
 * `?Send` argument (`#[async_trait(?Send)]`). When `?Send` is present,
 * the generated async trait methods are `!Send` — the returned futures
 * are allowed to be non-Send, meaning they can only be awaited in the
 * task that spawned them.
 *
 * Without `?Send` (the default), the generated futures are `Send` and
 * can be freely moved across thread boundaries.
 */
data class Args(
    val local: Boolean,
)

/**
 * Parses the attribute argument list, accepting either an empty input
 * or exactly `?Send`.
 */
fun parseArgs(input: ParseStream): SynResult<Args> {
    val result = tryParse(input)
    if (result is SynResult.Success<Args> && input.isEmpty()) {
        return result
    }
    return SynResult.failure(error())
}

private fun tryParse(input: ParseStream): SynResult<Args> {
    if (!input.peek(QuestionPeek)) {
        return SynResult.success(Args(local = false))
    }
    val questionResult = QuestionParse.parse(input)
    if (questionResult is SynResult.Failure) {
        return SynResult.success(Args(local = false))
    }
    val (_, sendParse) = customKeyword("Send")
    val sendResult = sendParse.parse(input)
    if (sendResult is SynResult.Failure) {
        return SynResult.success(Args(local = false))
    }
    return SynResult.success(Args(local = true))
}

private fun error(): SynError =
    SynError.new(
        Span.callSite(),
        "expected #[async_trait] or #[async_trait(?Send)]",
    )
