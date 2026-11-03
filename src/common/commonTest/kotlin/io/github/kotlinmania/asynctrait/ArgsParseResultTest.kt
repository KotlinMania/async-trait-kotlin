package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.syn.SynError
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ArgsParseResultTest {
    @Test
    fun successfulResultRetainsTheOriginalArguments() {
        val args = Args(local = true)
        val result = ArgsParseResult.success(args)

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertSame(args, result.value)
        assertSame(args, result.getOrNull())
        assertSame(args, result.getOrThrow())
        assertNull(result.error)
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun failedResultRetainsTheOriginalDiagnosticAtTheThrowingBoundary() {
        val diagnostic = SynError.new(Span.callSite(), "invalid arguments")
        val result = ArgsParseResult.failure(diagnostic)

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertNull(result.value)
        assertNull(result.getOrNull())
        assertSame(diagnostic, result.error)
        assertSame(diagnostic, result.exceptionOrNull())

        val failure = assertFailsWith<AsyncTraitParseException> { result.getOrThrow() }
        assertSame(diagnostic, failure.diagnostic)
    }
}
