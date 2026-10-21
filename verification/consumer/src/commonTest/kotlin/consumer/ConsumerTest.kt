package consumer

import io.github.kotlinmania.asynctrait.asyncTrait
import io.github.kotlinmania.asynctrait.asyncTraitOrThrow
import io.github.kotlinmania.asynctrait.expandAsyncTrait
import io.github.kotlinmania.asynctrait.parseArgs
import io.github.kotlinmania.quote.quote
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsumerTest {
    @Test
    fun consumesPublishedApiAndItsExposedDependencies() {
        val result = expand().getOrThrow().toString()
        assertTrue("Future" in result)
        assertTrue("Send" in result)
        assertFalse(options().local)
        assertTrue(inferredBound().toString() == "Send")
    }

    @Test
    fun consumesConcreteFailureMessages() {
        val source = "trait Trait { fn broken() { 1 + } }"
        val result = asyncTrait(quote(""), quote(source))
        val diagnostic = assertNotNull(result.exceptionOrNull())
        val message: String = assertNotNull(diagnostic.message)
        assertTrue(message.isNotEmpty())

        val expansion = expandAsyncTrait("", source)
        assertFalse(expansion.isSuccess)
        assertNull(expansion.source)
        assertTrue(assertNotNull(expansion.error).isNotEmpty())
    }

    @Test
    fun retainsThePublicThrowingBoundary() {
        val failure =
            assertFailsWith<IllegalArgumentException> {
                asyncTraitOrThrow(quote("?"), quote("trait Trait {}"))
            }

        assertTrue(assertNotNull(failure.message).contains("expected"))
    }

    @Test
    fun consumesConcreteArgumentResults() {
        val result = parseArgs(quote("?Send"))
        assertTrue(assertNotNull(result.value).local)
        assertNull(result.error)

        val failure = parseArgs(quote("?Sync"))
        assertNull(failure.value)
        val message: String = assertNotNull(assertNotNull(failure.error).message)
        assertTrue(message.contains("expected"))
    }
}
