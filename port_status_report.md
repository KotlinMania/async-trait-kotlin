# Code Port - Progress Report

**Generated:** 2026-05-20
**Source:** tmp/async-trait/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/asynctrait

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 4/54 matched (target 11) | 7.4% |
| Class/type parity | 2/13 matched (target 3) | 15.4% |
| Combined symbol parity | 6/67 matched (target 14) | 9.0% |
| Average function body similarity | 0.78 | inline-code cosine |
| Average documentation similarity | 0.00 | doc text cosine |
| Missing source functions | 50 | 0% parity until ported |
| Missing source classes/types | 11 | 0% parity until ported |
| Missing source symbol files | 6 | 61 symbols |
| Cheat/scoring failures | 0 | forced to 0% |
| Total source files | 8 | 100% |
| Target units (paired) | 2 | - |
| Target files (total) | 2 | - |
| Porting progress | 1 | 12.5% (matched) |
| Missing files | 6 | 75.0% |
| Reexport/wiring files | 1 | consult-only |

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
| 1 | `bound` | `asynctrait.Bound` | 0.78 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | - | 0 | 602.2 |

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
| 1 | `receiver` | `Receiver` | 0 | 18 | 3 | 21 | `receiver.rs` | `Receiver.kt` |
| 2 | `expand` | `Expand` | 1 | 13 | 3 | 16 | `expand.rs` | `Expand.kt` |
| 3 | `lifetime` | `Lifetime` | 0 | 13 | 2 | 15 | `lifetime.rs` | `Lifetime.kt` |
| 4 | `args` | `Args` | 1 | 3 | 1 | 4 | `args.rs` | `Args.kt` |
| 5 | `verbatim` | `Verbatim` | 0 | 2 | 1 | 3 | `verbatim.rs` | `Verbatim.kt` |
| 6 | `parse` | `Parse` | 0 | 1 | 1 | 2 | `parse.rs` | `Parse.kt` |

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
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

