// port-lint: source lifetime.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.syn.Expr
import io.github.kotlinmania.syn.FnArg
import io.github.kotlinmania.syn.GenericArgument
import io.github.kotlinmania.syn.Lifetime
import io.github.kotlinmania.syn.ReceiverKind
import io.github.kotlinmania.syn.ReturnType
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.TypeParamBound
import io.github.kotlinmania.syn.TypeParamBoundList
import io.github.kotlinmania.syn.gen.VisitMut
import io.github.kotlinmania.syn.token.And
import io.github.kotlinmania.syn.token.Paren
import io.github.kotlinmania.syn.token.Plus

internal class CollectLifetimes(
    val elided: MutableList<Lifetime> = mutableListOf(),
    val explicit: MutableList<Lifetime> = mutableListOf(),
) : VisitMut() {
    fun visitOptLifetime(reference: And, scopeRef: Lifetime?): Lifetime =
        if (scopeRef == null) {
            nextLifetime(reference.span)
        } else {
            visitCapturedLifetime(scopeRef)
            scopeRef
        }

    fun visitCapturedLifetime(scopeRef: Lifetime) {
        if (scopeRef.ident.toString() == "_") {
            val next = nextLifetime(scopeRef.span())
            scopeRef.ident = next.ident
            scopeRef.apostrophe = next.apostrophe
        } else {
            explicit.add(scopeRef.deepCopy())
        }
    }

    fun nextLifetime(span: Span): Lifetime {
        val name = "'life${elided.size}"
        val life = Lifetime.new(name, span)
        elided.add(life.deepCopy())
        return life
    }

    override fun visitReceiver(receiver: FnArg.Receiver) {
        when (val kind = receiver.kind) {
            ReceiverKind.Value -> Unit
            is ReceiverKind.Reference -> {
                kind.lifetime = visitOptLifetime(kind.andToken, kind.lifetime)
            }
            is ReceiverKind.Typed -> {
                visitType(kind.ty)
            }
        }
    }

    override fun visitTypeReference(ty: SynType.Reference) {
        ty.lifetime = visitOptLifetime(ty.andToken, ty.lifetime)
        super.visitTypeReference(ty)
    }

    override fun visitGenericArgument(genArg: GenericArgument) {
        if (genArg is GenericArgument.LifetimeArg) {
            visitCapturedLifetime(genArg.lifetime)
        }
        super.visitGenericArgument(genArg)
    }

    companion object {
        fun new(): CollectLifetimes = CollectLifetimes()
    }
}

internal object AddLifetimeToImplTrait : VisitMut() {
    override fun visitTypeImplTrait(ty: SynType.ImplTrait) {
        val span = ty.implToken.span
        val scopeBound = TypeParamBound.LifetimeBound(Lifetime.new("'async_trait", span))
        val newBounds = TypeParamBoundList()
        newBounds.pushValue(scopeBound)
        for (bound in ty.bounds.toList()) {
            newBounds.pushPunct(Plus.from(span))
            newBounds.pushValue(bound)
        }
        ty.bounds = newBounds
        super.visitTypeImplTrait(ty)
    }

    override fun visitTypeReference(ty: SynType.Reference) {
        parenthesizeImplTrait(ty.elem, ty.andToken.span) { ty.elem = it }
        super.visitTypeReference(ty)
    }

    override fun visitTypePtr(ty: SynType.Ptr) {
        parenthesizeImplTrait(ty.elem, ty.starToken.span) { ty.elem = it }
        super.visitTypePtr(ty)
    }

    override fun visitTypeFnPtr(ty: SynType.FnPtr) {
        val output = ty.output
        if (output is ReturnType.TypeReturn) {
            parenthesizeImplTrait(output.ty, output.arrowToken.spans[0]) { output.ty = it }
        }
        super.visitTypeFnPtr(ty)
    }

    override fun visitExpr(e: Expr): Expr =
        // Do not recurse into impl Traits inside of an array length expression.
        e
}

internal fun parenthesizeImplTrait(
    elem: SynType,
    parenSpan: Span,
    assign: (SynType) -> Unit,
) {
    if (elem is SynType.ImplTrait) {
        assign(
            SynType.Paren(
                parenToken = Paren.from(parenSpan),
                elem = elem,
            ),
        )
    }
}
