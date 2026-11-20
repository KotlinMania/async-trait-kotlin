// port-lint: source parse.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.Attribute
import io.github.kotlinmania.syn.ImplPeek
import io.github.kotlinmania.syn.ItemParse
import io.github.kotlinmania.syn.ParseStream
import io.github.kotlinmania.syn.PubPeek
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.TraitPeek
import io.github.kotlinmania.syn.UnsafeParse
import io.github.kotlinmania.syn.UnsafePeek
import io.github.kotlinmania.syn.Item as SynItem

internal sealed class Item : ToTokens {
    data class Trait(
        var item: SynItem.Trait,
    ) : Item()

    data class Impl(
        var item: SynItem.Impl,
    ) : Item()

    override fun toTokens(tokens: TokenStream) {
        when (this) {
            is Trait -> item.toTokens(tokens)
            is Impl -> item.toTokens(tokens)
        }
    }

    companion object {
        fun parse(input: ParseStream): SynResult<Item> {
            val attrs = Attribute.parseOuter(input).getOrElse { return SynResult.failure(it) }
            var lookahead = input.lookahead1()
            if (lookahead.peek(UnsafePeek)) {
                val ahead = input.fork()
                UnsafeParse.parse(ahead).getOrElse { return SynResult.failure(it) }
                lookahead = ahead.lookahead1()
            }
            return if (lookahead.peek(PubPeek) || lookahead.peek(TraitPeek)) {
                val parsed = ItemParse.parse(input).getOrElse { return SynResult.failure(it) }
                if (parsed is SynItem.Trait) {
                    parsed.attrs = attrs
                    SynResult.success(Trait(parsed))
                } else {
                    SynResult.failure(SynError.new(Span.callSite(), "expected trait"))
                }
            } else if (lookahead.peek(ImplPeek)) {
                val parsed = ItemParse.parse(input).getOrElse { return SynResult.failure(it) }
                if (parsed is SynItem.Impl) {
                    if (parsed.traitPath == null) {
                        return SynResult.failure(SynError.new(Span.callSite(), "expected a trait impl"))
                    }
                    parsed.attrs = attrs
                    SynResult.success(Impl(parsed))
                } else {
                    SynResult.failure(SynError.new(Span.callSite(), "expected a trait impl"))
                }
            } else {
                SynResult.failure(lookahead.error())
            }
        }
    }
}
