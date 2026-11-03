// port-lint: source expand.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.quote.quoteSpanned
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
import io.github.kotlinmania.syn.copy
import io.github.kotlinmania.syn.gen.Fold
import io.github.kotlinmania.syn.gen.VisitMut
import io.github.kotlinmania.syn.parse2
import io.github.kotlinmania.syn.parseQuoteAttribute
import io.github.kotlinmania.syn.parseQuotePat
import io.github.kotlinmania.syn.parseQuoteStmtList
import io.github.kotlinmania.syn.span
import io.github.kotlinmania.syn.token.Colon
import io.github.kotlinmania.syn.token.Comma
import io.github.kotlinmania.syn.token.Fn
import io.github.kotlinmania.syn.token.Gt
import io.github.kotlinmania.syn.token.Lt
import io.github.kotlinmania.syn.token.Plus
import io.github.kotlinmania.syn.token.Underscore
import io.github.kotlinmania.syn.token.Where

internal fun toTokens(item: Item, tokens: TokenStream) {
    item.toTokens(tokens)
}

internal sealed class Context {
    data class Trait(
        var generics: Generics,
        var supertraits: Supertraits,
    ) : Context()

    data class Impl(
        var implGenerics: Generics,
        var associatedTypeImplTraits: Set<Ident>,
    ) : Context()

    fun lifetimes(used: List<Lifetime>): List<GenericParam.LifetimeParam> {
        val targetGenerics =
            when (this) {
                is Trait -> generics
                is Impl -> implGenerics
            }
        val result = mutableListOf<GenericParam.LifetimeParam>()
        for (param in targetGenerics.params) {
            if (param is GenericParam.LifetimeParam && used.contains(param.lifetime)) {
                result.add(param)
            }
        }
        return result
    }
}

internal fun expand(input: Item, isLocal: Boolean) {
    when (input) {
        is Item.Trait -> {
            val traitItem = input.item
            val context = Context.Trait(generics = traitItem.generics, supertraits = traitItem.supertraits)
            for (inner in traitItem.items) {
                if (inner is TraitItem.Fn) {
                    val sig = inner.sig
                    val asyncToken = sig.asyncness
                    if (asyncToken != null) {
                        val block = inner.default
                        var hasSelf = hasSelfInSig(sig)
                        inner.attrs = inner.attrs + parseQuoteAttribute(quote("#[must_use]"))
                        if (block != null) {
                            hasSelf = hasSelf || hasSelfInBlock(block)
                            transformBlock(context, sig, block, asyncToken.span)
                            inner.attrs = inner.attrs + lintSuppressWithBody()
                        } else {
                            inner.attrs = inner.attrs + lintSuppressWithoutBody()
                        }
                        val hasDefault = inner.default != null
                        transformSig(context, sig, hasSelf, hasDefault, isLocal)
                    }
                }
            }
        }
        is Item.Impl -> {
            val implItem = input.item
            val associatedTypeImplTraits = linkedSetOf<Ident>()
            for (inner in implItem.items) {
                if (inner is ImplItem.AssocType && inner.ty is SynType.ImplTrait) {
                    associatedTypeImplTraits.add(inner.ident.copy())
                }
            }
            val context =
                Context.Impl(
                    implGenerics = implItem.generics,
                    associatedTypeImplTraits = associatedTypeImplTraits,
                )
            for (inner in implItem.items) {
                if (inner is ImplItem.Fn) {
                    val sig = inner.sig
                    val asyncToken = sig.asyncness
                    if (asyncToken != null) {
                        val hasSelf = hasSelfInSig(sig)
                        transformBlock(context, sig, inner.block, asyncToken.span)
                        transformSig(context, sig, hasSelf, false, isLocal)
                        inner.attrs = inner.attrs + lintSuppressWithBody()
                    }
                } else if (inner is ImplItem.Verbatim) {
                    val parsed = parse2(VerbatimFn::parse, inner.tokens.clone())
                    if (parsed is SynResult.Success) {
                        val method = parsed.value
                        val sig = method.sig
                        if (sig.asyncness != null) {
                            val hasSelf = hasSelfInSig(sig)
                            transformSig(context, sig, hasSelf, false, isLocal)
                            method.attrs.add(lintSuppressWithBody())
                            inner.tokens = quote("`#`method", "method" to method)
                        }
                    }
                }
            }
        }
    }
}

internal fun lintSuppressWithBody(): Attribute =
    parseQuoteAttribute(
        quote(
            "#[allow(elided_named_lifetimes, clippy::async_yields_async, clippy::diverging_sub_expression, clippy::let_unit_value, clippy::needless_arbitrary_self_type, clippy::no_effect_underscore_binding, clippy::shadow_same, clippy::type_complexity, clippy::type_repetition_in_bounds, clippy::used_underscore_binding)]",
        ),
    )

internal fun lintSuppressWithoutBody(): Attribute =
    parseQuoteAttribute(
        quote(
            "#[allow(elided_named_lifetimes, clippy::type_complexity, clippy::type_repetition_in_bounds)]",
        ),
    )

// Rewrites an async method signature to return a pinned boxed Future with inferred bounds.
internal fun transformSig(
    context: Context,
    sig: Signature,
    hasSelf: Boolean,
    hasDefault: Boolean,
    isLocal: Boolean,
) {
    val asyncToken = checkNotNull(sig.asyncness.also { sig.asyncness = null })
    sig.fnToken = Fn.from(asyncToken.span)

    val (retArrow, ret) =
        when (val out = sig.output) {
            is ReturnType.Default -> quote("->") to quote("()")
            is ReturnType.TypeReturn ->
                quote("`#`arrow", "arrow" to out.arrowToken) to quote("`#`ret", "ret" to out.ty)
        }

    val scopes = CollectLifetimes.new()
    for (arg in sig.inputs) {
        when (arg) {
            is FnArg.Receiver -> scopes.visitReceiver(arg)
            is FnArg.Typed -> scopes.visitType(arg.patType.ty)
        }
    }

    for (param in sig.generics.params) {
        when (param) {
            is GenericParam.TypeParam -> {
                val paramName = param.ident
                val span = param.colonToken.also { param.colonToken = null }?.span ?: paramName.span()
                if (param.attrs.isEmpty()) {
                    val bounds = param.bounds
                    param.bounds = TypeParamBoundList()
                    val predTokens =
                        if (bounds.isEmpty()) {
                            quoteSpanned(span, "`#`paramName: 'async_trait", "paramName" to paramName)
                        } else {
                            quoteSpanned(
                                span,
                                "`#`paramName: 'async_trait + `#`bounds",
                                "paramName" to paramName,
                                "bounds" to bounds,
                            )
                        }
                    whereClauseOrDefault(sig.generics).predicates.push(parseQuoteWherePredicate(predTokens)) {
                        Comma.default()
                    }
                } else {
                    param.bounds.push(TypeParamBound.LifetimeBound(Lifetime.new("'async_trait", span))) {
                        Plus.default()
                    }
                }
            }
            is GenericParam.LifetimeParam -> {
                val paramName = param.lifetime
                val span = param.colonToken.also { param.colonToken = null }?.span ?: paramName.span()
                if (param.attrs.isEmpty()) {
                    val bounds = param.bounds
                    param.bounds = LifetimeList()
                    val predTokens =
                        if (bounds.isEmpty()) {
                            quoteSpanned(span, "`#`paramName: 'async_trait", "paramName" to paramName)
                        } else {
                            quoteSpanned(
                                span,
                                "`#`paramName: 'async_trait + `#`bounds",
                                "paramName" to paramName,
                                "bounds" to bounds,
                            )
                        }
                    whereClauseOrDefault(sig.generics).predicates.push(parseQuoteWherePredicate(predTokens)) {
                        Comma.default()
                    }
                } else {
                    param.bounds.push(Lifetime.new("'async_trait", span)) { Plus.default() }
                }
            }
            is GenericParam.ConstParam -> Unit
        }
    }

    for (param in context.lifetimes(scopes.explicit)) {
        val paramScope = param.lifetime
        val span = paramScope.span()
        val pred = parseQuoteWherePredicate(quoteSpanned(span, "`#`param: 'async_trait", "param" to paramScope))
        whereClauseOrDefault(sig.generics).predicates.push(pred) { Comma.default() }
    }

    if (sig.generics.ltToken == null) {
        sig.generics.ltToken = Lt.from(sig.ident.span())
    }
    if (sig.generics.gtToken == null) {
        sig.generics.gtToken = Gt.from(sig.parenToken.span.join())
    }

    for (elided in scopes.elided) {
        sig.generics.params.push(GenericParam.LifetimeParam.new(elided)) { Comma.default() }
        val bounds = LifetimeList()
        bounds.pushValue(Lifetime.new("'async_trait", elided.span()))
        val pred = WherePredicate.LifetimePredicate(elided, Colon.from(elided.span()), bounds)
        whereClauseOrDefault(sig.generics).predicates.push(pred) { Comma.default() }
    }

    sig.generics.params.push(
        GenericParam.LifetimeParam.new(Lifetime.new("'async_trait", Span.callSite())),
    ) { Comma.default() }

    if (hasSelf) {
        val inferredBounds: List<InferredBound> =
            if (isLocal) {
                emptyList()
            } else {
                val receiverType = extractReceiverType(sig)
                when {
                    receiverType == null -> listOf(InferredBound.Send)
                    // Shared reference receiver: &Self
                    receiverType is SynType.Reference && receiverType.mutability == null ->
                        listOf(InferredBound.Sync)
                    // Reference-counted shared receiver: Arc<Self>
                    receiverType is SynType.Path && isArcSelf(receiverType) ->
                        listOf(InferredBound.Sync, InferredBound.Send)
                    else -> listOf(InferredBound.Send)
                }
            }

        val filteredBounds =
            inferredBounds.filter { bound ->
                when (context) {
                    is Context.Trait -> hasDefault && !hasBound(context.supertraits, bound)
                    is Context.Impl -> false
                }
            }

        val selfPredTokens =
            if (filteredBounds.isEmpty()) {
                quote("Self: 'async_trait")
            } else {
                val boundStream = TokenStream.new()
                for (b in filteredBounds) {
                    b.toTokens(boundStream)
                    Plus.default().toTokens(boundStream)
                }
                quote("Self: `#`bounds 'async_trait", "bounds" to boundStream)
            }
        whereClauseOrDefault(sig.generics).predicates.push(parseQuoteWherePredicate(selfPredTokens)) {
            Comma.default()
        }
    }

    for ((i, arg) in sig.inputs.withIndex()) {
        when (arg) {
            is FnArg.Receiver -> {
                if (arg.reference == null) {
                    arg.mutability = null
                }
            }
            is FnArg.Typed -> {
                if (arg.patType.ty !is SynType.Reference) {
                    val pat = arg.patType.pat
                    if (pat is Pat.Ident) {
                        pat.byRef = null
                        pat.mutability = FieldMutability.None
                    } else {
                        val positional = positionalArg(i, pat)
                        val m = mutPat(pat)
                        val newPatTokens =
                            if (m != null) {
                                quote("`#`m `#`positional", "m" to m, "positional" to positional)
                            } else {
                                quote("`#`positional", "positional" to positional)
                            }
                        arg.patType.pat = parseQuotePat(newPatTokens)
                    }
                }
                AddLifetimeToImplTrait.visitType(arg.patType.ty)
            }
        }
    }

    val returnBounds =
        if (isLocal) {
            quote("'async_trait")
        } else {
            quote("::core::marker::Send + 'async_trait")
        }
    sig.output =
        parseQuoteReturnType(
            quote(
                "`#`retArrow ::core::pin::Pin<Box<dyn ::core::future::Future<Output = `#`ret> + `#`bounds>>",
                "retArrow" to retArrow,
                "ret" to ret,
                "bounds" to returnBounds,
            ),
        )
}

// Wraps an async method body inside a pinned async move block while preserving parameter captures.
internal fun transformBlock(
    context: Context,
    sig: Signature,
    block: Block,
    asyncSpan: Span,
) {
    var replaceSelf = false
    val decls = mutableListOf<TokenStream>()
    for ((i, arg) in sig.inputs.withIndex()) {
        when (arg) {
            is FnArg.Receiver -> {
                replaceSelf = true
                val ident = Ident.new("__self", arg.selfToken.span)
                val mutability = arg.mutability
                if (mutability != null) {
                    decls.add(quote("let mut `#`ident = `#`selfToken;", "ident" to ident, "selfToken" to arg.selfToken))
                } else {
                    decls.add(quote("let `#`ident = `#`selfToken;", "ident" to ident, "selfToken" to arg.selfToken))
                }
            }
            is FnArg.Typed -> {
                // Forward conditional compilation attributes from the parameter to its local capture.
                val attrs = arg.patType.attrs.filter { it.path().isIdent("cfg") }
                if (arg.patType.ty !is SynType.Reference) {
                    val pat = arg.patType.pat
                    if (pat is Pat.Ident) {
                        val ident = pat.ident
                        val mutability = pat.mutability
                        decls.add(
                            quote(
                                "`#`(`#`attrs)* let `#`mutability `#`ident = `#`ident;",
                                "attrs" to attrs,
                                "mutability" to mutability,
                                "ident" to ident,
                            ),
                        )
                    } else {
                        val ident = positionalArg(i, pat)
                        if (pat is Pat.Wild) {
                            decls.add(
                                quote(
                                    "`#`(`#`attrs)* let `#`ident = `#`ident;",
                                    "attrs" to attrs,
                                    "ident" to ident,
                                ),
                            )
                        } else {
                            decls.add(
                                quote(
                                    "`#`(`#`attrs)* let `#`pat = { let `#`ident = `#`ident; `#`ident };",
                                    "attrs" to attrs,
                                    "pat" to pat,
                                    "ident" to ident,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    if (replaceSelf) {
        ReplaceSelf.visitBlock(block)
    }

    val letRet =
        when (val out = sig.output) {
            is ReturnType.Default ->
                quote("`#`(`#`decls)* let _: () = `#`block;", "decls" to decls, "block" to block)
            is ReturnType.TypeReturn -> {
                val ret = out.ty
                if (containsAssociatedTypeImplTrait(context, ret)) {
                    if (decls.isEmpty()) {
                        quote("`#`(`#`stmts)*", "stmts" to block.stmts)
                    } else {
                        quote("`#`(`#`decls)* `#`block", "decls" to decls, "block" to block)
                    }
                } else {
                    val retInfer = replaceImplTraitWithInfer(ret.deepCopy())
                    quote(
                        "if let ::core::option::Option::Some(__ret) = ::core::option::Option::None::<`#`ret> { #[allow(unreachable_code)] return __ret; } `#`(`#`decls)* let __ret: `#`ret = `#`block; #[allow(unreachable_code)] __ret",
                        "ret" to retInfer,
                        "decls" to decls,
                        "block" to block,
                    )
                }
            }
        }

    val boxPin = quoteSpanned(asyncSpan, "Box::pin(async move { `#`letRet })", "letRet" to letRet)
    block.stmts = parseQuoteStmtList(boxPin)
}

private fun extractReceiverType(sig: Signature): SynType? =
    when (val first = sig.inputs.firstOrNull()) {
        is FnArg.Receiver -> first.type
        is FnArg.Typed -> {
            val pat = first.patType.pat
            if (pat is Pat.Ident && pat.ident.toString() == "self") {
                first.patType.ty
            } else {
                null
            }
        }
        else -> null
    }

private fun isArcSelf(ty: SynType.Path): Boolean {
    val segment = ty.path.segments.last() ?: return false
    if (segment.ident.toString() != "Arc") return false
    val arguments = segment.arguments
    if (arguments is PathArguments.AngleBracketed && arguments.args.len() == 1) {
        val firstArg = arguments.args[0]
        if (firstArg is GenericArgument.TypeArg) {
            val innerTy = firstArg.type
            return innerTy is SynType.Path && innerTy.path.isIdent("Self")
        }
    }
    return false
}

internal fun positionalArg(i: Int, pat: Pat): Ident {
    val span = pat.span().resolvedAt(Span.mixedSite())
    return Ident.new("__arg$i", span)
}

internal class AssociatedTypeImplTraits(
    val set: Set<Ident>,
    var contains: Boolean = false,
) : VisitMut() {
    override fun visitTypePath(typePath: SynType.Path) {
        if (
            typePath.qself == null &&
            typePath.path.segments.len() == 2 &&
            typePath.path.segments[0].ident.toString() == "Self" &&
            set.contains(typePath.path.segments[1].ident)
        ) {
            contains = true
        }
        super.visitTypePath(typePath)
    }
}

internal fun containsAssociatedTypeImplTrait(context: Context, ret: SynType): Boolean =
    when (context) {
        is Context.Trait -> false
        is Context.Impl -> {
            val visit = AssociatedTypeImplTraits(set = context.associatedTypeImplTraits)
            visit.visitType(ret)
            visit.contains
        }
    }

internal fun whereClauseOrDefault(generics: Generics): WhereClause {
    val existing = generics.whereClause
    if (existing != null) return existing
    val created = WhereClause(whereToken = Where.default(), predicates = WherePredicateList())
    generics.whereClause = created
    return created
}

private object ReplaceImplTraitWithInfer : Fold() {
    override fun foldType(t: SynType): SynType =
        if (t is SynType.ImplTrait) {
            SynType.Infer(Underscore.from(t.implToken.span))
        } else {
            super.foldType(t)
        }
}

internal fun replaceImplTraitWithInfer(ty: SynType): SynType =
    ReplaceImplTraitWithInfer.foldType(ty)

internal fun parseQuoteWherePredicate(tokenStream: TokenStream): WherePredicate {
    val result: SynResult<WherePredicate> = parse2(WherePredicate.Companion::parse, tokenStream)
    return result.getOrThrowParseException()
}

internal fun parseQuoteReturnType(tokenStream: TokenStream): ReturnType {
    val result: SynResult<ReturnType> = parse2(ReturnType.Companion::parse, tokenStream)
    return result.getOrThrowParseException()
}
