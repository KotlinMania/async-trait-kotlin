// port-lint: source async-trait/src/lifetime.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.syn.FnArg
import io.github.kotlinmania.syn.GenericArgument
import io.github.kotlinmania.syn.Lifetime
import io.github.kotlinmania.syn.ReturnType
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.TypeParamBound
import io.github.kotlinmania.syn.TypeParamBoundList
import io.github.kotlinmania.syn.gen.VisitMut
import io.github.kotlinmania.syn.token.And
import io.github.kotlinmania.syn.token.Paren
import io.github.kotlinmania.syn.token.Plus

internal class CollectLifetimes : VisitMut() {
    public val elided: MutableList<Lifetime> = mutableListOf()
    public val explicit: MutableList<Lifetime> = mutableListOf()

    private fun visitOptLifetime(reference: And, lifetimeRef: Lifetime?): Lifetime =
        if (lifetimeRef == null) {
            nextLifetime(reference.span)
        } else {
            visitLifetime(lifetimeRef)
            lifetimeRef
        }

    override fun visitLifetime(lt: Lifetime) {
        if (lt.ident.toString() == "_") {
            val next = nextLifetime(lt.span())
            lt.ident = next.ident
            lt.apostrophe = next.apostrophe
        } else {
            explicit.add(lt.deepCopy())
        }
    }

    private fun nextLifetime(span: Span): Lifetime {
        val name = "'life${elided.size}"
        val life = Lifetime.new(name, span)
        elided.add(life.deepCopy())
        return life
    }

    override fun visitReceiver(receiver: FnArg.Receiver) {
        val ref = receiver.reference
        if (ref != null) {
            val updated = visitOptLifetime(ref.andToken, ref.lifetime)
            ref.lifetime = updated
        } else {
            visitType(receiver.type)
        }
    }

    override fun visitTypeReference(ty: SynType.Reference) {
        val updated = visitOptLifetime(ty.andToken, ty.lifetime)
        ty.lifetime = updated
        super.visitTypeReference(ty)
    }

    override fun visitGenericArgument(genArg: GenericArgument) {
        if (genArg is GenericArgument.LifetimeArg) {
            visitLifetime(genArg.lifetime)
        }
        super.visitGenericArgument(genArg)
    }
}

internal object AddLifetimeToImplTrait : VisitMut() {
    override fun visitTypeImplTrait(ty: SynType.ImplTrait) {
        val span = ty.implToken.span
        val lifetimeBound = TypeParamBound.LifetimeBound(Lifetime.new("'async_trait", span))
        val newBounds = TypeParamBoundList()
        newBounds.pushValue(lifetimeBound)
        for (bound in ty.bounds.toList()) {
            newBounds.pushPunct(Plus.from(span))
            newBounds.pushValue(bound)
        }
        ty.bounds = newBounds
        super.visitTypeImplTrait(ty)
    }

    override fun visitTypeReference(ty: SynType.Reference) {
        parenthesizeImplTrait(ty.elem) { ty.elem = it }
        super.visitTypeReference(ty)
    }

    override fun visitTypePtr(ty: SynType.Ptr) {
        parenthesizeImplTrait(ty.elem) { ty.elem = it }
        super.visitTypePtr(ty)
    }

    override fun visitTypeBareFn(ty: SynType.BareFn) {
        val output = ty.output
        if (output is ReturnType.TypeReturn) {
            parenthesizeImplTrait(output.ty) { output.ty = it }
        }
        super.visitTypeBareFn(ty)
    }

    override fun visitExpr(e: io.github.kotlinmania.syn.Expr): io.github.kotlinmania.syn.Expr {
        // Do not recurse into impl Traits inside of an array length expression.
        return e
    }
}

private fun parenthesizeImplTrait(elem: SynType, setter: (SynType) -> Unit) {
    if (elem is SynType.ImplTrait) {
        setter(
            SynType.Paren(
                parenToken = Paren.default(),
                elem = elem,
            ),
        )
    }
}
