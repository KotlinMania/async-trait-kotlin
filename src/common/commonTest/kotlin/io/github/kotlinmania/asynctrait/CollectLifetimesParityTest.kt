package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.FnArg
import io.github.kotlinmania.syn.Signature
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.parse2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CollectLifetimesParityTest {
    @Test
    fun explicitReferenceLifetimeIsCollectedOnceWithoutElidedEntries() {
        val ty = parseType("&'a T")
        val collector = CollectLifetimes.new()

        collector.visitType(ty)

        assertEquals(emptyList(), collector.elided.map { it.toString() })
        assertEquals(listOf("'a"), collector.explicit.map { it.toString() })
        assertEquals(quote("&'a T").toString(), quote("`#`ty", "ty" to ty).toString())
    }

    @Test
    fun elidedReferenceAndReceiverLifetimesStayElidedOnly() {
        val sig = parseSignature("async fn method(&self, value: &T)")
        val collector = CollectLifetimes.new()

        for (arg in sig.inputs) {
            when (arg) {
                is FnArg.Receiver -> collector.visitReceiver(arg)
                is FnArg.Typed -> collector.visitType(arg.patType.ty)
            }
        }

        assertEquals(listOf("'life0", "'life1"), collector.elided.map { it.toString() })
        assertEquals(emptyList(), collector.explicit.map { it.toString() })
        assertEquals(
            quote("async fn method(&'life0 self, value: &'life1 T)").toString(),
            quote("`#`sig", "sig" to sig).toString(),
        )
    }

    @Test
    fun placeholderReferenceGeneratesSingleElidedLifetimeWithoutExplicitEntry() {
        val ty = parseType("&'_ T")
        val collector = CollectLifetimes.new()

        collector.visitType(ty)

        assertEquals(listOf("'life0"), collector.elided.map { it.toString() })
        assertEquals(emptyList(), collector.explicit.map { it.toString() })
        assertEquals(quote("&'life0 T").toString(), quote("`#`ty", "ty" to ty).toString())
    }

    @Test
    fun genericLifetimeArgumentsCollectExplicitOnceAndPlaceholderAsElidedOnly() {
        val explicitGeneric = parseType("Foo<'a>")
        val explicitCollector = CollectLifetimes.new()
        explicitCollector.visitType(explicitGeneric)

        assertEquals(emptyList(), explicitCollector.elided.map { it.toString() })
        assertEquals(listOf("'a"), explicitCollector.explicit.map { it.toString() })

        val placeholderGeneric = parseType("Foo<'_>")
        val placeholderCollector = CollectLifetimes.new()
        placeholderCollector.visitType(placeholderGeneric)

        assertEquals(listOf("'life0"), placeholderCollector.elided.map { it.toString() })
        assertEquals(emptyList(), placeholderCollector.explicit.map { it.toString() })
        assertEquals(
            quote("Foo<'life0>").toString(),
            quote("`#`ty", "ty" to placeholderGeneric).toString(),
        )
    }

    @Test
    fun nestedReferencesPreserveOuterBeforeInnerOrderWithoutDuplicates() {
        val explicitNested = parseType("&'a &'b T")
        val explicitCollector = CollectLifetimes.new()
        explicitCollector.visitType(explicitNested)

        assertEquals(emptyList(), explicitCollector.elided.map { it.toString() })
        assertEquals(listOf("'a", "'b"), explicitCollector.explicit.map { it.toString() })

        val elidedNested = parseType("& &T")
        val elidedCollector = CollectLifetimes.new()
        elidedCollector.visitType(elidedNested)

        assertEquals(listOf("'life0", "'life1"), elidedCollector.elided.map { it.toString() })
        assertEquals(emptyList(), elidedCollector.explicit.map { it.toString() })
        assertEquals(
            quote("&'life0 &'life1 T").toString(),
            quote("`#`ty", "ty" to elidedNested).toString(),
        )
    }

    @Test
    fun unrelatedDeclaredAndTraitBoundLifetimesAreNotCollectedOrRewritten() {
        val sig =
            parseSignature(
                "async fn method<'d>(boxed: Box<dyn Send + 'a>, anon: Box<dyn Send + '_>, cb: for<'b> fn(i32))",
            )
        val collector = CollectLifetimes.new()

        // Even full signature traversal must not collect declared method lifetimes,
        // higher-ranked binder lifetimes, or trait-object lifetime bounds.
        collector.visitSignature(sig)

        assertEquals(emptyList(), collector.elided.map { it.toString() })
        assertEquals(emptyList(), collector.explicit.map { it.toString() })
        assertEquals(
            quote("async fn method<'d>(boxed: Box<dyn Send + 'a>, anon: Box<dyn Send + '_>, cb: for<'b> fn(i32))")
                .toString(),
            quote("`#`sig", "sig" to sig).toString(),
        )
    }

    @Test
    fun publicExpansionDoesNotRewriteTraitObjectBoundsOrBindUnusedEnclosingLifetimes() {
        val placeholderBound =
            expandAsyncTrait(
                "",
                "trait Service { async fn handle(item: Box<dyn Send + '_>, stream: impl Send + '_); }",
            )
        assertTrue(placeholderBound.isSuccess)
        val placeholderSource = assertNotNull(placeholderBound.source)
        assertTrue(quote("Box<dyn Send + '_>").toString() in placeholderSource, placeholderSource)
        assertTrue(quote("impl 'async_trait + Send + '_").toString() in placeholderSource, placeholderSource)
        assertFalse("'life0" in placeholderSource, placeholderSource)

        val boundAndBinderOnly =
            expandAsyncTrait(
                "",
                "trait Scoped<'a> { async fn run(boxed: Box<dyn Send + 'a>, cb: for<'a> fn(i32)); }",
            )
        assertTrue(boundAndBinderOnly.isSuccess)
        val scopedSource = assertNotNull(boundAndBinderOnly.source)
        assertFalse(quote("'a: 'async_trait").toString() in scopedSource, scopedSource)

        val collidingElidedName =
            asyncTraitOrThrow(
                quote(""),
                quote("trait Collide<'life0> { async fn check(input: &str); }"),
            ).toString()
        val predicateToken = quote("'life0: 'async_trait").toString()
        assertEquals(
            1,
            collidingElidedName.split(predicateToken).size - 1,
            "Expected single generated lifetime predicate in: $collidingElidedName",
        )
    }

    private fun parseType(source: String): SynType =
        parse2(SynType.Companion::parse, quote(source)).getOrThrow()

    private fun parseSignature(source: String): Signature =
        parse2(Signature.Companion::parse, quote(source)).getOrThrow()
}
