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

public fun hasSelfInSig(sig: Signature): Boolean {
    val visitor = HasSelf(false)
    visitor.visitSignature(sig)
    return visitor.found
}

public fun hasSelfInBlock(block: Block): Boolean {
    val visitor = HasSelf(false)
    visitor.visitBlock(block)
    return visitor.found
}

public fun mutPat(pat: Pat): Mut? {
    val visitor = HasMutPat(null)
    visitor.visitPat(pat)
    return visitor.mutToken
}

private fun hasSelfInTokenStream(tokens: TokenStream): Boolean =
    tokens.any { tt ->
        when (tt) {
            is TokenTree.Ident -> tt.value.toString() == "Self"
            is TokenTree.Group -> hasSelfInTokenStream(tt.value.stream())
            else -> false
        }
    }

private fun containsFn(tokens: TokenStream): Boolean =
    tokens.any { tt ->
        when (tt) {
            is TokenTree.Ident -> tt.value.toString() == "fn"
            is TokenTree.Group -> containsFn(tt.value.stream())
            else -> false
        }
    }

private class HasMutPat(
    public var mutToken: Mut?,
) : VisitMut() {
    override fun visitPatIdent(patIdent: Pat.Ident) {
        val mutability = patIdent.mutability
        if (mutability is FieldMutability.Mut) {
            mutToken = mutability.token
        } else {
            super.visitPatIdent(patIdent)
        }
    }
}

private class HasSelf(
    public var found: Boolean,
) : VisitMut() {
    override fun visitExprPath(exprPath: Expr.Path) {
        if (!exprPath.path.segments.isEmpty() &&
            exprPath.path.segments[0]
                .ident
                .toString() == "Self"
        ) {
            found = true
        }
        super.visitExprPath(exprPath)
    }

    override fun visitTypePath(typePath: SynType.Path) {
        if (!typePath.path.segments.isEmpty() &&
            typePath.path.segments[0]
                .ident
                .toString() == "Self"
        ) {
            found = true
        }
        super.visitTypePath(typePath)
    }

    override fun visitReceiver(receiver: FnArg.Receiver) {
        found = true
        super.visitReceiver(receiver)
    }

    override fun visitItem(i: Item) {
        // Do not recurse into nested items.
        if (i is Item.Verbatim) {
            // no-op
        }
    }

    override fun visitMacro(mac: Macro) {
        if (!containsFn(mac.tokens)) {
            found = found || hasSelfInTokenStream(mac.tokens)
        }
    }
}

public object ReplaceSelf : VisitMut() {
    public fun visitTokenStream(tokens: TokenStream): Pair<TokenStream, Boolean> {
        val out = mutableListOf<TokenTree>()
        var modified = false
        for (tt in tokens) {
            when (tt) {
                is TokenTree.Ident -> {
                    var ident = tt.value
                    if (prependUnderscoreToSelf(ident)) {
                        ident = Ident.new("__self", ident.span())
                        modified = true
                    }
                    out.add(TokenTree.Ident(ident))
                }
                is TokenTree.Group -> {
                    val (innerStream, groupModified) = visitTokenStream(tt.value.stream())
                    if (groupModified) {
                        modified = true
                    }
                    val newGroup = Group(tt.value.delimiter(), innerStream)
                    newGroup.setSpan(tt.value.span())
                    out.add(TokenTree.Group(newGroup))
                }
                else -> out.add(tt)
            }
        }
        return (if (modified) TokenStream.fromTokenTrees(out) else tokens) to modified
    }

    private fun prependUnderscoreToSelf(ident: Ident): Boolean =
        ident.toString() == "self"

    override fun visitPath(p: Path) {
        if (p.segments.size == 1) {
            val segment = p.segments[0]
            if (prependUnderscoreToSelf(segment.ident)) {
                segment.ident = Ident.new("__self", segment.ident.span())
            }
        }
        for (segment in p.segments.toList()) {
            visitPathArguments(segment.arguments)
        }
    }

    override fun visitItem(i: Item) {
        if (i is Item.Macro) {
            val path = i.mac.path
            if (path.isIdent("macro_rules") ||
                (
                    path.segments
                        .last()
                        ?.ident
                        ?.toString() == "select"
                )
            ) {
                visitMacro(i.mac)
            }
        }
    }

    override fun visitMacro(mac: Macro) {
        if (!containsFn(mac.tokens)) {
            val (newTokens, modified) = visitTokenStream(mac.tokens)
            if (modified) {
                mac.tokens = newTokens
            }
        }
    }
}
