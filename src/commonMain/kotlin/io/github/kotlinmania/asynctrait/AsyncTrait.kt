// port-lint: source lib.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.parse2

/**
 * Attribute macro to make async functions in traits work with dynamic dispatch.
 *
 * Async functions are transformed into methods returning boxed futures.
 */
public fun asyncTrait(args: TokenStream, input: TokenStream): SynResult<TokenStream> {
    val parsedArgs = parse2(::parseArgs, args).getOrElse { return SynResult.failure(it) }
    val item = parse2(Item::parse, input).getOrElse { return SynResult.failure(it) }
    expand(item, parsedArgs.local)
    val tokens = quote("#item", "item" to item)
    return SynResult.success(tokens)
}

public fun asyncTraitOrThrow(args: TokenStream, input: TokenStream): TokenStream =
    asyncTrait(args, input).fold(
        onSuccess = { it },
        onFailure = { throw it },
    )
