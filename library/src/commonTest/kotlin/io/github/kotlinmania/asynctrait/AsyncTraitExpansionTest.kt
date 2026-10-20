package io.github.kotlinmania.asynctrait

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AsyncTraitExpansionTest {
    @Test
    fun expandsTextThroughTheSameMacroImplementation() {
        val result = expandAsyncTrait("", "trait Trait { async fn f(&self); }")
        assertTrue(result.isSuccess)
        assertTrue("Future" in assertNotNull(result.source))
        assertNull(result.error)
    }

    @Test
    fun reportsArgumentErrorsWithoutLosingTheDiagnostic() {
        val result = expandAsyncTrait("?", "trait Trait {}")
        assertFalse(result.isSuccess)
        assertNull(result.source)
        assertTrue("expected" in assertNotNull(result.error))
    }

    @Test
    fun reportsUnbalancedSourceDelimiters() {
        val result = expandAsyncTrait("", "trait Trait {")
        assertFalse(result.isSuccess)
        assertNull(result.source)
        assertNotNull(result.error)
    }

    @Test
    fun reportsMalformedTraitAndImplementationBodies() {
        val sources =
            listOf(
                "trait Trait { fn broken() { 1 + } }",
                "impl Trait for () { fn broken() { 1 + } }",
            )
        for (source in sources) {
            val result = expandAsyncTrait("", source)

            assertFalse(result.isSuccess, source)
            assertNull(result.source, source)
            assertTrue(assertNotNull(result.error, source).isNotEmpty(), source)
        }
    }
}
