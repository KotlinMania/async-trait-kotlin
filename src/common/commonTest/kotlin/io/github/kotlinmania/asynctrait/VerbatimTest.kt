package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.parse2
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VerbatimTest {
    @Test
    fun invalidRestrictedVisibilityRetainsItsDiagnostic() {
        val parsed = parse2(VerbatimFn::parse, quote("pub(in crate::) fn method();"))
        val error = assertNotNull(parsed.exceptionOrNull())
        assertTrue(error.message.orEmpty().contains("expected path segment after `::`"))
    }

    @Test
    fun malformedVerbatimFunctionsReturnFailures() {
        val sources = listOf("#[] fn method();", "fn ;", "fn method()")
        for (source in sources) {
            val parsed = parse2(VerbatimFn::parse, quote(source))

            assertTrue(parsed.isFailure, source)
            assertNotNull(parsed.exceptionOrNull(), source)
        }
    }

    @Test
    fun testVerbatimFnParsing() {
        val input = quote("pub fn test_fn();")
        val parsed = parse2(VerbatimFn::parse, input)
        assertTrue(parsed.isSuccess)
        val verbatimFn = parsed.getOrThrow()
        assertNotNull(verbatimFn)

        val output = TokenStream.new()
        verbatimFn.toTokens(output)
        val rendered = output.toString()
        assertTrue("test_fn" in rendered)
    }
}
