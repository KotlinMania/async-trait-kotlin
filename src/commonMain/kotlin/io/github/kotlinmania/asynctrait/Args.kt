// port-lint: source args.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.syn.Parse
import io.github.kotlinmania.syn.ParseStream
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.SynResult

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
internal object ArgsParse : Parse<Args> {
    override fun parse(input: ParseStream): SynResult<Args> {
        val result = tryParse(input)
        if (result is SynResult.Success<Args> && input.isEmpty()) {
            return result
        }
        return SynResult.failure(error())
    }
}

private fun tryParse(input: ParseStream): SynResult<Args> =
    input.call { stream ->
        val cursor = stream.cursor()
        val punctPair = cursor.punct()

        if (punctPair != null) {
            val (punct, afterQuestion) = punctPair
            if (punct.asChar() == '?') {
                val identPair = afterQuestion.ident()
                if (identPair != null) {
                    val (ident, _) = identPair
                    if (ident.toString() == "Send") {
                        return@call SynResult.success(Args(local = true))
                    }
                }
            }
        }

        SynResult.success(Args(local = false))
    }

private fun error(): SynError =
    SynError.new(
        Span.callSite(),
        "expected #[async_trait] or #[async_trait(?Send)]",
    )
