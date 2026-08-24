# port-lint Proposed Changes

**Generated:** 2026-08-24
**Source:** tmp/async-trait
**Target:** src

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `commonMain/kotlin/io/github/kotlinmania/asynctrait/Expand.kt` | `// port-lint: source expand.rs` | `// port-lint: source expand.rs` | `expand.rs` | `port-lint provenance header matched only after fallback normalization: 'expand.rs' vs expected 'expand.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/asynctrait/Args.kt` | `// port-lint: source args.rs` | `// port-lint: source args.rs` | `args.rs` | `port-lint provenance header matched only after fallback normalization: 'args.rs' vs expected 'args.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/asynctrait/Receiver.kt` | `// port-lint: source receiver.rs` | `// port-lint: source receiver.rs` | `receiver.rs` | `port-lint provenance header matched only after fallback normalization: 'receiver.rs' vs expected 'receiver.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/asynctrait/Lifetime.kt` | `// port-lint: source lifetime.rs` | `// port-lint: source lifetime.rs` | `lifetime.rs` | `port-lint provenance header matched only after fallback normalization: 'lifetime.rs' vs expected 'lifetime.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/asynctrait/Bound.kt` | `// port-lint: source bound.rs` | `// port-lint: source bound.rs` | `bound.rs` | `port-lint provenance header matched only after fallback normalization: 'bound.rs' vs expected 'bound.rs'` |
| `commonTest/kotlin/io/github/kotlinmania/asynctrait/BoundTest.kt` | `// port-lint: tests bound.rs` | `// port-lint: tests bound.rs` | `bound.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:bound.rs' vs expected 'bound.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/asynctrait/Verbatim.kt` | `// port-lint: source verbatim.rs` | `// port-lint: source verbatim.rs` | `verbatim.rs` | `port-lint provenance header matched only after fallback normalization: 'verbatim.rs' vs expected 'verbatim.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/asynctrait/Parse.kt` | `// port-lint: source parse.rs` | `// port-lint: source parse.rs` | `parse.rs` | `port-lint provenance header matched only after fallback normalization: 'parse.rs' vs expected 'parse.rs'` |
| `commonMain/kotlin/io/github/kotlinmania/asynctrait/AsyncTrait.kt` | `// port-lint: source lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'lib.rs' vs expected 'lib.rs'` |
