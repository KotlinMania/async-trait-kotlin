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
import io.github.kotlinmania.syn.gen.Visit
import io.github.kotlinmania.syn.parse2
import io.github.kotlinmania.syn.parseQuoteAttribute
import io.github.kotlinmania.syn.parseQuotePat
import io.github.kotlinmania.syn.parseQuoteStmtList
import io.github.kotlinmania.syn.token.Comma
import io.github.kotlinmania.syn.token.Gt
import io.github.kotlinmania.syn.token.Lt
import io.github.kotlinmania.syn.token.Plus
import io.github.kotlinmania.syn.token.Underscore
import io.github.kotlinmania.syn.token.Where

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
    when (input) {
        is Item.Trait -> {
            val traitItem = input.item
            val context = Context.Trait(traitItem.generics, traitItem.supertraits)
            for (inner in traitItem.items) {
                if (inner is TraitItem.Fn) {
                    val sig = inner.sig
                    if (sig.asyncness != null) {
                        val block = inner.default
                        var hasSelf = hasSelfInSig(sig)
                        val mustUseAttr = parseQuoteAttribute(quote("#[must_use]"))
                        inner.attrs = inner.attrs + mustUseAttr
                        if (block != null) {
                            hasSelf = hasSelf || hasSelfInBlock(block)
                            transformBlock(context, sig, block)
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
            val associatedTypeImplTraits = mutableSetOf<String>()
            for (inner in implItem.items) {
                if (inner is ImplItem.AssocType && inner.ty is SynType.ImplTrait) {
                    associatedTypeImplTraits.add(inner.ident.toString())
                }
            }
            val context = Context.Impl(implItem.generics, associatedTypeImplTraits)
            for (inner in implItem.items) {
                if (inner is ImplItem.Fn) {
                    val sig = inner.sig
                    if (sig.asyncness != null) {
                        val hasSelf = hasSelfInSig(sig) || hasSelfInBlock(inner.block)
                        transformBlock(context, sig, inner.block)
                        transformSig(context, sig, hasSelf, false, isLocal)
                        inner.attrs = inner.attrs + lintSuppressWithBody()
                    }
                } else if (inner is ImplItem.Verbatim) {
                    val verbatimResult = parse2(VerbatimFn::parse, inner.tokens)
                    if (verbatimResult is SynResult.Success) {
                        val verbatim = verbatimResult.value
                        val sig = verbatim.sig
                        if (sig.asyncness != null) {
                            val hasSelf = hasSelfInSig(sig)
                            transformSig(context, sig, hasSelf, false, isLocal)
                            verbatim.attrs.add(lintSuppressWithoutBody())
                            inner.tokens = quote("#verbatim", "verbatim" to verbatim)
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

private fun transformSig(
    context: Context,
    sig: Signature,
    hasSelf: Boolean,
    hasDefault: Boolean,
    isLocal: Boolean,
) {
    sig.asyncness = null

    val (retArrow, ret) =
        when (val out = sig.output) {
            is ReturnType.Default -> quote("->") to quote("()")
            is ReturnType.TypeReturn -> quote("#arrow", "arrow" to out.arrowToken) to quote("#ty", "ty" to out.ty)
        }

    val lifetimes = CollectLifetimes()
    for (arg in sig.inputs) {
        when (arg) {
            is FnArg.Receiver -> lifetimes.visitReceiver(arg)
            is FnArg.Typed -> lifetimes.visitType(arg.patType.ty)
        }
    }

    for (param in sig.generics.params) {
        when (param) {
            is GenericParam.TypeParam -> {
                val paramName = param.ident
                val span = param.colonToken?.span ?: paramName.span()
                if (param.attrs.isEmpty()) {
                    val bounds = param.bounds
                    param.bounds = TypeParamBoundList()
                    param.colonToken = null
                    val predTokens =
                        if (bounds.isEmpty()) {
                            quote("#paramName: 'async_trait", "paramName" to paramName)
                        } else {
                            quote("#paramName: 'async_trait + #bounds", "paramName" to paramName, "bounds" to bounds)
                        }
                    val pred = parseQuoteWherePredicate(predTokens)
                    whereClauseOrDefault(sig.generics).predicates.push(pred) { Comma.default() }
                } else {
                    param.bounds.push(TypeParamBound.LifetimeBound(Lifetime.new("'async_trait", span))) { Plus.default() }
                }
            }
            is GenericParam.LifetimeParam -> {
                val paramName = param.lifetime
                val span = param.colonToken?.span ?: paramName.span()
                if (param.attrs.isEmpty()) {
                    val bounds = param.bounds
                    param.bounds = LifetimeList()
                    param.colonToken = null
                    val predTokens =
                        if (bounds.isEmpty()) {
                            quote("#paramName: 'async_trait", "paramName" to paramName)
                        } else {
                            quote("#paramName: 'async_trait + #bounds", "paramName" to paramName, "bounds" to bounds)
                        }
                    val pred = parseQuoteWherePredicate(predTokens)
                    whereClauseOrDefault(sig.generics).predicates.push(pred) { Comma.default() }
                } else {
                    param.bounds.push(Lifetime.new("'async_trait", span)) { Plus.default() }
                }
            }
            is GenericParam.ConstParam -> {}
        }
    }

    for (param in context.lifetimes(lifetimes.explicit)) {
        val paramLifetime = param.lifetime
        val pred = parseQuoteWherePredicate(quote("#param: 'async_trait", "param" to paramLifetime))
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
        val pred = parseQuoteWherePredicate(quote("#elided: 'async_trait", "elided" to elided))
        whereClauseOrDefault(sig.generics).predicates.push(pred) { Comma.default() }
    }

    sig.generics.params.push(GenericParam.LifetimeParam.new(Lifetime.new("'async_trait", Span.callSite()))) { Comma.default() }

    if (hasSelf) {
        val bounds: List<InferredBound> =
            if (isLocal) {
                emptyList()
            } else {
                val receiverType: SynType? =
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
                if (receiverType != null) {
                    if (receiverType is SynType.Reference && receiverType.mutability == null) {
                        listOf(InferredBound.Sync)
                    } else if (receiverType is SynType.Path && isArcSelf(receiverType)) {
                        listOf(InferredBound.Sync, InferredBound.Send)
                    } else {
                        listOf(InferredBound.Send)
                    }
                } else {
                    listOf(InferredBound.Send)
                }
            }

        val filteredBounds =
            bounds.filter { bound ->
                when (context) {
                    is Context.Trait -> hasDefault && !hasBound(context.supertraits, bound)
                    is Context.Impl -> false
                }
            }

        val selfPredTokens =
            if (filteredBounds.isEmpty()) {
                quote("Self: 'async_trait")
            } else {
                val bTokens = TokenStream.new()
                for (b in filteredBounds) {
                    b.toTokens(bTokens)
                    Plus.default().toTokens(bTokens)
                }
                quote("Self: #bTokens 'async_trait", "bTokens" to bTokens)
            }
        whereClauseOrDefault(sig.generics).predicates.push(parseQuoteWherePredicate(selfPredTokens)) { Comma.default() }
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
                        val newPatTokens = if (m != null) quote("#m #pos", "m" to m, "pos" to positional) else quote("#pos", "pos" to positional)
                        arg.patType.pat = parseQuotePat(newPatTokens)
                    }
                }
                AddLifetimeToImplTrait.visitType(arg.patType.ty)
            }
        }
    }

    val boundsToken = if (isLocal) quote("'async_trait") else quote("::core::marker::Send + 'async_trait")
    sig.output =
        parseQuoteReturnType(
            quote(
                "#retArrow ::core::pin::Pin<Box<dyn ::core::future::Future<Output = #ret> + #boundsToken>>",
                "retArrow" to retArrow,
                "ret" to ret,
                "boundsToken" to boundsToken,
            ),
        )
}

private fun transformBlock(context: Context, sig: Signature, block: Block) {
    var replaceSelf = false
    val decls = mutableListOf<TokenStream>()
    for ((i, arg) in sig.inputs.withIndex()) {
        when (arg) {
            is FnArg.Receiver -> {
                replaceSelf = true
                val ident = Ident.new("__self", arg.selfToken.span)
                val mutability = arg.mutability
                decls.add(quote("let #mutability #ident = #self;", "mutability" to mutability, "ident" to ident, "self" to arg.selfToken))
            }
            is FnArg.Typed -> {
                val attrs = arg.patType.attrs.filter { it.path().isIdent("cfg") }
                if (arg.patType.ty is SynType.Reference) {
                    // Nothing
                } else if (arg.patType.pat is Pat.Ident) {
                    val pat = arg.patType.pat as Pat.Ident
                    val ident = pat.ident
                    val mutability = pat.mutability
                    decls.add(quote("#(#attrs)* let #mutability #ident = #ident;", "attrs" to attrs, "mutability" to mutability, "ident" to ident))
                } else {
                    val pat = arg.patType.pat
                    val ident = positionalArg(i, pat)
                    if (pat is Pat.Wild) {
                        decls.add(quote("#(#attrs)* let #ident = #ident;", "attrs" to attrs, "ident" to ident))
                    } else {
                        decls.add(quote("#(#attrs)* let #pat = { let #ident = #ident; #ident };", "attrs" to attrs, "pat" to pat, "ident" to ident))
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
            is ReturnType.Default -> {
                quote("#(#decls)* let _: () = #block;", "decls" to decls, "block" to block)
            }
            is ReturnType.TypeReturn -> {
                val ret = out.ty
                if (containsAssociatedTypeImplTrait(context, ret)) {
                    if (decls.isEmpty()) {
                        quote("#(#stmts)*", "stmts" to block.stmts)
                    } else {
                        quote("#(#decls)* #block", "decls" to decls, "block" to block)
                    }
                } else {
                    val retCopy = replaceImplTraitWithInfer(ret.deepCopy())
                    quote(
                        "if let ::core::option::Option::Some(__ret) = ::core::option::Option::None::<#ret> { #[allow(unreachable_code)] return __ret; } #(#decls)* let __ret: #ret = #block; #[allow(unreachable_code)] __ret",
                        "ret" to retCopy,
                        "decls" to decls,
                        "block" to block,
                    )
                }
            }
        }

    val boxPin = quote("Box::pin(async move { #letRet })", "letRet" to letRet)
    block.stmts = parseQuoteStmtList(boxPin)
}

private fun isArcSelf(ty: SynType.Path): Boolean {
    val segment = ty.path.segments.last() ?: return false
    if (segment.ident.toString() != "Arc") return false
    val args = segment.arguments
    if (args is PathArguments.AngleBracketed && args.args.size == 1) {
        val firstArg = args.args[0]
        if (firstArg is GenericArgument.TypeArg) {
            val innerTy = firstArg.type
            if (innerTy is SynType.Path && innerTy.path.isIdent("Self")) {
                return true
            }
        }
    }
    return false
}

private fun positionalArg(i: Int, pat: Pat): Ident =
    Ident.new("__arg$i", pat.spanOrCallSite())

private fun Pat.spanOrCallSite(): Span = Span.callSite()

internal class AssociatedTypeImplTraits(
    val set: Set<String>,
    var contains: Boolean = false,
) : Visit() {
    override fun visitTypePath(t: SynType.Path) {
        if (t.qself == null &&
            t.path.segments.size == 2 &&
            t.path.segments[0].ident.toString() == "Self" &&
            t.path.segments[1].ident.toString() in set
        ) {
            contains = true
        }
        super.visitTypePath(t)
    }
}

private fun containsAssociatedTypeImplTrait(context: Context, ret: SynType): Boolean =
    when (context) {
        is Context.Trait -> false
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

private object ReplaceImplTraitWithInfer : Fold() {
    override fun foldType(t: SynType): SynType =
        if (t is SynType.ImplTrait) {
            SynType.Infer(Underscore.from(t.implToken.span))
        } else {
            super.foldType(t)
        }
}

private fun replaceImplTraitWithInfer(ty: SynType): SynType =
    ReplaceImplTraitWithInfer.foldType(ty)

internal fun parseQuoteWherePredicate(tokenStream: TokenStream): WherePredicate {
    val result: SynResult<WherePredicate> = parse2(WherePredicate.Companion::parse, tokenStream)
    return result.fold(
        onSuccess = { it },
        onFailure = { throw it },
    )
}

internal fun parseQuoteReturnType(tokenStream: TokenStream): ReturnType {
    val result: SynResult<ReturnType> = parse2(ReturnType.Companion::parse, tokenStream)
    return result.fold(
        onSuccess = { it },
        onFailure = { throw it },
    )
}
