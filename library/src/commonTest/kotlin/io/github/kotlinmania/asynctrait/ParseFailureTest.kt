package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.SynResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ParseFailureTest {
    @Test
    fun successfulResultRetainsItsTokenStream() {
        val tokens = quote("trait Service {}")
        val result = SynResult.success(tokens)

        assertSame(tokens, result.getOrThrowParseException())
    }

    @Test
    fun throwingBoundaryRetainsTheOriginalDiagnostic() {
        val diagnostic = SynError.new(Span.callSite(), "invalid method")
        val result: SynResult<TokenStream> = SynResult.failure(diagnostic)

        val failure = assertFailsWith<AsyncTraitParseException> { result.getOrThrowParseException() }

        assertSame(diagnostic, failure.diagnostic)
        assertEquals(diagnostic.message, failure.message)
    }

    @Test
    fun publicThrowingApiRejectsMalformedArguments() {
        val failure = assertFailsWith<IllegalArgumentException> {
            asyncTraitOrThrow(quote("?"), quote("trait Service {}"))
        }

        assertTrue(assertNotNull(failure.message).contains("expected"))
    }

    @Test
    fun wherePredicateHelperRetainsParseFailureBehavior() {
        val failure = assertFailsWith<AsyncTraitParseException> {
            parseQuoteWherePredicate(quote("?"))
        }

        assertTrue(assertNotNull(failure.message).isNotEmpty())
        assertEquals(failure.diagnostic.message, failure.message)
    }

    @Test
    fun returnTypeHelperRetainsParseFailureBehavior() {
        val failure = assertFailsWith<AsyncTraitParseException> {
            parseQuoteReturnType(quote("->"))
        }

        assertTrue(assertNotNull(failure.message).isNotEmpty())
        assertEquals(failure.diagnostic.message, failure.message)
    }
}
