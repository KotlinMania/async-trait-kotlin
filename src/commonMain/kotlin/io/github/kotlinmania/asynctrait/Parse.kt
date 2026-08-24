// port-lint: source parse.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.ParseStream
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.Item as SynItem

internal sealed class Item : ToTokens {
    public data class Trait(
        public var item: SynItem.Trait,
    ) : Item() {
        override fun toTokens(tokens: TokenStream) {
            item.toTokens(tokens)
        }
    }

    public data class Impl(
        public var item: SynItem.Impl,
    ) : Item() {
        override fun toTokens(tokens: TokenStream) {
            item.toTokens(tokens)
        }
    }

    public companion object {
        public fun parse(input: ParseStream): SynResult<Item> {
            val item = SynItem.parse(input).getOrElse { return SynResult.failure(it) }
            return when (item) {
                is SynItem.Trait -> SynResult.success(Trait(item))
                is SynItem.Impl -> {
                    if (item.traitPath == null) {
                        SynResult.failure(SynError.new(Span.callSite(), "expected a trait impl"))
                    } else {
                        SynResult.success(Impl(item))
                    }
                }
                else -> SynResult.failure(input.error("expected a trait or trait impl"))
            }
        }
    }
}
