// port-lint: source parse.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Delimiter
import io.github.kotlinmania.procmacro2.Group
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.procmacro2.TokenTree
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.Attribute
import io.github.kotlinmania.syn.Block
import io.github.kotlinmania.syn.BracePeek
import io.github.kotlinmania.syn.DefaultParse
import io.github.kotlinmania.syn.DefaultPeek
import io.github.kotlinmania.syn.Expr
import io.github.kotlinmania.syn.ImplItem
import io.github.kotlinmania.syn.Macro
import io.github.kotlinmania.syn.NotPeek
import io.github.kotlinmania.syn.ParseStream
import io.github.kotlinmania.syn.SemiParse
import io.github.kotlinmania.syn.SemiPeek
import io.github.kotlinmania.syn.Signature
import io.github.kotlinmania.syn.Stmt
import io.github.kotlinmania.syn.StmtParse
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.TraitItem
import io.github.kotlinmania.syn.VisibilityParse
import io.github.kotlinmania.syn.braced
import io.github.kotlinmania.syn.parse2
import io.github.kotlinmania.syn.parseWithin
import io.github.kotlinmania.syn.Item as SynItem

internal sealed class Item : ToTokens {
    data class Trait(
        var item: SynItem.Trait,
    ) : Item() {
        override fun toTokens(tokens: TokenStream) {
            item.toTokens(tokens)
        }
    }

    data class Impl(
        var item: SynItem.Impl,
    ) : Item() {
        override fun toTokens(tokens: TokenStream) {
            item.toTokens(tokens)
        }
    }

    override fun toTokens(tokens: TokenStream) {
        when (this) {
            is Trait -> item.toTokens(tokens)
            is Impl -> item.toTokens(tokens)
        }
    }

    companion object {
        fun parse(input: ParseStream): SynResult<Item> {
            val trees = collectTokenTrees(input.fork())
            val lastTree = trees.lastOrNull()
            if (lastTree is TokenTree.Group && lastTree.value.delimiter() == Delimiter.Brace) {
                val emptyBraceGroup =
                    Group(Delimiter.Brace, TokenStream.new()).apply {
                        setSpan(lastTree.value.delimSpan().join())
                    }
                val headerStream = TokenStream.fromTokenTrees(trees.dropLast(1) + TokenTree.Group(emptyBraceGroup))
                val headerItem = parse2(SynItem::parse, headerStream)
                if (headerItem is SynResult.Success) {
                    val bodyStream = lastTree.value.stream()
                    when (val parsed = headerItem.value) {
                        is SynItem.Trait -> {
                            consumeAll(input)
                            val items = parse2(::parseTraitBody, bodyStream).getOrElse { return SynResult.failure(it) }
                            parsed.items = items
                            return SynResult.success(Trait(parsed))
                        }

                        is SynItem.Impl -> {
                            consumeAll(input)
                            if (parsed.traitPath == null) {
                                return SynResult.failure(SynError.new(Span.callSite(), "expected a trait impl"))
                            }
                            val items = parse2(::parseImplBody, bodyStream).getOrElse { return SynResult.failure(it) }
                            parsed.items = items
                            return SynResult.success(Impl(parsed))
                        }

                        else -> Unit
                    }
                }
            }

            val item = SynItem.parse(input).getOrElse { return SynResult.failure(it) }
            return when (item) {
                is SynItem.Trait -> {
                    SynResult.success(Trait(item))
                }

                is SynItem.Impl -> {
                    if (item.traitPath == null) {
                        SynResult.failure(SynError.new(Span.callSite(), "expected a trait impl"))
                    } else {
                        SynResult.success(Impl(item))
                    }
                }

                else -> {
                    SynResult.failure(input.error("expected a trait or trait impl"))
                }
            }
        }
    }
}

private fun collectTokenTrees(input: ParseStream): List<TokenTree> {
    val result = mutableListOf<TokenTree>()
    while (!input.isEmpty()) {
        val tree =
            input
                .step { cursor ->
                    val next = cursor.tokenTree() ?: return@step SynResult.failure(cursor.error("unexpected end of input"))
                    SynResult.success(next)
                }.getOrNull() ?: break
        result.add(tree)
    }
    return result
}

private fun consumeAll(input: ParseStream) {
    while (!input.isEmpty()) {
        input.step { cursor ->
            val next = cursor.tokenTree() ?: return@step SynResult.failure(cursor.error("unexpected end of input"))
            SynResult.success(next)
        }
    }
}

private fun parseTraitBody(input: ParseStream): SynResult<List<TraitItem>> {
    val items = mutableListOf<TraitItem>()
    while (!input.isEmpty()) {
        if (input.peek(SemiPeek)) {
            SemiParse.parse(input).getOrElse { return SynResult.failure(it) }
            items.add(TraitItem.Verbatim(TokenStream.new()))
            continue
        }
        val attrs =
            Attribute
                .parseOuter(input)
                .getOrElse { return SynResult.failure(it) }
                .toMutableList()
        if (Signature.parse(input.fork()).isSuccess) {
            val sig = Signature.parse(input).getOrElse { return SynResult.failure(it) }
            if (input.peek(BracePeek)) {
                val braces = braced(input).getOrElse { return SynResult.failure(it) }
                val innerAttrs = Attribute.parseInner(braces.content).getOrElse { return SynResult.failure(it) }
                attrs.addAll(innerAttrs)
                val stmts = parseBlockStmts(braces.content).getOrElse { return SynResult.failure(it) }
                braces.content.finishChildBuffer()
                items.add(TraitItem.Fn(attrs, sig, Block(braces.token, stmts), null))
            } else {
                val semi = SemiParse.parse(input).getOrElse { return SynResult.failure(it) }
                items.add(TraitItem.Fn(attrs, sig, null, semi))
            }
        } else {
            val itemSlice = consumeUntilItemEnd(input).getOrElse { return SynResult.failure(it) }
            val wrapped = quote("trait __AsyncTraitContainer { `#`itemSlice }", "itemSlice" to itemSlice)
            val parsedTrait = parse2(SynItem::parse, wrapped).getOrElse { return SynResult.failure(it) }
            val single =
                (parsedTrait as? SynItem.Trait)?.items?.singleOrNull()
                    ?: return SynResult.failure(input.error("expected trait item"))
            when (single) {
                is TraitItem.Const -> single.attrs = attrs + single.attrs
                is TraitItem.AssocType -> single.attrs = attrs + single.attrs
                is TraitItem.Fn -> single.attrs = attrs + single.attrs
                is TraitItem.Macro -> single.attrs = attrs + single.attrs
                is TraitItem.Verbatim -> Unit
            }
            items.add(single)
        }
    }
    return SynResult.success(items)
}

private fun parseImplBody(input: ParseStream): SynResult<List<ImplItem>> {
    val items = mutableListOf<ImplItem>()
    while (!input.isEmpty()) {
        if (input.peek(SemiPeek)) {
            SemiParse.parse(input).getOrElse { return SynResult.failure(it) }
            items.add(ImplItem.Verbatim(TokenStream.new()))
            continue
        }
        val fnFork = input.fork()
        val attrsResult = Attribute.parseOuter(fnFork)
        val visResult = if (attrsResult.isSuccess) VisibilityParse.parse(fnFork) else null
        val defaultness =
            if (visResult != null && visResult.isSuccess && fnFork.peek(DefaultPeek) && !fnFork.peek2(NotPeek)) {
                DefaultParse.parse(fnFork).getOrNull()
            } else {
                null
            }
        if (visResult != null && visResult.isSuccess && Signature.parse(fnFork.fork()).isSuccess) {
            input.advanceTo(fnFork)
            val attrs = attrsResult.getOrThrow().toMutableList()
            val vis = visResult.getOrThrow()
            val sig = Signature.parse(input).getOrElse { return SynResult.failure(it) }
            if (input.peek(BracePeek)) {
                val braces = braced(input).getOrElse { return SynResult.failure(it) }
                val innerAttrs = Attribute.parseInner(braces.content).getOrElse { return SynResult.failure(it) }
                attrs.addAll(innerAttrs)
                val stmts = parseBlockStmts(braces.content).getOrElse { return SynResult.failure(it) }
                braces.content.finishChildBuffer()
                items.add(ImplItem.Fn(attrs, vis, defaultness, sig, Block(braces.token, stmts)))
            } else {
                val semi = SemiParse.parse(input).getOrElse { return SynResult.failure(it) }
                val verbatimTokens = TokenStream.new()
                VerbatimFn(attrs, vis, defaultness, sig, semi).toTokens(verbatimTokens)
                items.add(ImplItem.Verbatim(verbatimTokens))
            }
        } else {
            val attrs =
                Attribute
                    .parseOuter(input)
                    .getOrElse { return SynResult.failure(it) }
                    .toMutableList()
            val itemSlice = consumeUntilItemEnd(input).getOrElse { return SynResult.failure(it) }
            val wrapped = quote("impl __AsyncTraitContainer for () { `#`itemSlice }", "itemSlice" to itemSlice)
            val parsedImpl = parse2(SynItem::parse, wrapped).getOrElse { return SynResult.failure(it) }
            val single =
                (parsedImpl as? SynItem.Impl)?.items?.singleOrNull()
                    ?: return SynResult.failure(input.error("expected impl item"))
            when (single) {
                is ImplItem.Const -> single.attrs = attrs + single.attrs
                is ImplItem.AssocType -> single.attrs = attrs + single.attrs
                is ImplItem.Fn -> single.attrs = attrs + single.attrs
                is ImplItem.Macro -> single.attrs = attrs + single.attrs
                is ImplItem.Verbatim -> Unit
            }
            items.add(single)
        }
    }
    return SynResult.success(items)
}

private fun consumeUntilItemEnd(input: ParseStream): SynResult<TokenStream> {
    val trees = mutableListOf<TokenTree>()
    while (!input.isEmpty()) {
        val tree =
            input
                .step { cursor ->
                    val next = cursor.tokenTree() ?: return@step SynResult.failure(cursor.error("unexpected end of input"))
                    SynResult.success(next)
                }.getOrElse { return SynResult.failure(it) }
        trees.add(tree)
        if (tree is TokenTree.Punct && tree.value.asChar() == ';') {
            return SynResult.success(TokenStream.fromTokenTrees(trees))
        }
        if (tree is TokenTree.Group && tree.value.delimiter() == Delimiter.Brace) {
            return SynResult.success(TokenStream.fromTokenTrees(trees))
        }
    }
    return SynResult.failure(input.error("unexpected end of input"))
}

private fun parseBlockStmts(input: ParseStream): SynResult<List<Stmt>> {
    val directFork = input.fork()
    val direct = parseWithin(directFork)
    if (direct.isSuccess && directFork.isEmpty()) {
        input.advanceTo(directFork)
        return direct
    }

    val stmts = mutableListOf<Stmt>()
    while (!input.isEmpty()) {
        while (input.peek(SemiPeek)) {
            stmts.add(Stmt.ExprStmt(Expr.Verbatim(TokenStream.new()), SemiParse.parse(input).getOrThrow()))
        }
        if (input.isEmpty()) break

        val macFork = input.fork()
        val macAttrs = Attribute.parseOuter(macFork)
        val macResult = if (macAttrs.isSuccess) Macro.parse(macFork) else null
        if (macResult != null && macResult.isSuccess) {
            val mac = macResult.getOrThrow()
            if (mac.path.segments.size > 1 && (mac.isBrace() || macFork.peek(SemiPeek) || macFork.isEmpty())) {
                input.advanceTo(macFork)
                val semi = if (input.peek(SemiPeek)) SemiParse.parse(input).getOrThrow() else null
                stmts.add(Stmt.MacroStmt(macAttrs.getOrThrow(), mac, semi))
                continue
            }
        }

        val restFork = input.fork()
        val rest = parseWithin(restFork)
        if (rest.isSuccess && restFork.isEmpty()) {
            input.advanceTo(restFork)
            stmts.addAll(rest.getOrThrow())
            break
        }

        val single = StmtParse.parse(input).getOrElse { return SynResult.failure(it) }
        stmts.add(single)
    }
    return SynResult.success(stmts)
}
