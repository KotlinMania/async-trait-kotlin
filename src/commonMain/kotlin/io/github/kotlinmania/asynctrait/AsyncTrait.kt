// port-lint: source lib.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.parse2

/**
 * Type erasure macro for asynchronous trait methods.
 *
 * This procedural attribute macro transforms asynchronous functions in traits and trait
 * implementations into methods returning boxed, pinned futures to enable polymorphic dispatch.
 *
 * ### Motivation
 *
 * Using traits containing asynchronous functions with dynamic dispatch requires type
 * erasure because each asynchronous method has an anonymous future return type. This
 * attribute macro transforms asynchronous functions into methods returning boxed futures.
 *
 * ### Supported Features
 *
 * - Receiver by value, by reference, by mutable reference, or without a receiver parameter
 * - Arbitrary argument lists and return types
 * - Generic type parameters
 * - Associated types
 * - Traits containing a mix of asynchronous and synchronous functions
 * - Default implementations provided by the trait
 *
 * ### Transformation Details
 *
 * Asynchronous functions are transformed into methods returning pinned boxed futures
 * and delegate their execution to an asynchronous move block.
 *
 * ### Concurrency Bounds
 *
 * When concurrent thread execution bounds are not required, pass question mark Send in the
 * attribute arguments on both the trait and its implementations to omit thread safety constraints.
 *
 * @param args Macro attribute arguments.
 * @param input The trait definition or implementation item to transform.
 * @return The desugared token stream as a [SynResult].
 */
fun asyncTrait(args: TokenStream, input: TokenStream): SynResult<TokenStream> {
    val parsedArgs = parse2(::parseArgs, args).getOrElse { return SynResult.failure(it) }
    val item = parse2(Item::parse, input).getOrElse { return SynResult.failure(it) }
    expand(item, parsedArgs.local)
    val tokens = quote("#item", "item" to item)
    return SynResult.success(tokens)
}

fun asyncTraitOrThrow(args: TokenStream, input: TokenStream): TokenStream =
    asyncTrait(args, input).fold(
        onSuccess = { it },
        onFailure = { throw it },
    )
