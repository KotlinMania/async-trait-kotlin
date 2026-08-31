// port-lint: tests async-trait/src/bound.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.PathSegment
import io.github.kotlinmania.syn.PathSegmentList
import io.github.kotlinmania.syn.TraitBoundModifier
import io.github.kotlinmania.syn.TypeParamBound
import io.github.kotlinmania.syn.TypeParamBoundList
import io.github.kotlinmania.syn.token.PathSep
import io.github.kotlinmania.syn.token.Plus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoundTest {
    @Test
    fun detectsSimpleInferredBounds() {
        val supertraits = supertraits(simplePath("Send"))

        assertTrue(hasBound(supertraits, InferredBound.Send))
        assertFalse(hasBound(supertraits, InferredBound.Sync))
    }

    @Test
    fun detectsQualifiedMarkerBounds() {
        val supertraits = supertraits(qualifiedPath("core", "marker", "Sync"))

        assertTrue(hasBound(supertraits, InferredBound.Sync))
        assertFalse(hasBound(supertraits, InferredBound.Send))
    }

    @Test
    fun emitsCoreMarkerPathForInferredBound() {
        val tokens = TokenStream.new()

        InferredBound.Send.toTokens(tokens)

        val rendered = tokens.toString()
        assertTrue("core" in rendered)
        assertTrue("marker" in rendered)
        assertTrue("Send" in rendered)
    }
}

private fun supertraits(vararg paths: Path): Supertraits {
    val supertraits = TypeParamBoundList()
    for (path in paths) {
        if (!supertraits.emptyOrTrailing()) {
            supertraits.pushPunct(Plus.default())
        }
        supertraits.pushValue(TypeParamBound.Trait(null, TraitBoundModifier.None, null, path))
    }
    return supertraits
}

private fun simplePath(name: String): Path =
    Path.from(Ident.new(name, Span.callSite()))

private fun qualifiedPath(vararg names: String): Path {
    val segments = PathSegmentList()
    for (name in names) {
        if (!segments.emptyOrTrailing()) {
            segments.pushPunct(PathSep.default())
        }
        segments.pushValue(PathSegment.from(Ident.new(name, Span.callSite())))
    }
    return Path(null, segments)
}
