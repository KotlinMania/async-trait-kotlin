# port-lint Proposed Changes

**Generated:** 2026-08-31
**Source:** tmp/async-trait/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/asynctrait

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/asynctrait/Expand.kt` | `// port-lint: source async-trait/src/expand.rs` | `// port-lint: source expand.rs` | `expand.rs` | `port-lint provenance header matched only after fallback normalization: 'async-trait/src/expand.rs' vs expected 'expand.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/asynctrait/Args.kt` | `// port-lint: source async-trait/src/args.rs` | `// port-lint: source args.rs` | `args.rs` | `port-lint provenance header matched only after fallback normalization: 'async-trait/src/args.rs' vs expected 'args.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/asynctrait/ArgsTest.kt` | `// port-lint: tests async-trait/src/args.rs` | `// port-lint: tests args.rs` | `args.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:async-trait/src/args.rs' vs expected 'args.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/asynctrait/Receiver.kt` | `// port-lint: source async-trait/src/receiver.rs` | `// port-lint: source receiver.rs` | `receiver.rs` | `port-lint provenance header matched only after fallback normalization: 'async-trait/src/receiver.rs' vs expected 'receiver.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/asynctrait/Lifetime.kt` | `// port-lint: source async-trait/src/lifetime.rs` | `// port-lint: source lifetime.rs` | `lifetime.rs` | `port-lint provenance header matched only after fallback normalization: 'async-trait/src/lifetime.rs' vs expected 'lifetime.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/asynctrait/Bound.kt` | `// port-lint: source async-trait/src/bound.rs` | `// port-lint: source bound.rs` | `bound.rs` | `port-lint provenance header matched only after fallback normalization: 'async-trait/src/bound.rs' vs expected 'bound.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/asynctrait/BoundTest.kt` | `// port-lint: tests async-trait/src/bound.rs` | `// port-lint: tests bound.rs` | `bound.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:async-trait/src/bound.rs' vs expected 'bound.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/asynctrait/Verbatim.kt` | `// port-lint: source async-trait/src/verbatim.rs` | `// port-lint: source verbatim.rs` | `verbatim.rs` | `port-lint provenance header matched only after fallback normalization: 'async-trait/src/verbatim.rs' vs expected 'verbatim.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/asynctrait/VerbatimTest.kt` | `// port-lint: tests async-trait/src/verbatim.rs` | `// port-lint: tests verbatim.rs` | `verbatim.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:async-trait/src/verbatim.rs' vs expected 'verbatim.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/asynctrait/Parse.kt` | `// port-lint: source async-trait/src/parse.rs` | `// port-lint: source parse.rs` | `parse.rs` | `port-lint provenance header matched only after fallback normalization: 'async-trait/src/parse.rs' vs expected 'parse.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/asynctrait/AsyncTrait.kt` | `// port-lint: source async-trait/src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'async-trait/src/lib.rs' vs expected 'lib.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/asynctrait/AsyncTraitTest.kt` | `// port-lint: tests async-trait/src/lib.rs` | `// port-lint: tests lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:async-trait/src/lib.rs' vs expected 'lib.rs'` |
