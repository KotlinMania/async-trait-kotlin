package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.quote.quote
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemBoundaryParityTest {
    @Test
    fun bracedConstInitializersInTraitAndImplPreserveItemAndExpandFollowingAsyncMethod() {
        val traitExpansion =
            expandAsyncTrait(
                "",
                """
                trait Config {
                    #[allow(dead_code)]
                    const LIMIT: usize = {
                        let base = 16;
                        base * 2
                    };
                    async fn fetch(&self) -> usize;
                }
                """.trimIndent(),
            )
        assertTrue(traitExpansion.isSuccess, "Trait expansion failed: ${traitExpansion.error}")
        val traitSource = assertNotNull(traitExpansion.source)
        containsTokens(traitSource, "#[allow(dead_code)] const LIMIT: usize = { let base = 16; base * 2 };")
        containsTokens(
            traitSource,
            "fn fetch<'life0, 'async_trait>(&'life0 self) -> ::core::pin::Pin<Box<dyn ::core::future::Future<Output = usize> + ::core::marker::Send + 'async_trait> >",
        )

        val implExpansion =
            expandAsyncTrait(
                "",
                """
                impl Config for Client {
                    #[allow(dead_code)]
                    const LIMIT: usize = {
                        let base = 16;
                        base * 2
                    };
                    async fn fetch(&self) -> usize {
                        Self::LIMIT
                    }
                }
                """.trimIndent(),
            )
        assertTrue(implExpansion.isSuccess, "Impl expansion failed: ${implExpansion.error}")
        val implSource = assertNotNull(implExpansion.source)
        containsTokens(implSource, "#[allow(dead_code)] const LIMIT: usize = { let base = 16; base * 2 };")
        assertTrue("Box :: pin (async move {" in implSource, implSource)
        containsTokens(implSource, "Self::LIMIT")
    }

    @Test
    fun constExpressionsContinuingAfterBracesKeepSuffixAndExpandNextAsyncMethod() {
        val traitExpansion =
            expandAsyncTrait(
                "",
                """
                trait Computed {
                    const SUM: usize = { 2 } + if true { 3 } else { 0 } * { 4 };
                    async fn compute(&self, extra: usize) -> usize;
                }
                """.trimIndent(),
            )
        assertTrue(traitExpansion.isSuccess, "Trait expansion failed: ${traitExpansion.error}")
        val traitSource = assertNotNull(traitExpansion.source)
        containsTokens(traitSource, "const SUM: usize = ({ 2 }) + if true { 3 } else { 0 } * ({ 4 });")
        containsTokens(traitSource, "fn compute<'life0, 'async_trait>(&'life0 self, extra: usize)")

        val implExpansion =
            expandAsyncTrait(
                "",
                """
                impl Computed for Calculator {
                    const SUM: usize = { 2 } + if true { 3 } else { 0 } * { 4 };
                    const OFFSET: i32 = Point { x: 4, y: 5 }.x + 1;
                    async fn compute(&self, extra: usize) -> usize {
                        Self::SUM + extra
                    }
                }
                """.trimIndent(),
            )
        assertTrue(implExpansion.isSuccess, "Impl expansion failed: ${implExpansion.error}")
        val implSource = assertNotNull(implExpansion.source)
        containsTokens(implSource, "const SUM: usize = ({ 2 }) + if true { 3 } else { 0 } * ({ 4 });")
        containsTokens(implSource, "const OFFSET: i32 = Point { x: 4, y: 5 }.x + 1;")
        containsTokens(implSource, "let extra = extra;")
        assertTrue("Box :: pin (async move {" in implSource, implSource)
    }

    @Test
    fun bracedAssociatedTypeItemsPreserveBoundsAndOpaqueImplTraitDetection() {
        val traitExpansion =
            expandAsyncTrait(
                "",
                """
                trait PacketChannel {
                    type Packet: Into<make_type! { usize }>;
                    async fn send(&self, packet: Self::Packet);
                }
                """.trimIndent(),
            )
        assertTrue(traitExpansion.isSuccess, "Trait expansion failed: ${traitExpansion.error}")
        val traitSource = assertNotNull(traitExpansion.source)
        containsTokens(traitSource, "type Packet: Into<make_type! { usize }>;")
        containsTokens(traitSource, "fn send<'life0, 'async_trait>(&'life0 self, packet: Self::Packet)")

        val implExpansion =
            expandAsyncTrait(
                "",
                """
                impl PacketChannel for Transport {
                    type Packet = make_type! { usize };
                    type Opaque = impl Into<make_type! { usize }>;
                    async fn send(&self, packet: Self::Packet) {}
                    async fn make(&self) -> Self::Opaque {}
                }
                """.trimIndent(),
            )
        assertTrue(implExpansion.isSuccess, "Impl expansion failed: ${implExpansion.error}")
        val implSource = assertNotNull(implExpansion.source)
        containsTokens(implSource, "type Packet = make_type! { usize };")
        containsTokens(implSource, "type Opaque = impl Into<make_type! { usize }>;")
        containsTokens(implSource, "let packet = packet;")
        containsTokens(implSource, "Future<Output = Self::Opaque>")
        absentTokens(implSource, "let __ret: Self::Opaque")
    }

    @Test
    fun synchronousFunctionsKeepBoundariesBeforeAsyncMethods() {
        val traitExpansion =
            expandAsyncTrait(
                "",
                """
                trait MixedItems {
                    fn sync_decl(&self) -> usize;
                    fn sync_calc(&self) -> usize {
                        let base = { 1 };
                        assert_eq!(base, 1);
                        base + 2
                    }
                    async fn run(&self) -> usize;
                }
                """.trimIndent(),
            )
        assertTrue(traitExpansion.isSuccess, "Trait expansion failed: ${traitExpansion.error}")
        val traitSource = assertNotNull(traitExpansion.source)
        containsTokens(traitSource, "fn sync_decl(&self) -> usize;")
        containsTokens(traitSource, "fn sync_calc(&self) -> usize { let base = { 1 }; assert_eq!(base, 1); base + 2 }")
        containsTokens(traitSource, "fn run<'life0, 'async_trait>(&'life0 self)")

        val implExpansion =
            expandAsyncTrait(
                "",
                """
                impl MixedItems for Worker {
                    fn sync_decl(&self) -> usize {
                        0
                    }
                    fn sync_calc(&self) -> usize {
                        let base = { 1 };
                        assert_eq!(base, 1);
                        base + 2
                    }
                    default fn sync_default(&self) -> usize {
                        let extra = { 3 };
                        extra
                    }
                    async fn run(&self) -> usize {
                        self.sync_calc()
                    }
                }
                """.trimIndent(),
            )
        assertTrue(implExpansion.isSuccess, "Impl expansion failed: ${implExpansion.error}")
        val implSource = assertNotNull(implExpansion.source)
        containsTokens(implSource, "fn sync_decl(&self) -> usize { 0 }")
        containsTokens(implSource, "fn sync_calc(&self) -> usize { let base = { 1 }; assert_eq!(base, 1); base + 2 }")
        containsTokens(implSource, "default fn sync_default(&self) -> usize { let extra = { 3 }; extra }")
        containsTokens(implSource, "__self.sync_calc()")
    }

    @Test
    fun bracedConstAndTypeItemsWithoutTerminatingSemicolonStillFailToParse() {
        val synResultFailureSources =
            listOf(
                "trait Broken { const LIMIT: usize = { 1 } }",
                "trait Broken { const LIMIT: usize = { 1 } async fn run(&self); }",
                "impl Broken for () { type Assoc = make_type! { usize } async fn run(&self) {} }",
                "impl Broken for () { type Assoc = make_type! { usize } const NEXT: usize = 2; }",
            )
        for (source in synResultFailureSources) {
            val expansion = expandAsyncTrait("", source)
            assertFalse(expansion.isSuccess, "Expected parse failure for: $source")
            assertNull(expansion.source, source)
            assertTrue(assertNotNull(expansion.error, source).isNotEmpty(), source)
        }
    }

    private fun containsTokens(output: String, expected: String) {
        val expectedRendered = quote(expected).toString()
        assertTrue(expectedRendered in output, "Expected `$expectedRendered` in `$output`")
    }

    private fun absentTokens(output: String, unexpected: String) {
        val unexpectedRendered = quote(unexpected).toString()
        assertFalse(unexpectedRendered in output, "Unexpected `$unexpectedRendered` in `$output`")
    }
}
