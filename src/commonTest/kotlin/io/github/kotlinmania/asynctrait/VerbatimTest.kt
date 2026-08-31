// port-lint: tests async-trait/src/verbatim.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.parse2
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VerbatimTest {
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
