// port-lint: tests tests/test.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestTest {
    @Test
    fun testDynCompatibleWithoutDefault() {
        val input =
            quote(
                """
                trait DynCompatible {
                    async fn f(&self);
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertTrue("Future" in rendered, "Expected Future in signature: $rendered")
        assertTrue("Send" in rendered, "Expected Send bound: $rendered")

        val implInput =
            quote(
                """
                impl DynCompatible for Struct {
                    async fn f(&self) {}
                }
                """.trimIndent(),
            )
        val implOutput = asyncTraitOrThrow(TokenStream.new(), implInput)
        val implRendered = implOutput.toString()
        assertTrue(
            "Box :: pin" in implRendered || "Box::pin" in implRendered,
            "Expected Box::pin in impl: $implRendered",
        )
        assertTrue("__self" in implRendered, "Expected __self in impl: $implRendered")
    }

    @Test
    fun testDynCompatibleWithDefault() {
        val input =
            quote(
                """
                trait DynCompatible: Sync {
                    async fn f(&self) {}
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertTrue("Box :: pin" in rendered || "Box::pin" in rendered, "Expected Box::pin in default body: $rendered")
    }

    @Test
    fun testDynCompatibleNoSend() {
        val args = quote("?Send")
        val input =
            quote(
                """
                trait DynCompatible: Sync {
                    async fn f(&self) {}
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(args, input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertFalse(
            ":: core :: marker :: Send" in rendered || "::core::marker::Send" in rendered,
            "Should not require Send with ?Send: $rendered",
        )

        val implInput =
            quote(
                """
                impl DynCompatible for Struct {
                    async fn f(&self) {}
                }
                """.trimIndent(),
            )
        val implOutput = asyncTraitOrThrow(args, implInput)
        val implRendered = implOutput.toString()
        assertTrue(
            "Box :: pin" in implRendered || "Box::pin" in implRendered,
            "Expected Box::pin in impl: $implRendered",
        )
    }

    @Test
    fun testCanDestruct() {
        val input =
            quote(
                """
                trait CanDestruct {
                    async fn f(&self, foos: (u8, u8, u8, u8));
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")

        val implInput =
            quote(
                """
                impl CanDestruct for Struct {
                    async fn f(&self, (a, ref mut b, ref c, d): (u8, u8, u8, u8)) {
                        let _a: u8 = a;
                        let _b: &mut u8 = b;
                        let _c: &u8 = c;
                        let _d: u8 = d;
                    }
                }
                """.trimIndent(),
            )
        val implOutput = asyncTraitOrThrow(TokenStream.new(), implInput)
        val implRendered = implOutput.toString()
        assertTrue(
            "Box :: pin" in implRendered || "Box::pin" in implRendered,
            "Expected Box::pin in impl: $implRendered",
        )
        assertTrue("__arg1" in implRendered, "Expected positional arg rewrite in pattern destruct: $implRendered")
    }

    @Test
    fun testSelfInMacro() {
        val input =
            quote(
                """
                trait Trait {
                    async fn a(self);
                    async fn b(&mut self);
                    async fn c(&self);
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")

        val implInput =
            quote(
                """
                impl Trait for String {
                    async fn a(self) {
                        println!("{}", self);
                    }
                    async fn b(&mut self) {
                        println!("{}", self);
                    }
                    async fn c(&self) {
                        println!("{}", self);
                    }
                }
                """.trimIndent(),
            )
        val implOutput = asyncTraitOrThrow(TokenStream.new(), implInput)
        val implRendered = implOutput.toString()
        assertTrue("__self" in implRendered, "Expected self renamed to __self: $implRendered")
    }

    @Test
    fun testInference() {
        val input =
            quote(
                """
                pub trait Trait {
                    async fn f() -> Box<dyn Iterator<Item = ()>> {
                        Box::new(std::iter::empty())
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
    fun testInternalItems() {
        val input =
            quote(
                """
                pub trait Trait: Sized {
                    async fn f(self) {
                        struct Struct;
                        impl Struct {
                            fn f(self) {
                                let _ = self;
                            }
                        }
                    }
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun testUnimplemented() {
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
        assertTrue("Box :: pin" in rendered || "Box::pin" in rendered, "Expected Box::pin in body: $rendered")
    }

    @Test
    fun testIssue2Flatten() {
        val input =
            quote(
                """
                pub trait Issue2: Future {
                    async fn flatten(self) -> <Self::Output as Future>::Output
                    where
                        Self::Output: Future + Send,
                        Self: Sized,
                    {
                        let nested_future = self.await;
                        nested_future.await
                    }
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertTrue("where" in rendered, "Expected where clause preserved: $rendered")
    }

    @Test
    fun testIssue9SelfByValue() {
        val input =
            quote(
                """
                pub trait Issue9: Sized + Send {
                    async fn f(_x: Self) {}
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertTrue("Box :: pin" in rendered || "Box::pin" in rendered, "Expected Box::pin in default body: $rendered")
    }

    @Test
    fun testIssue15PhantomData() {
        val input =
            quote(
                """
                pub trait Issue15 {
                    async fn myfn(&self, _: PhantomData<dyn Trait + Send>) {}
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun testIssue17FieldAccess() {
        val implInput =
            quote(
                """
                impl Issue17 for Struct {
                    async fn f(&self) {
                        println!("{}", self.string);
                    }
                }
                """.trimIndent(),
            )
        val implOutput = asyncTraitOrThrow(TokenStream.new(), implInput)
        val implRendered = implOutput.toString()
        assertTrue("__self" in implRendered, "Expected self rewritten to __self: $implRendered")
        assertTrue(
            "Box :: pin" in implRendered || "Box::pin" in implRendered,
            "Expected Box::pin in impl: $implRendered",
        )
    }

    @Test
    fun testIssue23MutSelf() {
        val input =
            quote(
                """
                pub trait Issue23 {
                    async fn f(self);
                    async fn g(mut self)
                    where
                        Self: Sized,
                    {
                        do_something(&mut self);
                    }
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")

        val implInput =
            quote(
                """
                impl Issue23 for S {
                    async fn f(mut self) {
                        do_something(&mut self);
                    }
                }
                """.trimIndent(),
            )
        val implOutput = asyncTraitOrThrow(TokenStream.new(), implInput)
        val implRendered = implOutput.toString()
        assertTrue("__self" in implRendered, "Expected mut self rewritten to __self: $implRendered")
    }

    @Test
    fun testIssue31ReturnSelf() {
        val input =
            quote(
                """
                pub trait Factory: Sized {
                    async fn create() -> Self;
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun testIssue42MutSelfDefault() {
        val input =
            quote(
                """
                pub trait Service {
                    fn step(&mut self);
                    async fn run(&mut self) {
                        self.step();
                    }
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
        assertTrue("__self" in rendered, "Expected self rewritten to __self in default body: $rendered")
    }

    @Test
    fun testIssue44MultipleLifetimes() {
        val input =
            quote(
                """
                pub trait Context<'a, 'b> {
                    async fn process(&'a self, data: &'b str) -> &'b str;
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun testIssue45InstrumentedSelf() {
        val implInput =
            quote(
                """
                impl Parent for Impl {
                    #[instrument]
                    async fn foo(&mut self, v: usize) {
                        self.0 = v;
                        self.bar().await;
                    }
                }
                """.trimIndent(),
            )
        val implOutput = asyncTraitOrThrow(TokenStream.new(), implInput)
        val implRendered = implOutput.toString()
        assertTrue("__self" in implRendered, "Expected self.0 rewritten to __self.0: $implRendered")
    }

    @Test
    fun testIssue57LifetimesOnTrait() {
        val input =
            quote(
                """
                pub trait Reader<'a> {
                    async fn read(&'a self) -> &'a [u8];
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun testIssue68WhereClauseOnMethod() {
        val input =
            quote(
                """
                pub trait Container {
                    async fn get<T>(&self) -> T
                    where
                        T: Default + Send;
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("where" in rendered, "Expected where clause on method: $rendered")
    }

    @Test
    fun testIssue85MixedAsyncAndSync() {
        val input =
            quote(
                """
                pub trait Mixed {
                    fn sync_fn(&self) -> i32;
                    async fn async_fn(&self) -> i32;
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("sync_fn" in rendered, "Expected sync_fn preserved: $rendered")
        assertTrue("async_fn" in rendered, "Expected async_fn transformed: $rendered")
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun testIssue104TuplePatternArg() {
        val input =
            quote(
                """
                pub trait Processor {
                    async fn process(&self, (x, y): (i32, i32));
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("process" in rendered, "Expected process in signature: $rendered")
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun testIssue147MutArgInImpl() {
        val implInput =
            quote(
                """
                impl Worker for Struct {
                    async fn work(&self, mut count: i32) {
                        count += 1;
                    }
                }
                """.trimIndent(),
            )
        val implOutput = asyncTraitOrThrow(TokenStream.new(), implInput)
        val implRendered = implOutput.toString()
        assertTrue(
            "Box :: pin" in implRendered || "Box::pin" in implRendered,
            "Expected Box::pin in impl: $implRendered",
        )
    }

    @Test
    fun testIssue152AssociatedTypeWhereClause() {
        val input =
            quote(
                """
                pub trait Service {
                    type Response;
                    async fn call(&self) -> Self::Response
                    where
                        Self::Response: Send;
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("where" in rendered, "Expected where clause preserved: $rendered")
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun testIssue154GenericDefault() {
        val input =
            quote(
                """
                pub trait Configurable<T = String> {
                    async fn configure(&self, value: T);
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Configurable" in rendered, "Expected Configurable trait: $rendered")
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")
    }

    @Test
    fun testIssue232GenericTupleRef() {
        val input =
            quote(
                """
                pub trait Generic<T> {
                    async fn take_ref(&self, thing: &T);
                }
                """.trimIndent(),
            )
        val output = asyncTraitOrThrow(TokenStream.new(), input)
        val rendered = output.toString()
        assertTrue("Generic" in rendered, "Expected Generic trait: $rendered")
        assertTrue("Pin" in rendered, "Expected Pin in signature: $rendered")

        val implTwo =
            quote(
                """
                impl<T: Sync> Generic<(T, T)> for Two {
                    async fn take_ref(&self, (a, b): &(T, T)) {
                        let _ = a;
                        let _ = b;
                    }
                }
                """.trimIndent(),
            )
        val implTwoOutput = asyncTraitOrThrow(TokenStream.new(), implTwo)
        val implTwoRendered = implTwoOutput.toString()
        assertTrue(
            "Box :: pin" in implTwoRendered || "Box::pin" in implTwoRendered,
            "Expected Box::pin in impl: $implTwoRendered",
        )
        assertTrue("__self" in implTwoRendered, "Expected __self in impl: $implTwoRendered")
    }
}
