package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.parse2
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArgsTest {
    @Test
    fun acceptsOnlyEmptyOrQuestionSendArguments() {
        assertFalse(parseArgs(quote("")).getOrThrow().local)
        assertTrue(parseArgs(quote("?Send")).getOrThrow().local)
        for (source in listOf("?", "?Sync", "Send", "?Send,", "?Send extra", "??Send")) {
            val result = parseArgs(quote(source))
            assertTrue(result.isFailure, "Unexpectedly accepted argument list: $source")
        }
    }

    @Test
    fun parseStreamEntryPointsRetainConcreteArgumentResults() {
        val direct = parse2({ input -> SynResult.success(parseArgs(input)) }, quote("?Send"))
        val companion = parse2({ input -> SynResult.success(Args.parse(input)) }, quote(""))

        assertTrue(direct.getOrThrow().getOrThrow().local)
        assertFalse(companion.getOrThrow().getOrThrow().local)
    }

    @Test
    fun testArgs() {
        val nonLocal = Args(local = false)
        assertFalse(nonLocal.local)

        val local = Args(local = true)
        assertTrue(local.local)
    }
}
