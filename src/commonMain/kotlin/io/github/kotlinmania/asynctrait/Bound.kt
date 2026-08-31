// port-lint: source bound.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.toTokens
import io.github.kotlinmania.syn.TypeParamBound
import io.github.kotlinmania.syn.TypeParamBoundList
import io.github.kotlinmania.syn.token.PathSep

typealias Supertraits = TypeParamBoundList

enum class InferredBound : ToTokens {
    Send,
    Sync,
    ;

    internal fun asStr(): String =
        when (this) {
            Send -> "Send"
            Sync -> "Sync"
        }

    override fun toTokens(tokens: TokenStream) {
        val span = Span.callSite()
        PathSep.from(span).toTokens(tokens)
        Ident.new("core", span).toTokens(tokens)
        PathSep.from(span).toTokens(tokens)
        Ident.new("marker", span).toTokens(tokens)
        PathSep.from(span).toTokens(tokens)
        Ident.new(asStr(), span).toTokens(tokens)
    }

    override fun toString(): String =
        asStr()
}

fun hasBound(supertraits: Supertraits, bound: InferredBound): Boolean {
    for (supertrait in supertraits) {
        if (supertrait is TypeParamBound.Trait) {
            val path = supertrait.path
            if (
                path.isIdent(bound.asStr()) ||
                path.segments.len() == 3 &&
                (path.segments[0].ident.toString() == "std" || path.segments[0].ident.toString() == "core") &&
                path.segments[1].ident.toString() == "marker" &&
                path.segments[2].ident.toString() == bound.asStr()
            ) {
                return true
            }
        }
    }
    return false
}

fun Ident.eq(bound: InferredBound): Boolean =
    this.toString() == bound.asStr()
