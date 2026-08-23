// port-lint: source lib.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.parse2

/**
 * Attribute macro to make async fn in traits work with dyn traits.
 *
 * Async fns get transformed into methods that return
 * `Pin<Box<dyn Future + Send + 'async_trait>>` and delegate to an async block.
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
