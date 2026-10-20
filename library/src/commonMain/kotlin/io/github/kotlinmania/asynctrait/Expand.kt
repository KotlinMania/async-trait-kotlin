// port-lint: source expand.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.Attribute
import io.github.kotlinmania.syn.Block
import io.github.kotlinmania.syn.FieldMutability
import io.github.kotlinmania.syn.FnArg
import io.github.kotlinmania.syn.GenericArgument
import io.github.kotlinmania.syn.GenericParam
import io.github.kotlinmania.syn.Generics
import io.github.kotlinmania.syn.ImplItem
import io.github.kotlinmania.syn.Lifetime
import io.github.kotlinmania.syn.LifetimeList
import io.github.kotlinmania.syn.Pat
import io.github.kotlinmania.syn.PathArguments
import io.github.kotlinmania.syn.ReturnType
import io.github.kotlinmania.syn.Signature
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.TraitItem
import io.github.kotlinmania.syn.TypeParamBound
import io.github.kotlinmania.syn.TypeParamBoundList
import io.github.kotlinmania.syn.WhereClause
import io.github.kotlinmania.syn.WherePredicate
import io.github.kotlinmania.syn.WherePredicateList
import io.github.kotlinmania.syn.gen.Fold
import io.github.kotlinmania.syn.gen.VisitMut
import io.github.kotlinmania.syn.parse2
import io.github.kotlinmania.syn.parseQuoteAttribute
import io.github.kotlinmania.syn.parseQuotePat
import io.github.kotlinmania.syn.parseQuoteStmtList
import io.github.kotlinmania.syn.span
import io.github.kotlinmania.syn.token.Colon
import io.github.kotlinmania.syn.token.Comma
import io.github.kotlinmania.syn.token.Gt
import io.github.kotlinmania.syn.token.Lt
import io.github.kotlinmania.syn.token.Plus
import io.github.kotlinmania.syn.token.Underscore
import io.github.kotlinmania.syn.token.Where

internal fun toTokens(item: Item, tokens: TokenStream) {
    when (item) {
        is Item.Trait -> item.item.toTokens(tokens)
        is Item.Impl -> item.item.toTokens(tokens)
    }
}

internal sealed class Context {
    data class Trait(
        val generics: Generics,
        val supertraits: Supertraits,
    ) : Context()

    data class Impl(
        val implGenerics: Generics,
        val associatedTypeImplTraits: Set<String>,
    ) : Context()

    fun lifetimes(used: List<Lifetime>): List<GenericParam.LifetimeParam> {
        val generics =
            when (this) {
                is Trait -> this.generics
                is Impl -> this.implGenerics
            }
        val result = mutableListOf<GenericParam.LifetimeParam>()
        for (param in generics.params) {
            if (param is GenericParam.LifetimeParam) {
                if (used.any { it.ident.toString() == param.lifetime.ident.toString() }) {
                    result.add(param)
                }
            }
        }
        return result
    }
}

internal fun expand(input: Item, isLocal: Boolean) {
    val expansion = Expansion(isLocal)
    when (input) {
        is Item.Trait -> expansion.trait(input)
        is Item.Impl -> expansion.implementation(input)
    }
}

private class Expansion(private val isLocal: Boolean) {
    fun trait(input: Item.Trait) {
        val traitItem = input.item
        val context = Context.Trait(traitItem.generics, traitItem.supertraits)
        for (inner in traitItem.items.filterIsInstance<TraitItem.Fn>()) {
            if (inner.sig.asyncness != null) {
                traitFunction(context, inner)
            }
        }
    }

    private fun traitFunction(context: Context.Trait, inner: TraitItem.Fn) {
        val sig = inner.sig
        val block = inner.default
        var hasSelf = hasSelfInSig(sig)
        inner.attrs = inner.attrs + parseQuoteAttribute(quote("#[must_use]"))
        if (block != null) {
            hasSelf = hasSelf || hasSelfInBlock(block)
            BlockTransformation.transform(context, sig, block)
            inner.attrs = inner.attrs + lintSuppressWithBody()
        } else {
            inner.attrs = inner.attrs + lintSuppressWithoutBody()
        }
        SignatureTransformation(context, sig, hasSelf, inner.default != null, isLocal).transform()
    }

    fun implementation(input: Item.Impl) {
        val implItem = input.item
        val associatedTypeImplTraits = implItem.items.filterIsInstance<ImplItem.AssocType>()
            .filter { it.ty is SynType.ImplTrait }
            .map { it.ident.toString() }
            .toSet()
        val context = Context.Impl(implItem.generics, associatedTypeImplTraits)
        for (inner in implItem.items) {
            when (inner) {
                is ImplItem.Fn -> implFunction(context, inner)
                is ImplItem.Verbatim -> verbatimFunction(context, inner)
                else -> Unit
            }
        }
    }

    private fun implFunction(context: Context.Impl, inner: ImplItem.Fn) {
        val sig = inner.sig
        if (sig.asyncness != null) {
            val hasSelf = hasSelfInSig(sig)
            BlockTransformation.transform(context, sig, inner.block)
            SignatureTransformation(context, sig, hasSelf, false, isLocal).transform()
            inner.attrs = inner.attrs + lintSuppressWithBody()
        }
    }

    private fun verbatimFunction(context: Context.Impl, inner: ImplItem.Verbatim) {
        val verbatim = parse2(VerbatimFn::parse, inner.tokens).getOrElse { return }
        val sig = verbatim.sig
        if (sig.asyncness != null) {
            SignatureTransformation(context, sig, hasSelfInSig(sig), false, isLocal).transform()
            verbatim.attrs.add(lintSuppressWithBody())
            inner.tokens = quote("`#`verbatim", "verbatim" to verbatim)
        }
    }
}

internal fun lintSuppressWithBody(): Attribute =
    parseQuoteAttribute(
        quote(
            "#[allow(elided_named_lifetimes, clippy::async_yields_async, clippy::diverging_sub_expression, " +
                "clippy::let_unit_value, clippy::needless_arbitrary_self_type, " +
                "clippy::no_effect_underscore_binding, clippy::shadow_same, clippy::type_complexity, " +
                "clippy::type_repetition_in_bounds, clippy::used_underscore_binding)]",
        ),
    )

internal fun lintSuppressWithoutBody(): Attribute =
    parseQuoteAttribute(
        quote(
            "#[allow(elided_named_lifetimes, clippy::type_complexity, clippy::type_repetition_in_bounds)]",
        ),
    )

private class SignatureTransformation(
    private val context: Context,
    private val sig: Signature,
    private val hasSelf: Boolean,
    private val hasDefault: Boolean,
    private val isLocal: Boolean,
) {
    fun transform() {
        sig.asyncness = null
        val (retArrow, ret) = when (val out = sig.output) {
            is ReturnType.Default -> quote("->") to quote("()")
            is ReturnType.TypeReturn -> quote("`#`arrow", "arrow" to out.arrowToken) to quote("`#`ty", "ty" to out.ty)
        }
        val lifetimes = collectLifetimes()
        for (param in sig.generics.params) {
            GenericBounds.transform(param, sig.generics)
        }
        addLifetimes(lifetimes)
        addSelfBounds()
        transformInputs()
        sig.output = boxedReturn(retArrow, ret)
    }

    private fun collectLifetimes(): CollectLifetimes {
        val lifetimes = CollectLifetimes()
        for (arg in sig.inputs) {
            when (arg) {
                is FnArg.Receiver -> lifetimes.visitReceiver(arg)
                is FnArg.Typed -> lifetimes.visitType(arg.patType.ty)
            }
        }
        return lifetimes
    }

    private fun addLifetimes(lifetimes: CollectLifetimes) {
        for ((_, paramLifetime) in context.lifetimes(lifetimes.explicit)) {
            val pred = parseQuoteWherePredicate(quote("`#`param: 'async_trait", "param" to paramLifetime))
            whereClauseOrDefault(sig.generics).predicates.push(pred) { Comma.default() }
        }
        if (sig.generics.ltToken == null) {
            sig.generics.ltToken = Lt.default()
        }
        if (sig.generics.gtToken == null) {
            sig.generics.gtToken = Gt.default()
        }
        for (elided in lifetimes.elided) {
            sig.generics.params.push(GenericParam.LifetimeParam.new(elided)) { Comma.default() }
            val bounds = LifetimeList()
            bounds.pushValue(Lifetime.new("'async_trait", Span.callSite()))
            val pred = WherePredicate.LifetimePredicate(elided, Colon.default(), bounds)
            whereClauseOrDefault(sig.generics).predicates.push(pred) { Comma.default() }
        }
        val lifetime = GenericParam.LifetimeParam.new(Lifetime.new("'async_trait", Span.callSite()))
        sig.generics.params.push(lifetime) { Comma.default() }
    }

    private fun addSelfBounds() {
        if (!hasSelf) return
        val filteredBounds = inferredBounds().filter { bound ->
            when (context) {
                is Context.Trait -> hasDefault && !hasBound(context.supertraits, bound)
                is Context.Impl -> false
            }
        }
        val selfPredTokens = if (filteredBounds.isEmpty()) {
            quote("Self: 'async_trait")
        } else {
            val boundTokens = TokenStream.new()
            for (bound in filteredBounds) {
                bound.toTokens(boundTokens)
                Plus.default().toTokens(boundTokens)
            }
            quote("Self: `#`bounds 'async_trait", "bounds" to boundTokens)
        }
        val predicate = parseQuoteWherePredicate(selfPredTokens)
        whereClauseOrDefault(sig.generics).predicates.push(predicate) { Comma.default() }
    }

    private fun inferredBounds(): List<InferredBound> {
        if (isLocal) return emptyList()
        val receiver = receiverType()
        return when {
            receiver is SynType.Reference && receiver.mutability == null -> listOf(InferredBound.Sync)
            receiver is SynType.Path && isArcSelf(receiver) -> listOf(InferredBound.Sync, InferredBound.Send)
            else -> listOf(InferredBound.Send)
        }
    }

    private fun receiverType(): SynType? = when (val first = sig.inputs.firstOrNull()) {
        is FnArg.Receiver -> first.type
        is FnArg.Typed -> {
            val pat = first.patType.pat
            if (pat is Pat.Ident && pat.ident.toString() == "self") first.patType.ty else null
        }
        else -> null
    }

    private fun transformInputs() {
        for ((index, arg) in sig.inputs.withIndex()) {
            when (arg) {
                is FnArg.Receiver -> if (arg.reference == null) {
                    arg.mutability = null
                }
                is FnArg.Typed -> transformTypedInput(index, arg)
            }
        }
    }

    private fun transformTypedInput(index: Int, arg: FnArg.Typed) {
        if (arg.patType.ty !is SynType.Reference) {
            val pat = arg.patType.pat
            if (pat is Pat.Ident) {
                pat.byRef = null
                pat.mutability = FieldMutability.None
            } else {
                val positional = positionalArg(index, pat)
                val mutability = mutPat(pat)
                val tokens = if (mutability != null) {
                    quote("`#`mutability `#`pos", "mutability" to mutability, "pos" to positional)
                } else {
                    quote("`#`pos", "pos" to positional)
                }
                arg.patType.pat = parseQuotePat(tokens)
            }
        }
        AddLifetimeToImplTrait.visitType(arg.patType.ty)
    }

    private fun boxedReturn(retArrow: TokenStream, ret: TokenStream): ReturnType {
        val bounds = if (isLocal) quote("'async_trait") else quote("::core::marker::Send + 'async_trait")
        return parseQuoteReturnType(
            quote(
                "`#`retArrow ::core::pin::Pin<Box<dyn ::core::future::Future<Output = `#`ret> + `#`bounds>>",
                "retArrow" to retArrow,
                "ret" to ret,
                "bounds" to bounds,
            ),
        )
    }
}

private object GenericBounds {
    fun transform(param: GenericParam, generics: Generics) {
        when (param) {
            is GenericParam.TypeParam -> typeParameter(param, generics)
            is GenericParam.LifetimeParam -> lifetimeParameter(param, generics)
            is GenericParam.ConstParam -> Unit
        }
    }

    private fun typeParameter(param: GenericParam.TypeParam, generics: Generics) {
        val name = param.ident
        val span = param.colonToken?.span ?: name.span()
        if (param.attrs.isEmpty()) {
            val bounds = param.bounds
            param.bounds = TypeParamBoundList()
            param.colonToken = null
            val tokens = if (bounds.isEmpty()) {
                quote("`#`name: 'async_trait", "name" to name)
            } else {
                quote("`#`name: 'async_trait + `#`bounds", "name" to name, "bounds" to bounds)
            }
            whereClauseOrDefault(generics).predicates.push(parseQuoteWherePredicate(tokens)) { Comma.default() }
        } else {
            val bound = TypeParamBound.LifetimeBound(Lifetime.new("'async_trait", span))
            param.bounds.push(bound) { Plus.default() }
        }
    }

    private fun lifetimeParameter(param: GenericParam.LifetimeParam, generics: Generics) {
        val name = param.lifetime
        val span = param.colonToken?.span ?: name.span()
        if (param.attrs.isEmpty()) {
            val bounds = param.bounds
            param.bounds = LifetimeList()
            param.colonToken = null
            val tokens = if (bounds.isEmpty()) {
                quote("`#`name: 'async_trait", "name" to name)
            } else {
                quote("`#`name: 'async_trait + `#`bounds", "name" to name, "bounds" to bounds)
            }
            whereClauseOrDefault(generics).predicates.push(parseQuoteWherePredicate(tokens)) { Comma.default() }
        } else {
            param.bounds.push(Lifetime.new("'async_trait", span)) { Plus.default() }
        }
    }
}

private object BlockTransformation {
    fun transform(context: Context, sig: Signature, block: Block) {
        val declarations = mutableListOf<TokenStream>()
        for ((index, arg) in sig.inputs.withIndex()) {
            when (arg) {
                is FnArg.Receiver -> declarations.add(captureReceiver(arg))
                is FnArg.Typed -> captureTyped(index, arg)?.let(declarations::add)
            }
        }
        if (sig.inputs.any { it is FnArg.Receiver }) {
            ReplaceSelf.visitBlock(block)
        }
        val returned = returnedBody(context, sig.output, block, declarations)
        block.stmts = parseQuoteStmtList(quote("Box::pin(async move { `#`returned })", "returned" to returned))
    }

    private fun captureReceiver(arg: FnArg.Receiver): TokenStream {
        val ident = Ident.new("__self", arg.selfToken.span)
        return if (arg.mutability != null) {
            quote("let mut `#`ident = `#`self;", "ident" to ident, "self" to arg.selfToken)
        } else {
            quote("let `#`ident = `#`self;", "ident" to ident, "self" to arg.selfToken)
        }
    }

    private fun captureTyped(index: Int, arg: FnArg.Typed): TokenStream? {
        if (arg.patType.ty is SynType.Reference) return null
        val attrs = arg.patType.attrs.filter { it.path().isIdent("cfg") }
        val pat = arg.patType.pat
        return when (pat) {
            is Pat.Ident -> quote(
                "`#`(`#`attrs)* let `#`mutability `#`ident = `#`ident;",
                "attrs" to attrs,
                "mutability" to pat.mutability,
                "ident" to pat.ident,
            )
            is Pat.Wild -> quote(
                "`#`(`#`attrs)* let `#`ident = `#`ident;",
                "attrs" to attrs,
                "ident" to positionalArg(index, pat),
            )
            else -> quote(
                "`#`(`#`attrs)* let `#`pat = { let `#`ident = `#`ident; `#`ident };",
                "attrs" to attrs,
                "pat" to pat,
                "ident" to positionalArg(index, pat),
            )
        }
    }

    private fun returnedBody(
        context: Context,
        output: ReturnType,
        block: Block,
        declarations: List<TokenStream>,
    ): TokenStream = when (output) {
        is ReturnType.Default -> quote(
            "`#`(`#`decls)* let _: () = `#`block;",
            "decls" to declarations,
            "block" to block,
        )
        is ReturnType.TypeReturn -> {
            if (containsAssociatedTypeImplTrait(context, output.ty)) {
                associatedReturn(block, declarations)
            } else {
                explicitReturn(output.ty, block, declarations)
            }
        }
    }

    private fun associatedReturn(block: Block, declarations: List<TokenStream>): TokenStream =
        if (declarations.isEmpty()) {
            quote("`#`(`#`stmts)*", "stmts" to block.stmts)
        } else {
            quote("`#`(`#`decls)* `#`block", "decls" to declarations, "block" to block)
        }

    private fun explicitReturn(ret: SynType, block: Block, declarations: List<TokenStream>): TokenStream =
        quote(
            "if let ::core::option::Option::Some(__ret) = ::core::option::Option::None::<`#`ret> { " +
                "#[allow(unreachable_code)] return __ret; } `#`(`#`decls)* " +
                "let __ret: `#`ret = `#`block; #[allow(unreachable_code)] __ret",
            "ret" to replaceImplTraitWithInfer(ret.deepCopy()),
            "decls" to declarations,
            "block" to block,
        )
}

private fun isArcSelf(ty: SynType.Path): Boolean {
    val segment = ty.path.segments.last() ?: return false
    val args = segment.arguments as? PathArguments.AngleBracketed
    val argument = if (args != null && args.args.size == 1) args.args[0] as? GenericArgument.TypeArg else null
    val inner = argument?.type as? SynType.Path
    return segment.ident.toString() == "Arc" && inner?.path?.isIdent("Self") == true
}

private fun positionalArg(i: Int, pat: Pat): Ident =
    Ident.new("__arg$i", pat.span().resolvedAt(Span.mixedSite()))

internal class AssociatedTypeImplTraits(
    val set: Set<String>,
    var contains: Boolean = false,
) : VisitMut() {
    override fun visitTypePath(typePath: SynType.Path) {
        val segments = typePath.path.segments
        val isSelfPath = segments.size == 2 && segments[0].ident.toString() == "Self"
        if (typePath.qself == null && isSelfPath && segments[1].ident.toString() in set) {
            contains = true
        }
        super.visitTypePath(typePath)
    }

    override fun visitTypePathMut(typePath: SynType.Path) {
        super.visitTypePathMut(typePath)
    }
}

private fun containsAssociatedTypeImplTrait(context: Context, ret: SynType): Boolean =
    when (context) {
        is Context.Trait -> {
            false
        }

        is Context.Impl -> {
            val visitor = AssociatedTypeImplTraits(context.associatedTypeImplTraits)
            visitor.visitType(ret)
            visitor.contains
        }
    }

private fun whereClauseOrDefault(generics: Generics): WhereClause {
    val existing = generics.whereClause
    if (existing != null) return existing
    val newClause = WhereClause(Where.default(), WherePredicateList())
    generics.whereClause = newClause
    return newClause
}

internal class ReplaceImplTraitWithInfer : VisitMut() {
    override fun visitTypeMut(t: SynType) {
        super.visitTypeMut(t)
    }
}

private object ReplaceImplTraitWithInferFold : Fold() {
    override fun foldType(t: SynType): SynType =
        if (t is SynType.ImplTrait) {
            SynType.Infer(Underscore.from(t.implToken.span))
        } else {
            super.foldType(t)
        }
}

private fun replaceImplTraitWithInfer(ty: SynType): SynType {
    ReplaceImplTraitWithInfer().visitTypeMut(ty)
    return ReplaceImplTraitWithInferFold.foldType(ty)
}

internal fun parseQuoteWherePredicate(tokenStream: TokenStream): WherePredicate {
    val result: SynResult<WherePredicate> = parse2(WherePredicate.Companion::parse, tokenStream)
    return result.getOrThrowParseException()
}

internal fun parseQuoteReturnType(tokenStream: TokenStream): ReturnType {
    val result: SynResult<ReturnType> = parse2(ReturnType.Companion::parse, tokenStream)
    return result.getOrThrowParseException()
}
