# Code Port - Progress Report

**Generated:** 2026-06-21
**Source:** tmp/async-trait
**Target:** src/commonMain

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 4/240 matched (target 5) | 1.7% |
| Class/type parity | 2/109 matched (target 2) | 1.8% |
| Combined symbol parity | 6/349 matched (target 7) | 1.7% |
| Average function body similarity | 0.78 | inline-code cosine |
| Average documentation similarity | 0.00 | doc text cosine |
| Missing source functions | 236 | 0% parity until ported |
| Missing source classes/types | 107 | 0% parity until ported |
| Missing source symbol files | 24 | 343 symbols |
| Cheat/scoring failures | 0 | forced to 0% |
| Total source files | 27 | 100% |
| Target units (paired) | 1 | - |
| Target files (total) | 1 | - |
| Porting progress | 1 | 3.7% (matched) |
| Missing files | 24 | 88.9% |
| Reexport/wiring files | 2 | consult-only |

## Port Quality Analysis

**Average Function Similarity:** 0.78

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 0 files (0.0% of matched)
- Needs review (0.60-0.84): 1 files (100.0% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|
| 1 | `bound` | `asynctrait.Bound [PROVENANCE-FALLBACK]` | 0.78 | 4/4 matched (target 5) | _none_ | 2/2 matched | _none_ | - | 0 | 602.2 |

## Cheat Detection / Scoring Failures

_None detected._

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:


## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| _None detected_ | | | |

## High Priority Missing Files

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `tests.test` | `tests.Test` | 0 | 144 | 62 | 206 | `tests/test.rs` | `tests/Test.kt` |
| 2 | `receiver` | `Receiver` | 0 | 18 | 3 | 21 | `src/receiver.rs` | `Receiver.kt` |
| 3 | `expand` | `Expand` | 1 | 13 | 3 | 16 | `src/expand.rs` | `Expand.kt` |
| 4 | `lifetime` | `Lifetime` | 0 | 13 | 2 | 15 | `src/lifetime.rs` | `Lifetime.kt` |
| 5 | `ui.lifetime-span` | `tests.ui.Lifetime-span` | 0 | 5 | 4 | 9 | `tests/ui/lifetime-span.rs` | `tests/ui/Lifetime-span.kt` |
| 6 | `ui.consider-restricting` | `tests.ui.Consider-restricting` | 0 | 3 | 4 | 7 | `tests/ui/consider-restricting.rs` | `tests/ui/Consider-restricting.kt` |
| 7 | `ui.self-span` | `tests.ui.Self-span` | 0 | 3 | 3 | 6 | `tests/ui/self-span.rs` | `tests/ui/Self-span.kt` |
| 8 | `ui.must-use` | `tests.ui.Must-use` | 0 | 3 | 2 | 5 | `tests/ui/must-use.rs` | `tests/ui/Must-use.kt` |
| 9 | `ui.send-not-implemented` | `tests.ui.Send-not-implemented` | 0 | 4 | 1 | 5 | `tests/ui/send-not-implemented.rs` | `tests/ui/Send-not-implemented.kt` |
| 10 | `ui.type-mismatch` | `tests.ui.Type-mismatch` | 0 | 3 | 2 | 5 | `tests/ui/type-mismatch.rs` | `tests/ui/Type-mismatch.kt` |
| 11 | `ui.unreachable` | `tests.ui.Unreachable` | 0 | 3 | 2 | 5 | `tests/ui/unreachable.rs` | `tests/ui/Unreachable.kt` |
| 12 | `args` | `Args` | 1 | 3 | 1 | 4 | `src/args.rs` | `Args.kt` |
| 13 | `ui.arg-implementation-detail` | `tests.ui.Arg-implementation-detail` | 0 | 2 | 2 | 4 | `tests/ui/arg-implementation-detail.rs` | `tests/ui/Arg-implementation-detail.kt` |
| 14 | `ui.delimiter-span` | `tests.ui.Delimiter-span` | 0 | 2 | 2 | 4 | `tests/ui/delimiter-span.rs` | `tests/ui/Delimiter-span.kt` |
| 15 | `ui.lifetime-defined-here` | `tests.ui.Lifetime-defined-here` | 0 | 2 | 2 | 4 | `tests/ui/lifetime-defined-here.rs` | `tests/ui/Lifetime-defined-here.kt` |
| 16 | `ui.missing-async-in-impl` | `tests.ui.Missing-async-in-impl` | 0 | 2 | 2 | 4 | `tests/ui/missing-async-in-impl.rs` | `tests/ui/Missing-async-in-impl.kt` |
| 17 | `ui.missing-async-in-trait` | `tests.ui.Missing-async-in-trait` | 0 | 2 | 2 | 4 | `tests/ui/missing-async-in-trait.rs` | `tests/ui/Missing-async-in-trait.kt` |
| 18 | `ui.no-attribute-macro` | `tests.ui.No-attribute-macro` | 0 | 2 | 2 | 4 | `tests/ui/no-attribute-macro.rs` | `tests/ui/No-attribute-macro.kt` |
| 19 | `ui.bare-trait-object` | `tests.ui.Bare-trait-object` | 0 | 2 | 1 | 3 | `tests/ui/bare-trait-object.rs` | `tests/ui/Bare-trait-object.kt` |
| 20 | `ui.missing-body` | `tests.ui.Missing-body` | 0 | 1 | 2 | 3 | `tests/ui/missing-body.rs` | `tests/ui/Missing-body.kt` |
| 21 | `ui.unsupported-self` | `tests.ui.Unsupported-self` | 0 | 2 | 1 | 3 | `tests/ui/unsupported-self.rs` | `tests/ui/Unsupported-self.kt` |
| 22 | `verbatim` | `Verbatim` | 0 | 2 | 1 | 3 | `src/verbatim.rs` | `Verbatim.kt` |
| 23 | `parse` | `Parse` | 0 | 1 | 1 | 2 | `src/parse.rs` | `Parse.kt` |
| 24 | `tests.compiletest` | `tests.Compiletest` | 0 | 1 | 0 | 1 | `tests/compiletest.rs` | `tests/Compiletest.kt` |

## Documentation Gaps

**Documentation coverage:** 0 / 0 lines (N/A)

Documentation gaps (>20%), complete list:

No significant documentation gaps found.

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `src/lib.rs` | `Lib.kt` |
| `executor.mod` | `tests.executor.Mod` | 0 | `tests/executor/mod.rs` | `tests/executor/Mod.kt` |

