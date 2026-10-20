// port-lint: tests tests/test.rs
package io.github.kotlinmania.asynctrait

import io.github.kotlinmania.quote.quote
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpstreamRegressionTest {
    @Test
    fun unsafeTraitsAndImplementationsKeepTheirVisibilityAndAttributes() {
        val trait = expanded("#[allow(dead_code)] pub(crate) unsafe trait Trait { async fn f(); }")
        contains(trait, "#[allow(dead_code)] pub(crate) unsafe trait Trait")
        val impl = expanded("unsafe impl Trait for () { async fn f() {} }")
        contains(impl, "unsafe impl Trait for ()")
    }

    @Test
    fun requiredMethodsDoNotAddSyncToSelf() {
        val output = expanded("trait Trait { async fn f(&self); }")
        contains(output, "Self: 'async_trait")
        absent(output, "::core::marker::Sync")
        absent(output, "Box::pin")
        contains(output, "#[must_use]")
    }

    @Test
    fun defaultMethodsInferOnlyBoundsAbsentFromSupertraits() {
        contains(expanded("trait Trait { async fn f(&self) {} }"), "Self: ::core::marker::Sync + 'async_trait")
        contains(expanded("trait Trait { async fn f(&mut self) {} }"), "Self: ::core::marker::Send + 'async_trait")
        val output = expanded("trait Trait: ::core::marker::Sync { async fn f(&self) {} }")
        contains(output, "Self: 'async_trait")
        absent(output, "Self: ::core::marker::Sync")
    }

    @Test
    fun localMethodsDoNotRequireThreadSafety() {
        val output = expanded("trait Trait { async fn f(&self) {} }", local = true)
        contains(output, "Future<Output = ()> + 'async_trait")
        contains(output, "Self: 'async_trait")
        absent(output, "::core::marker::Send")
        absent(output, "::core::marker::Sync")
    }

    @Test
    fun receiverReplacementDoesNotEnterNestedImplementations() {
        val output = expanded(
            "trait Trait: Sized { async fn f(self) { struct Nested; impl Nested { fn f(self) { let _ = " +
            "self; } } let _ = self; } }",
        )
        contains(output, "impl Nested { fn f(self) { let _ = self; } }")
        contains(output, "let _ = __self;")
    }

    @Test
    fun returnTypesKeepFullyQualifiedFutureAndPinPaths() {
        val output = expanded(
            "trait Trait { async fn f() -> Box<dyn Iterator<Item = ()>> { Box::new(std::iter::empty()) } }",
        )
        contains(output, "::core::pin::Pin<Box<dyn ::core::future::Future<Output = Box<dyn Iterator<Item = ()> > >")
        contains(output, "let __ret: Box<dyn Iterator<Item = ()> >")
    }

    @Test
    fun mutableReceiversAreCapturedInsideTheFuture() {
        val output = expanded("impl Trait for S { async fn f(mut self) { do_something(&mut self); } }")
        contains(output, "fn f<'async_trait>(self)")
        contains(output, "let mut __self = self;")
        contains(output, "do_something(&mut __self)")
    }

    // Nightly specialization dispatch requires Rust's trait specialization rules.

    @Test
    fun onlyUsedEnclosingLifetimesReceiveFutureBounds() {
        val output = expanded("trait Trait<'a, 'b> { async fn g(x: &'b ()); }")
        contains(output, "'b: 'async_trait")
        absent(output, "'a: 'async_trait")
        val unused = expanded("impl<'a> Trait for &'a () { async fn f() {} }")
        absent(unused, "'a: 'async_trait")
    }

    @Test
    fun staticDefaultBodiesUsingSelfReceiveTheRequiredBound() {
        val output = expanded("trait Trait { async fn f() { Self::g(); } }")
        contains(output, "Self: ::core::marker::Send + 'async_trait")
    }

    @Test
    fun instrumentationAttributesRemainOnTheMethod() {
        val output = expanded(
            "impl Trait for S { #[instrument(skip(self))] async fn f(&self) { info!(value = self.0); } }",
        )
        contains(output, "#[instrument(skip(self))]")
        contains(output, "info!(value = __self.0)")
    }

    // Tracing subscriber entry/exit timing requires Rust's instrument macro and future polling.

    @Test
    fun constantGenericParametersRemainInTheSignature() {
        val output = expanded("trait Trait { async fn f<T: Send, const C: usize>(_: [T; C]) {} }")
        contains(output, "const C: usize")
        contains(output, "T: 'async_trait + Send")
        absent(output, "C: 'async_trait")
        contains(output, "let __arg0 = __arg0;")
    }

    @Test
    fun localMacrosAndSelectExpressionsUseTheCapturedReceiver() {
        val macro = expanded(
            "trait Trait { async fn f(&self) { macro_rules! local { () => {{ let _: &Self = self; }}; } " +
            "local!(); } }",
        )
        contains(macro, "let _: &Self = __self;")
        val select = expanded(
            "impl Trait for S { async fn f(&self) { futures::select! { () = async { consume(self); }.fuse() " +
            "=> {} } } }",
        )
        contains(select, "consume(__self)")
    }

    @Test
    fun macrosContainingNestedFunctionsRetainTheirOwnReceiver() {
        val output = expanded("impl Trait for S { async fn f(&self) { make! { fn local(self) { consume(self); } } } }")
        contains(output, "fn local(self) { consume(self); }")
    }

    @Test
    fun explicitSharedReceiversInferSync() {
        val output = expanded("trait Trait { async fn g(self: &Self) {} }")
        contains(output, "Self: ::core::marker::Sync + 'async_trait")
    }

    @Test
    fun synchronousItemsAndMethodAttributesArePreserved() {
        val output = expanded("trait Trait { const VALUE: u8; type Output; fn sync(&self) -> u8; }")
        contains(output, "const VALUE: u8;")
        contains(output, "type Output;")
        contains(output, "fn sync(&self) -> u8;")
        val attributedMethod = expanded("trait Trait { #[allow(non_snake_case)] async fn camelCase(); }")
        contains(attributedMethod, "#[allow(non_snake_case)]")
    }

    @Test
    fun qualifiedAssociatedTypesAndExistingWhereClausesArePreserved() {
        val output = expanded(
            "trait Trait: Future { async fn flatten(self) -> <Self::Output as Future>::Output where " +
            "Self::Output: Future + Send, Self: Sized { self.await.await } }",
        )
        contains(output, "Output = <Self::Output as Future>::Output")
        contains(output, "Self::Output: Future + Send")
        contains(output, "__self.await.await")
    }

    @Test
    fun unitReturnBodiesAreTypeCheckedInsideTheFuture() {
        contains(expanded("impl Trait for S { async fn f() {} }"), "let _: () = {};")
        contains(expanded("impl Trait for S { async fn f() -> () {} }"), "let __ret: () = {};")
    }

    @Test
    fun opaqueAssociatedReturnTypesDoNotGetAnExplicitReturnBinding() {
        val output = expanded("impl Trait for S { type Assoc = impl Sized; async fn f(&self) -> Self::Assoc {} }")
        contains(output, "Future<Output = Self::Assoc>")
        absent(output, "let __ret: Self::Assoc")
        absent(output, "None::<Self::Assoc>")
    }

    @Test
    fun moduleQualifiedSelfPathsAreNotReceiverReferences() {
        val output = expanded("trait Trait { async fn f(&self) { self::f(); } }")
        contains(output, "self::f();")
        absent(output, "__self::f")
    }

    @Test
    fun argumentImplTraitsReceiveTheFutureLifetime() {
        val output = expanded("trait Trait { async fn f(callback: impl FnMut(&str) + Send); }")
        contains(output, "impl 'async_trait + FnMut")
    }

    // Drop order and cancellation before first poll require Rust's deterministic destructors.

    @Test
    fun wildcardArgumentsAreCapturedBeforeDestructuring() {
        val output = expanded(
            "impl Trait for S { async fn f(_: Droppable, Tuple(_, value): Tuple<Droppable, i32>) {} }",
        )
        contains(output, "let __arg0 = __arg0;")
        contains(output, "let Tuple(_, value) = { let __arg1 = __arg1; __arg1 };")
    }

    @Test
    fun referenceAndPointerImplTraitsAreParenthesized() {
        val output = expanded("trait Trait { async fn f(arg: &impl Trait); async fn g(arg: *const impl Trait); }")
        contains(output, "(impl 'async_trait + Trait)")
        contains(output, "*const (impl 'async_trait + Trait)")
    }

    @Test
    fun arcDefaultReceiversRequireBothSendAndSync() {
        val output = expanded("trait Trait { async fn f(self: Arc<Self>) {} }")
        contains(output, "Self: ::core::marker::Sync + ::core::marker::Send + 'async_trait")
    }

    @Test
    fun conditionalParametersKeepTheirAttributesOnCaptures() {
        val output = expanded("impl Trait for S { async fn f(#[cfg(any())] value: u8, #[cfg(all())] _: u8) {} }")
        contains(output, "#[cfg(any())] let value = value;")
        contains(output, "#[cfg(all())] let __arg1 = __arg1;")
    }

    @Test
    fun borrowedPatternsAreNotMovedIntoTemporaryArguments() {
        val output = expanded("impl<T: Sync> Trait for T { async fn f((a, b): &(T, T)) { consume(a, b); } }")
        contains(output, "(a, b): &'life0 (T, T)")
        absent(output, "__arg0")
    }

    @Test
    fun mutableExplicitReceiversKeepTheInnerMutableBinding() {
        val output = expanded("impl Trait for () { async fn f(mut self: &Self) { g(&mut self); } }")
        contains(output, "let mut __self = self;")
        contains(output, "g(&mut __self)")
    }

    @Test
    fun nestedOpaqueReturnsUseInferenceOnlyInsideTheBody() {
        val output = expanded(
            "impl Trait for T { async fn method(&self) -> Result<impl AsRef<str> + Send + Sync, " +
            "Self::Error> { Ok(\"Hello World\") } }",
        )
        contains(output, "Future<Output = Result<impl AsRef<str> + Send + Sync, Self::Error> >")
        contains(output, "let __ret: Result<_, Self::Error>")
    }

    @Test
    fun staticImplBodyDoesNotAddSelfLifetimeBounds() {
        val output = expanded("impl<T: Bound> Trait for T { async fn a() { Self::b(); } }")
        contains(output, "Self::b();")
        absent(output, "Self: 'async_trait")
    }

    @Test
    fun conditionalGenericBoundsStayWithTheirParameters() {
        val output = expanded("impl Trait for S { async fn f<#[cfg(any())] T: Send>(#[cfg(any())] value: T) {} }")
        contains(output, "#[cfg(any())] T: Send + 'async_trait")
        absent(output, "where T:")
        contains(output, "#[cfg(any())] let value = value;")
    }

    // Rust's compile-fail diagnostics require its borrow checker, trait solver, macro hygiene and lint engine.
    // The UI fixtures cannot be replaced with Kotlin type-error simulations.

    private fun expanded(source: String, local: Boolean = false): String =
        asyncTraitOrThrow(quote(if (local) "?Send" else ""), quote(source)).toString()

    private fun contains(output: String, expected: String) {
        val tokens = quote(expected).toString()
        assertTrue(tokens in output, "Expected $tokens in $output")
    }

    private fun absent(output: String, unexpected: String) {
        val tokens = quote(unexpected).toString()
        assertFalse(tokens in output, "Unexpected $tokens in $output")
    }
}
