// port-lint: source receiver.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Group
import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.procmacro2.TokenTree
import io.github.kotlinmania.syn.Block
import io.github.kotlinmania.syn.Expr
import io.github.kotlinmania.syn.FieldMutability
import io.github.kotlinmania.syn.FnArg
import io.github.kotlinmania.syn.Item
import io.github.kotlinmania.syn.Macro
import io.github.kotlinmania.syn.Pat
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.Signature
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.gen.VisitMut
import io.github.kotlinmania.syn.token.Mut

internal fun hasSelfInSig(sig: Signature): Boolean {
    val visitor = HasSelf(false)
    visitor.visitSignature(sig)
    return visitor.value
}

internal fun hasSelfInBlock(block: Block): Boolean {
    val visitor = HasSelf(false)
    visitor.visitBlock(block)
    return visitor.value
}

internal fun hasSelfInTokenStream(tokens: TokenStream): Boolean =
    tokens.clone().any { tt ->
        when (tt) {
            is TokenTree.Ident -> tt.value.toString() == "Self"
            is TokenTree.Group -> hasSelfInTokenStream(tt.value.stream())
            else -> false
        }
    }

internal fun mutPat(pat: Pat): Mut? {
    val visitor = HasMutPat(null)
    visitor.visitPat(pat)
    return visitor.value
}

internal fun containsFn(tokens: TokenStream): Boolean =
    tokens.clone().any { tt ->
        when (tt) {
            is TokenTree.Ident -> tt.value.toString() == "fn"
            is TokenTree.Group -> containsFn(tt.value.stream())
            else -> false
        }
    }

internal class HasMutPat(
    var value: Mut?,
) : VisitMut() {
    override fun visitPatIdent(patIdent: Pat.Ident) {
        val mutability = patIdent.mutability
        if (mutability is FieldMutability.Mut) {
            value = Mut.from(mutability.token.span)
        } else {
            super.visitPatIdent(patIdent)
        }
    }
}

internal class HasSelf(
    var value: Boolean,
) : VisitMut() {
    override fun visitExprPath(exprPath: Expr.Path) {
        if (!exprPath.path.segments.isEmpty()) {
            value = value || exprPath.path.segments[0].ident.toString() == "Self"
        }
        super.visitExprPath(exprPath)
    }

    override fun visitTypePath(typePath: SynType.Path) {
        if (!typePath.path.segments.isEmpty()) {
            value = value || typePath.path.segments[0].ident.toString() == "Self"
        }
        super.visitTypePath(typePath)
    }

    override fun visitReceiver(receiver: FnArg.Receiver) {
        value = true
        super.visitReceiver(receiver)
    }

    override fun visitItem(i: Item) {
        // Do not recurse into nested items.
        i.hashCode()
    }

    override fun visitMacro(mac: Macro) {
        if (!containsFn(mac.tokens.clone())) {
            value = value || hasSelfInTokenStream(mac.tokens.clone())
        }
    }
}

internal object ReplaceSelf : VisitMut() {
    fun visitTokenStream(tokens: TokenStream): Pair<TokenStream, Boolean> {
        val out = mutableListOf<TokenTree>()
        var modified = false

        fun visitTokenStreamImpl(stream: TokenStream) {
            for (tt in stream) {
                when (tt) {
                    is TokenTree.Ident -> {
                        val (rewritten, changed) = prependUnderscoreToSelf(tt.value)
                        modified = modified || changed
                        out.add(TokenTree.Ident(rewritten))
                    }
                    is TokenTree.Group -> {
                        val (content, changed) = visitTokenStream(tt.value.stream())
                        modified = modified || changed
                        val newGroup = Group(tt.value.delimiter(), content)
                        newGroup.setSpan(tt.value.span())
                        out.add(TokenTree.Group(newGroup))
                    }
                    else -> {
                        out.add(tt)
                    }
                }
            }
        }

        visitTokenStreamImpl(tokens.clone())
        return (if (modified) TokenStream.fromTokenTrees(out) else tokens) to modified
    }

    override fun visitPath(p: Path) {
        if (p.segments.size == 1) {
            // Replace self, but not self.function.
            val segment = p.segments[0]
            val (rewritten, changed) = prependUnderscoreToSelf(segment.ident)
            if (changed) {
                segment.ident = rewritten
            }
        }
        for ((_, arguments) in p.segments.toList()) {
            visitPathArguments(arguments)
        }
    }

    override fun visitItem(i: Item) {
        // Visit local macro definitions because locally defined macros can refer to self.
        // Visit select macros, which commonly appear syntactically like an item despite
        // expanding to an expression. Otherwise, do not recurse into nested items.
        if (i is Item.Macro) {
            val path = i.mac.path
            if (path.isIdent("macro_rules") || path.segments.last()?.ident?.toString() == "select") {
                visitMacro(i.mac)
            }
        }
    }

    override fun visitMacro(mac: Macro) {
        // If the macro input contains fn, then self is more likely to refer to something
        // other than the outer function's receiver argument.
        if (!containsFn(mac.tokens.clone())) {
            val (updated, modified) = visitTokenStream(mac.tokens.clone())
            if (modified) {
                mac.tokens = updated
            }
        }
    }
}

internal fun prependUnderscoreToSelf(ident: Ident): Pair<Ident, Boolean> {
    val modified = ident.toString() == "self"
    return if (modified) {
        Ident.new("__self", ident.span()) to true
    } else {
        ident to false
    }
}
