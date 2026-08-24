# Code Port - Progress Report

**Generated:** 2026-08-24
**Source:** tmp/async-trait
**Target:** src

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 31/238 matched (target 65) | 13.0% |
| Class/type parity | 12/109 matched (target 17) | 11.0% |
| Combined symbol parity | 43/347 matched (target 82) | 12.4% |
| Average function body similarity | 0.39 | inline-code cosine |
| Average documentation similarity | 0.08 | doc text cosine |
| Missing source functions | 186 | 0% parity until ported |
| Missing source classes/types | 96 | 0% parity until ported |
| Missing source symbol files | 18 | 282 symbols |
| Cheat/scoring failures | 2 | forced to 0% |
| Total source files | 27 | 100% |
| Target units (paired) | 10 | - |
| Target files (total) | 10 | - |
| Porting progress | 8 | 29.6% (matched) |
| Missing files | 18 | 66.7% |
| Reexport/wiring files | 1 | consult-only |

## Port Quality Analysis

**Average Function Similarity:** 0.39

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 6 files (75.0% of matched)
- Needs review (0.60-0.84): 2 files (25.0% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|
| 1 | `args` | `asynctrait.Args [ZERO] [PROVENANCE-FALLBACK]` | 0.00 | 2/3 matched | `parse` | 1/1 matched | _none_ | - | 1 | 1010410.0 |
| 2 | `lib` | `asynctrait.AsyncTrait [ZERO] [PROVENANCE-FALLBACK]` | 0.00 | 1/1 matched (target 2) | _none_ | 0/0 matched | _none_ | - | 0 | 110.0 |
| 3 | `lifetime` | `asynctrait.Lifetime [PROVENANCE-FALLBACK]` | 0.19 | 4/12 matched | `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut` | 2/2 matched | _none_ | - | 8 | 81408.1 |
| 4 | `receiver` | `asynctrait.Receiver [PROVENANCE-FALLBACK]` | 0.33 | 7/16 matched | `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut` | 3/3 matched | _none_ | - | 9 | 91906.7 |
| 5 | `expand` | `asynctrait.Expand [PROVENANCE-FALLBACK]` | 0.45 | 10/13 matched (target 16) | `to_tokens`, `visit_type_path_mut`, `visit_type_mut` | 2/3 matched (target 4) | `AssociatedTypeImplTraits` | - | 4 | 1041605.5 |
| 6 | `parse` | `asynctrait.Parse [PROVENANCE-FALLBACK]` | 0.59 | 1/1 matched (target 3) | _none_ | 1/1 matched (target 3) | _none_ | - | 0 | 204.1 |
| 7 | `bound` | `asynctrait.Bound [PROVENANCE-FALLBACK]` | 0.78 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | - | 0 | 602.2 |
| 8 | `verbatim` | `asynctrait.Verbatim [PROVENANCE-FALLBACK]` | 0.82 | 2/2 matched | _none_ | 1/1 matched | _none_ | - | 0 | 301.8 |

## Cheat Detection / Scoring Failures

- `args` -> `asynctrait.Args [ZERO] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. Args.kt: snake_case identifier `async_trait` in Kotlin comments; Args.kt: Rust attribute syntax in Kotlin comments
- `lib` -> `asynctrait.AsyncTrait [ZERO] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. AsyncTrait.kt: snake_case identifier `async_trait` in Kotlin comments; AsyncTrait.kt: Rust lifetime explanation in Kotlin comments; AsyncTrait.kt: Rust-only type/unsafe terminology in Kotlin comments

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:

- `args` -> `asynctrait.Args [ZERO] [PROVENANCE-FALLBACK]` (0.00, 1 deps)
- `lib` -> `asynctrait.AsyncTrait [ZERO] [PROVENANCE-FALLBACK]` (0.00)
- `lifetime` -> `asynctrait.Lifetime [PROVENANCE-FALLBACK]` (0.19)
- `receiver` -> `asynctrait.Receiver [PROVENANCE-FALLBACK]` (0.33)
- `expand` -> `asynctrait.Expand [PROVENANCE-FALLBACK]` (0.45, 1 deps)
- `parse` -> `asynctrait.Parse [PROVENANCE-FALLBACK]` (0.59)

## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| `expand` | `asynctrait.Expand [PROVENANCE-FALLBACK]` | 1/3 | `AssociatedTypeImplTraits` |

## High Priority Missing Files

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `tests.test` | `tests.Test` | 0 | 144 | 62 | 206 | `tests/test.rs` | `tests/Test.kt` |
| 2 | `ui.lifetime-span` | `tests.ui.Lifetime-span` | 0 | 5 | 4 | 9 | `tests/ui/lifetime-span.rs` | `tests/ui/Lifetime-span.kt` |
| 3 | `ui.consider-restricting` | `tests.ui.Consider-restricting` | 0 | 3 | 4 | 7 | `tests/ui/consider-restricting.rs` | `tests/ui/Consider-restricting.kt` |
| 4 | `ui.self-span` | `tests.ui.Self-span` | 0 | 3 | 3 | 6 | `tests/ui/self-span.rs` | `tests/ui/Self-span.kt` |
| 5 | `ui.must-use` | `tests.ui.Must-use` | 0 | 3 | 2 | 5 | `tests/ui/must-use.rs` | `tests/ui/Must-use.kt` |
| 6 | `ui.send-not-implemented` | `tests.ui.Send-not-implemented` | 0 | 4 | 1 | 5 | `tests/ui/send-not-implemented.rs` | `tests/ui/Send-not-implemented.kt` |
| 7 | `ui.type-mismatch` | `tests.ui.Type-mismatch` | 0 | 3 | 2 | 5 | `tests/ui/type-mismatch.rs` | `tests/ui/Type-mismatch.kt` |
| 8 | `ui.unreachable` | `tests.ui.Unreachable` | 0 | 3 | 2 | 5 | `tests/ui/unreachable.rs` | `tests/ui/Unreachable.kt` |
| 9 | `ui.arg-implementation-detail` | `tests.ui.Arg-implementation-detail` | 0 | 2 | 2 | 4 | `tests/ui/arg-implementation-detail.rs` | `tests/ui/Arg-implementation-detail.kt` |
| 10 | `ui.delimiter-span` | `tests.ui.Delimiter-span` | 0 | 2 | 2 | 4 | `tests/ui/delimiter-span.rs` | `tests/ui/Delimiter-span.kt` |
| 11 | `ui.lifetime-defined-here` | `tests.ui.Lifetime-defined-here` | 0 | 2 | 2 | 4 | `tests/ui/lifetime-defined-here.rs` | `tests/ui/Lifetime-defined-here.kt` |
| 12 | `ui.missing-async-in-impl` | `tests.ui.Missing-async-in-impl` | 0 | 2 | 2 | 4 | `tests/ui/missing-async-in-impl.rs` | `tests/ui/Missing-async-in-impl.kt` |
| 13 | `ui.missing-async-in-trait` | `tests.ui.Missing-async-in-trait` | 0 | 2 | 2 | 4 | `tests/ui/missing-async-in-trait.rs` | `tests/ui/Missing-async-in-trait.kt` |
| 14 | `ui.no-attribute-macro` | `tests.ui.No-attribute-macro` | 0 | 2 | 2 | 4 | `tests/ui/no-attribute-macro.rs` | `tests/ui/No-attribute-macro.kt` |
| 15 | `ui.bare-trait-object` | `tests.ui.Bare-trait-object` | 0 | 2 | 1 | 3 | `tests/ui/bare-trait-object.rs` | `tests/ui/Bare-trait-object.kt` |
| 16 | `ui.missing-body` | `tests.ui.Missing-body` | 0 | 1 | 2 | 3 | `tests/ui/missing-body.rs` | `tests/ui/Missing-body.kt` |
| 17 | `ui.unsupported-self` | `tests.ui.Unsupported-self` | 0 | 2 | 1 | 3 | `tests/ui/unsupported-self.rs` | `tests/ui/Unsupported-self.kt` |
| 18 | `tests.compiletest` | `tests.Compiletest` | 0 | 1 | 0 | 1 | `tests/compiletest.rs` | `tests/Compiletest.kt` |

## Documentation Gaps

There is missing documentation that is hurting overall scoring.

**Documentation coverage:** 22 / 430 lines (5%)

Documentation gaps (>20%), complete list:

- `lib` - 99% gap (430 → 6 lines)

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `executor.mod` | `tests.executor.Mod` | 0 | `tests/executor/mod.rs` | `tests/executor/Mod.kt` |

