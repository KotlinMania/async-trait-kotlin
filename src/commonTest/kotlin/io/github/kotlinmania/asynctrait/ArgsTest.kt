// port-lint: tests async-trait/src/args.rs
package io.github.kotlinmania.asynctrait

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArgsTest {
    @Test
    fun testArgs() {
        val nonLocal = Args(local = false)
        assertFalse(nonLocal.local)

        val local = Args(local = true)
        assertTrue(local.local)
    }
}
