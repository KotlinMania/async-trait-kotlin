# Code Port - Progress Report

**Generated:** 2026-08-31
**Source:** tmp
**Target:** src/commonMain/kotlin/io/github/kotlinmania/asynctrait

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 32/243 matched (target 81) | 13.2% |
| Class/type parity | 13/109 matched (target 21) | 11.9% |
| Combined symbol parity | 45/352 matched (target 102) | 12.8% |
| Average function body similarity | 0.54 | inline-code cosine |
| Average documentation similarity | 0.00 | doc text cosine |
| Missing source functions | 191 | 0% parity until ported |
| Missing source classes/types | 96 | 0% parity until ported |
| Missing source symbol files | 19 | 287 symbols |
| Cheat/scoring failures | 1 | forced to 0% |
| Total source files | 27 | 100% |
| Target units (paired) | 12 | - |
| Target files (total) | 12 | - |
| Porting progress | 8 | 29.6% (matched) |
| Missing files | 19 | 70.4% |

## Port Quality Analysis

**Average Function Similarity:** 0.54

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 5 files (62.5% of matched)
- Needs review (0.60-0.84): 3 files (37.5% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|
| 1 | `async-trait.lib` | `asynctrait.AsyncTrait [STUB]` | 0.00 | 1/1 matched (target 15) | _none_ | 0/0 matched (target 1) | _none_ | - | 0 | 110.0 |
| 2 | `async-trait.lifetime` | `asynctrait.Lifetime` | 0.19 | 4/12 matched | `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut` | 2/2 matched | _none_ | - | 8 | 81408.1 |
| 3 | `async-trait.receiver` | `asynctrait.Receiver` | 0.33 | 7/16 matched | `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut` | 3/3 matched | _none_ | - | 9 | 91906.7 |
| 4 | `async-trait.expand` | `asynctrait.Expand` | 0.44 | 10/13 matched (target 16) | `to_tokens`, `visit_type_path_mut`, `visit_type_mut` | 3/3 matched (target 5) | _none_ | - | 3 | 1031605.6 |
| 5 | `async-trait.parse` | `asynctrait.Parse` | 0.59 | 1/1 matched (target 3) | _none_ | 1/1 matched (target 3) | _none_ | - | 0 | 204.1 |
| 6 | `async-trait.args` | `asynctrait.Args` | 0.64 | 3/3 matched (target 5) | _none_ | 1/1 matched (target 2) | _none_ | - | 0 | 1000403.6 |
| 7 | `async-trait.bound` | `asynctrait.Bound` | 0.78 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | - | 0 | 602.2 |
| 8 | `async-trait.verbatim` | `asynctrait.Verbatim` | 0.82 | 2/2 matched (target 3) | _none_ | 1/1 matched (target 2) | _none_ | - | 0 | 301.8 |

## Cheat Detection / Scoring Failures

- `async-trait.lib` -> `asynctrait.AsyncTrait [STUB]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:

- `async-trait.lib` -> `asynctrait.AsyncTrait [STUB]` (0.00)
- `async-trait.lifetime` -> `asynctrait.Lifetime` (0.19)
- `async-trait.receiver` -> `asynctrait.Receiver` (0.33)
- `async-trait.expand` -> `asynctrait.Expand` (0.44, 1 deps)
- `async-trait.parse` -> `asynctrait.Parse` (0.59)

## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| _None detected_ | | | |

## High Priority Missing Files

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `tests.test` | `asynctrait.tests.Test` | 0 | 144 | 62 | 206 | `async-trait/tests/test.rs` | `asynctrait/tests/Test.kt` |
| 2 | `ui.lifetime-span` | `asynctrait.tests.ui.Lifetime-span` | 0 | 5 | 4 | 9 | `async-trait/tests/ui/lifetime-span.rs` | `asynctrait/tests/ui/Lifetime-span.kt` |
| 3 | `ui.consider-restricting` | `asynctrait.tests.ui.Consider-restricting` | 0 | 3 | 4 | 7 | `async-trait/tests/ui/consider-restricting.rs` | `asynctrait/tests/ui/Consider-restricting.kt` |
| 4 | `ui.self-span` | `asynctrait.tests.ui.Self-span` | 0 | 3 | 3 | 6 | `async-trait/tests/ui/self-span.rs` | `asynctrait/tests/ui/Self-span.kt` |
| 5 | `executor.mod` | `asynctrait.tests.executor.Mod` | 0 | 5 | 0 | 5 | `async-trait/tests/executor/mod.rs` | `asynctrait/tests/executor/Mod.kt` |
| 6 | `ui.must-use` | `asynctrait.tests.ui.Must-use` | 0 | 3 | 2 | 5 | `async-trait/tests/ui/must-use.rs` | `asynctrait/tests/ui/Must-use.kt` |
| 7 | `ui.send-not-implemented` | `asynctrait.tests.ui.Send-not-implemented` | 0 | 4 | 1 | 5 | `async-trait/tests/ui/send-not-implemented.rs` | `asynctrait/tests/ui/Send-not-implemented.kt` |
| 8 | `ui.type-mismatch` | `asynctrait.tests.ui.Type-mismatch` | 0 | 3 | 2 | 5 | `async-trait/tests/ui/type-mismatch.rs` | `asynctrait/tests/ui/Type-mismatch.kt` |
| 9 | `ui.unreachable` | `asynctrait.tests.ui.Unreachable` | 0 | 3 | 2 | 5 | `async-trait/tests/ui/unreachable.rs` | `asynctrait/tests/ui/Unreachable.kt` |
| 10 | `ui.arg-implementation-detail` | `asynctrait.tests.ui.Arg-implementation-detail` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/arg-implementation-detail.rs` | `asynctrait/tests/ui/Arg-implementation-detail.kt` |
| 11 | `ui.delimiter-span` | `asynctrait.tests.ui.Delimiter-span` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/delimiter-span.rs` | `asynctrait/tests/ui/Delimiter-span.kt` |
| 12 | `ui.lifetime-defined-here` | `asynctrait.tests.ui.Lifetime-defined-here` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/lifetime-defined-here.rs` | `asynctrait/tests/ui/Lifetime-defined-here.kt` |
| 13 | `ui.missing-async-in-impl` | `asynctrait.tests.ui.Missing-async-in-impl` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/missing-async-in-impl.rs` | `asynctrait/tests/ui/Missing-async-in-impl.kt` |
| 14 | `ui.missing-async-in-trait` | `asynctrait.tests.ui.Missing-async-in-trait` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/missing-async-in-trait.rs` | `asynctrait/tests/ui/Missing-async-in-trait.kt` |
| 15 | `ui.no-attribute-macro` | `asynctrait.tests.ui.No-attribute-macro` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/no-attribute-macro.rs` | `asynctrait/tests/ui/No-attribute-macro.kt` |
| 16 | `ui.bare-trait-object` | `asynctrait.tests.ui.Bare-trait-object` | 0 | 2 | 1 | 3 | `async-trait/tests/ui/bare-trait-object.rs` | `asynctrait/tests/ui/Bare-trait-object.kt` |
| 17 | `ui.missing-body` | `asynctrait.tests.ui.Missing-body` | 0 | 1 | 2 | 3 | `async-trait/tests/ui/missing-body.rs` | `asynctrait/tests/ui/Missing-body.kt` |
| 18 | `ui.unsupported-self` | `asynctrait.tests.ui.Unsupported-self` | 0 | 2 | 1 | 3 | `async-trait/tests/ui/unsupported-self.rs` | `asynctrait/tests/ui/Unsupported-self.kt` |
| 19 | `tests.compiletest` | `asynctrait.tests.Compiletest` | 0 | 1 | 0 | 1 | `async-trait/tests/compiletest.rs` | `asynctrait/tests/Compiletest.kt` |

## Documentation Gaps

There is missing documentation that is hurting overall scoring.

**Documentation coverage:** 16 / 430 lines (4%)

Documentation gaps (>20%), complete list:

- `async-trait.lib` - 99% gap (430 → 5 lines)

