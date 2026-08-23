// port-lint: source tests/test.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncTraitTest {
    @Test
    fun expandsBasicAsyncTrait() {
        val input =
            quote(
                """
                trait Trait {
                    type Assoc;
                    async fn selfvalue(self) where Self: Sized {}
                    async fn selfref(&self) {}
                    async fn selfmut(&mut self) {}
                    async fn required() -> Self::Assoc;
                    async fn elided_lifetime(_x: &str) {}
                    async fn explicit_lifetime<'a>(_x: &'a str) {}
                }
                """.trimIndent(),
            )

        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()

        assertTrue("Pin" in rendered, "Expected Pin in expansion: $rendered")
        assertTrue("Future" in rendered, "Expected Future in expansion: $rendered")
        assertTrue("'async_trait" in rendered, "Expected 'async_trait lifetime in expansion: $rendered")
        assertTrue("Send" in rendered, "Expected Send bound in expansion: $rendered")
        assertTrue("must_use" in rendered, "Expected must_use attribute on trait methods: $rendered")
    }

    @Test
    fun expandsAsyncTraitImpl() {
        val input =
            quote(
                """
                impl Trait for Struct {
                    type Assoc = ();
                    async fn selfvalue(self) {}
                    async fn selfref(&self) {}
                    async fn selfmut(&mut self) {}
                    async fn required() -> Self::Assoc {}
                    async fn elided_lifetime(_x: &str) {}
                    async fn explicit_lifetime<'a>(_x: &'a str) {}
                }
                """.trimIndent(),
            )

        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()

        assertTrue("Box :: pin" in rendered || "Box::pin" in rendered, "Expected Box::pin in impl body: $rendered")
        assertTrue("async move" in rendered, "Expected async move block: $rendered")
        assertTrue("__self" in rendered, "Expected __self binding: $rendered")
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun expandsLocalAsyncTraitWithQuestionSend() {
        val args = quote("?Send")
        val input =
            quote(
                """
                trait NonSendTrait {
                    async fn run(&self) -> i32;
                }
                """.trimIndent(),
            )

        val output = asyncTraitOrThrow(args, input)
        val rendered = output.toString()

        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertFalse(":: core :: marker :: Send" in rendered || "::core::marker::Send" in rendered, "Local trait should not require Send: $rendered")
    }

    @Test
    fun expandsLocalAsyncTraitImplWithQuestionSend() {
        val args = quote("?Send")
        val input =
            quote(
                """
                impl NonSendTrait for Struct {
                    async fn run(&self) -> i32 {
                        42
                    }
                }
                """.trimIndent(),
            )

        val output = asyncTraitOrThrow(args, input)
        val rendered = output.toString()

        assertTrue("Box :: pin" in rendered || "Box::pin" in rendered, "Expected Box::pin in impl body: $rendered")
        assertTrue("async move" in rendered, "Expected async move block: $rendered")
    }

    @Test
    fun expandsPatternArguments() {
        val input =
            quote(
                """
                impl Trait for Struct {
                    async fn patterns(&self, (a, b): (i32, i32), mut c: i32, _: i32) -> i32 {
                        a + b + c
                    }
                }
                """.trimIndent(),
            )

        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()

        assertTrue("Box :: pin" in rendered || "Box::pin" in rendered, "Expected Box::pin in impl: $rendered")
        assertTrue("__arg" in rendered, "Expected positional argument binding: $rendered")
    }

    @Test
    fun expandsArcSelfReceiver() {
        val input =
            quote(
                """
                trait ArcTrait {
                    async fn arc_method(self: Arc<Self>) -> i32;
                }
                """.trimIndent(),
            )

        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()

        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertTrue("Self : 'async_trait" in rendered || "Self: 'async_trait" in rendered || "'async_trait" in rendered, "Expected 'async_trait bound: $rendered")
    }

    @Test
    fun expandsTraitWithoutSelf() {
        val input =
            quote(
                """
                trait StaticTrait {
                    async fn static_async() -> String;
                }
                """.trimIndent(),
            )

        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()

        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertTrue("Future" in rendered, "Expected Future in signature: $rendered")
    }

    @Test
    fun rejectsNonTraitItem() {
        val input = quote("struct Foo;")
        val result = asyncTrait(TokenStream.new(), input)
        assertTrue(result.isFailure, "Expected failure when applying macro to struct")
    }

    @Test
    fun rejectsInvalidArgs() {
        val args = quote("invalid_arg")
        val input = quote("trait Foo { async fn bar(&self); }")
        val result = asyncTrait(args, input)
        assertTrue(result.isFailure, "Expected failure on invalid args")
    }

    @Test
    fun expandsGenericMethodInTrait() {
        val input =
            quote(
                """
                pub trait Issue1 {
                    async fn f<U>(&self);
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun expandsGenericMethodInImpl() {
        val input =
            quote(
                """
                impl<T: Sync> Issue1 for Vec<T> {
                    async fn f<U>(&self) {}
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Box :: pin" in rendered || "Box::pin" in rendered, "Expected Box::pin in impl: $rendered")
        assertTrue("async move" in rendered, "Expected async move in impl: $rendered")
    }

    @Test
    fun expandsUnimplementedDefaultMethod() {
        val input =
            quote(
                """
                pub trait Trait {
                    async fn f() {
                        unimplemented!()
                    }
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertTrue("Box :: pin" in rendered || "Box::pin" in rendered, "Expected Box::pin in default body: $rendered")
    }

    @Test
    fun expandsContextReturningSelf() {
        val input =
            quote(
                """
                pub trait Context: Sized {
                    async fn from_parts() -> Self;
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertTrue("Future" in rendered, "Expected Future in signature: $rendered")
    }
}
