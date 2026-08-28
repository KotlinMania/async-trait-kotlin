# Code Port - Progress Report

**Generated:** 2026-08-28
**Source:** tmp/async-trait/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/asynctrait

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 32/52 matched (target 68) | 61.5% |
| Class/type parity | 13/13 matched (target 20) | 100.0% |
| Combined symbol parity | 45/65 matched (target 88) | 69.2% |
| Average function body similarity | 0.56 | inline-code cosine |
| Average documentation similarity | 0.05 | doc text cosine |
| Missing source functions | 0 | 0% parity until ported |
| Missing source classes/types | 0 | 0% parity until ported |
| Missing source symbol files | 0 | 0 symbols |
| Cheat/scoring failures | 0 | forced to 0% |
| Total source files | 8 | 100% |
| Target units (paired) | 12 | - |
| Target files (total) | 12 | - |
| Porting progress | 8 | 100.0% (matched) |
| Missing files | 0 | 0.0% |

## Port Quality Analysis

**Average Function Similarity:** 0.56

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 4 files (50.0% of matched)
- Needs review (0.60-0.84): 4 files (50.0% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|
| 1 | `lifetime` | `asynctrait.Lifetime` | 0.19 | 4/12 matched | `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut` | 2/2 matched | _none_ | - | 8 | 81408.1 |
| 2 | `receiver` | `asynctrait.Receiver` | 0.33 | 7/16 matched | `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut` | 3/3 matched | _none_ | - | 9 | 91906.7 |
| 3 | `expand` | `asynctrait.Expand` | 0.44 | 10/13 matched (target 16) | `to_tokens`, `visit_type_path_mut`, `visit_type_mut` | 3/3 matched (target 5) | _none_ | - | 3 | 1031605.6 |
| 4 | `parse` | `asynctrait.Parse` | 0.59 | 1/1 matched (target 3) | _none_ | 1/1 matched (target 3) | _none_ | - | 0 | 204.1 |
| 5 | `args` | `asynctrait.Args` | 0.64 | 3/3 matched (target 5) | _none_ | 1/1 matched (target 2) | _none_ | - | 0 | 1000403.6 |
| 6 | `lib` | `asynctrait.AsyncTrait` | 0.71 | 1/1 matched (target 2) | _none_ | 0/0 matched | _none_ | - | 0 | 102.9 |
| 7 | `bound` | `asynctrait.Bound` | 0.78 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | - | 0 | 602.2 |
| 8 | `verbatim` | `asynctrait.Verbatim` | 0.82 | 2/2 matched (target 3) | _none_ | 1/1 matched (target 2) | _none_ | - | 0 | 301.8 |

## Cheat Detection / Scoring Failures

_None detected._

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:

- `lifetime` -> `asynctrait.Lifetime` (0.19)
- `receiver` -> `asynctrait.Receiver` (0.33)
- `expand` -> `asynctrait.Expand` (0.44, 1 deps)
- `parse` -> `asynctrait.Parse` (0.59)

## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| _None detected_ | | | |

## High Priority Missing Files

No missing files detected.

## Documentation Gaps

There is missing documentation that is hurting overall scoring.

**Documentation coverage:** 16 / 430 lines (4%)

Documentation gaps (>20%), complete list:

- `lib` - 99% gap (430 → 5 lines)

