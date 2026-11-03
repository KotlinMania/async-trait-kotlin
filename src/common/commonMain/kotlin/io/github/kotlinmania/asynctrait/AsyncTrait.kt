package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.TokenStreamParseResult
import io.github.kotlinmania.syn.parse2
import io.github.kotlinmania.syn.toTokenStreamParseResult

/**
 * Type erasure for async trait methods.
 *
 * This procedural attribute macro rewrites async methods in traits and trait
 * implementations so traits containing async methods work with dynamic trait
 * objects.
 *
 * # Example
 *
 * Placing `@asyncTrait` on top of traits and trait implementations that contain
 * async methods enables trait objects such as `List<Holder<Advertisement>>`.
 *
 * ```kotlin
 * import io.github.kotlinmania.asynctrait.asyncTrait
 *
 * @asyncTrait
 * interface Advertisement {
 *     public fun run()
 * }
 *
 * open class Modal : Advertisement {
 *     @asyncTrait
 *     public override fun run() {
 *         this.renderFullscreen()
 *         for (step in 0 until 4) {
 *             remindUserToJoinMailingList(step)
 *         }
 *         this.hideForNow()
 *     }
 * }
 *
 * open class AutoplayingVideo(public var mediaUrl: String) : Advertisement {
 *     @asyncTrait
 *     public override fun run() {
 *         val stream = connect(this.mediaUrl)
 *         stream.play()
 *         Modal().run()
 *     }
 * }
 * ```
 *
 * # Supported features
 *
 * - Receiver by value, by reference, by mutable reference, or no receiver;
 * - Any number of arguments, any return value;
 * - Generic type parameters and scope parameters;
 * - Associated types;
 * - Having async and synchronous functions in the same trait;
 * - Default implementations provided by the trait;
 * - Elided scopes.
 *
 * # Explanation
 *
 * Async methods are transformed into methods that return a pinned heap-allocated
 * `Future` and delegate execution to an async move block:
 *
 * ```kotlin
 * fun AutoplayingVideo.run(): Pin<Future<Unit>> {
 *     return Holder.pin(Unit)
 * }
 * ```
 *
 * # Non-threadsafe futures
 *
 * Not all async traits need futures with `Send` bounds. To avoid placing `Send`
 * and `Sync` bounds on async trait methods, invoke the attribute macro with
 * `?Send` on both the trait and the implementation blocks.
 *
 * # Elided scopes
 *
 * Async method syntax requires scopes outside `&` and `&mut` references to be
 * named or marked with the anonymous `_` placeholder:
 *
 * ```kotlin
 * @asyncTrait
 * interface Test {
 *     public fun test(elided: Elided) {}
 * }
 * ```
 *
 * @param args Macro attribute arguments.
 * @param input The trait definition or implementation item to transform.
 * @return The expanded token stream or diagnostic in a [TokenStreamParseResult].
 */
fun asyncTrait(args: TokenStream, input: TokenStream): TokenStreamParseResult {
    val parsedArgs = parseArgs(args.clone())
    val options = parsedArgs.value ?: return TokenStreamParseResult.failure(checkNotNull(parsedArgs.error))
    return parse2(Item::parse, input.clone())
        .map { item ->
            expand(item, options.local)
            quote("`#`item", "item" to item)
        }.toTokenStreamParseResult()
}

/**
 * Expands [input] using [args], throwing an [IllegalArgumentException] when parsing fails.
 */
fun asyncTraitOrThrow(args: TokenStream, input: TokenStream): TokenStream =
    asyncTrait(args, input).getOrThrow()
