# Code Port - Progress Report

**Generated:** 2026-08-26
**Source:** tmp/async-trait/src
**Target:** src/commonMain/kotlin

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 31/52 matched (target 67) | 59.6% |
| Class/type parity | 12/13 matched (target 19) | 92.3% |
| Combined symbol parity | 43/65 matched (target 86) | 66.2% |
| Average function body similarity | 0.53 | inline-code cosine |
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

**Average Function Similarity:** 0.53

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 5 files (62.5% of matched)
- Needs review (0.60-0.84): 3 files (37.5% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|
| 1 | `lifetime` | `asynctrait.Lifetime` | 0.19 | 4/12 matched | `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut` | 2/2 matched | _none_ | - | 8 | 81408.1 |
| 2 | `receiver` | `asynctrait.Receiver` | 0.33 | 7/16 matched | `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut` | 3/3 matched | _none_ | - | 9 | 91906.7 |
| 3 | `args` | `asynctrait.Args` | 0.42 | 2/3 matched (target 4) | `parse` | 1/1 matched (target 2) | _none_ | - | 1 | 1010405.8 |
| 4 | `expand` | `asynctrait.Expand` | 0.45 | 10/13 matched (target 16) | `to_tokens`, `visit_type_path_mut`, `visit_type_mut` | 2/3 matched (target 4) | `AssociatedTypeImplTraits` | - | 4 | 1041605.5 |
| 5 | `parse` | `asynctrait.Parse` | 0.59 | 1/1 matched (target 3) | _none_ | 1/1 matched (target 3) | _none_ | - | 0 | 204.1 |
| 6 | `lib` | `asynctrait.AsyncTrait` | 0.71 | 1/1 matched (target 2) | _none_ | 0/0 matched | _none_ | - | 0 | 102.9 |
| 7 | `bound` | `asynctrait.Bound` | 0.78 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | - | 0 | 602.2 |
| 8 | `verbatim` | `asynctrait.Verbatim` | 0.82 | 2/2 matched (target 3) | _none_ | 1/1 matched (target 2) | _none_ | - | 0 | 301.8 |

## Cheat Detection / Scoring Failures

_None detected._

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:

- `lifetime` -> `asynctrait.Lifetime` (0.19)
- `receiver` -> `asynctrait.Receiver` (0.33)
- `args` -> `asynctrait.Args` (0.42, 1 deps)
- `expand` -> `asynctrait.Expand` (0.45, 1 deps)
- `parse` -> `asynctrait.Parse` (0.59)

## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| `expand` | `asynctrait.Expand` | 1/3 | `AssociatedTypeImplTraits` |

## High Priority Missing Files

No missing files detected.

## Documentation Gaps

There is missing documentation that is hurting overall scoring.

**Documentation coverage:** 16 / 430 lines (4%)

Documentation gaps (>20%), complete list:

- `lib` - 99% gap (430 → 5 lines)

