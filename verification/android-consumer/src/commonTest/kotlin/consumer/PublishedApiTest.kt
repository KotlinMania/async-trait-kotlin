package consumer

import io.github.kotlinmania.asynctrait.asyncTrait
import io.github.kotlinmania.quote.quote
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublishedApiTest {
    @Test
    fun expandsRealAsyncMethodsThroughThePublishedResultApi() {
        val source = expand().getOrThrow().toString()
        assertTrue("Future" in source)
        assertTrue("Send" in source)
    }

    @Test
    fun expandsThroughThePublishedThrowingApiAndQuoteContract() {
        assertTrue("Future" in expandOrThrow().toString())
        assertTrue(inferredBound().toString() == "Send")
    }

    @Test
    fun preservesMalformedArgumentDiagnosticsThroughPublishedDependencies() {
        val result = asyncTrait(quote("?"), quote("trait Consumer {}"))
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        val diagnostic = assertNotNull(result.exceptionOrNull())
        assertTrue(assertNotNull(diagnostic.message).contains("expected"))
    }
}
