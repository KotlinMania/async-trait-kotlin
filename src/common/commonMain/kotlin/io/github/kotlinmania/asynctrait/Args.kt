// port-lint: source args.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.syn.ParseStream
import io.github.kotlinmania.syn.QuestionParse
import io.github.kotlinmania.syn.QuestionPeek
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.customKeyword
import io.github.kotlinmania.syn.parse2

/**
 * Parsed arguments to the async trait macro.
 *
 * The macro accepts either no arguments or a single `?Send` modifier argument.
 * When [local] is `true`, the generated async trait methods are allowed to produce
 * futures without thread-transfer bounds.
 */
data class Args(
    var local: Boolean,
) {
    companion object {
        fun parse(input: ParseStream): ArgsParseResult = parseArgs(input)
    }
}

/**
 * Parses the attribute argument list, accepting either an empty input
 * or `?Send`.
 */
fun parseArgs(input: ParseStream): ArgsParseResult =
    parseArgsResult(input).fold(ArgsParseResult::success, ArgsParseResult::failure)

/** Parses a complete attribute argument token stream. */
fun parseArgs(tokens: TokenStream): ArgsParseResult =
    parse2(::parseArgsResult, tokens.clone()).fold(ArgsParseResult::success, ArgsParseResult::failure)

internal fun parseArgsResult(input: ParseStream): SynResult<Args> {
    val matchVal = tryParse(input)
    return if (matchVal is SynResult.Success<Args> && input.isEmpty()) {
        matchVal
    } else {
        SynResult.failure(error())
    }
}

internal fun tryParse(input: ParseStream): SynResult<Args> {
    if (!input.peek(QuestionPeek)) {
        return SynResult.success(Args(local = false))
    }
    val (_, sendParse) = customKeyword("Send")
    QuestionParse.parse(input).getOrElse { return SynResult.failure(it) }
    sendParse.parse(input).getOrElse { return SynResult.failure(it) }
    return SynResult.success(Args(local = true))
}

internal fun error(): SynError {
    val msg = "expected #[async_trait] or #[async_trait(?Send)]"
    return SynError.new(Span.callSite(), msg)
}
