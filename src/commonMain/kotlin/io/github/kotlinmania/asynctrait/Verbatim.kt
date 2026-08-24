// port-lint: source verbatim.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.Attribute
import io.github.kotlinmania.syn.DefaultParse
import io.github.kotlinmania.syn.DefaultPeek
import io.github.kotlinmania.syn.ParseStream
import io.github.kotlinmania.syn.SemiParse
import io.github.kotlinmania.syn.Signature
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.Visibility
import io.github.kotlinmania.syn.VisibilityParse
import io.github.kotlinmania.syn.token.Default
import io.github.kotlinmania.syn.token.Semi

internal data class VerbatimFn(
    public var attrs: MutableList<Attribute>,
    public var vis: Visibility,
    public var defaultness: Default?,
    public var sig: Signature,
    public var semiToken: Semi,
) : ToTokens {
    public companion object {
        public fun parse(input: ParseStream): SynResult<VerbatimFn> {
            val attrs = Attribute.parseOuter(input).getOrElse { return SynResult.failure(it) }
            val visResult = VisibilityParse.parse(input)
            val vis = if (visResult.isSuccess) visResult.getOrThrow() else Visibility.Inherited
            val defaultness =
                if (input.peek(DefaultPeek)) {
                    DefaultParse.parse(input).getOrElse { return SynResult.failure(it) }
                } else {
                    null
                }
            val sig = Signature.parse(input).getOrElse { return SynResult.failure(it) }
            val semiToken = SemiParse.parse(input).getOrElse { return SynResult.failure(it) }
            return SynResult.success(VerbatimFn(attrs.toMutableList(), vis, defaultness, sig, semiToken))
        }
    }

    override fun toTokens(tokens: TokenStream) {
        for (attr in attrs) attr.toTokens(tokens)
        vis.toTokens(tokens)
        defaultness?.toTokens(tokens)
        sig.toTokens(tokens)
        semiToken.toTokens(tokens)
    }
}
