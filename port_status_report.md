# Code Port - Progress Report

**Generated:** 2026-11-20
**Source:** tmp/async-trait/src
**Target:** src/common/commonMain/kotlin

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 33/54 matched (target 63) | 61.1% |
| Class/type parity | 13/13 matched (target 17) | 100.0% |
| Combined symbol parity | 46/67 matched (target 80) | 68.7% |
| Average function body similarity | 0.22 | inline-code cosine |
| Average documentation similarity | 0.00 | doc text cosine |
| Missing source functions | 0 | 0% parity until ported |
| Missing source classes/types | 0 | 0% parity until ported |
| Missing source symbol files | 0 | 0 symbols |
| Cheat/scoring failures | 0 | forced to 0% |
| Total source files | 8 | 100% |
| Target units (paired) | 11 | - |
| Target files (total) | 11 | - |
| Porting progress | 7 | 87.5% (matched) |
| Missing files | 0 | 0.0% |
| Reexport/wiring files | 1 | consult-only |

## Port Quality Analysis

**Average Function Similarity:** 0.22

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 7 files (100.0% of matched)
- Needs review (0.60-0.84): 0 files (0.0% of matched)
- Excellent (>=0.85): 0 files (0.0% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|
| 1 | `lifetime` | `asynctrait.Lifetime` | 0.05 | 4/13 matched | `visit_lifetime`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_reference_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut` | 2/2 matched | _none_ | - | 9 | 91509.5 |
| 2 | `expand` | `asynctrait.Expand` | 0.13 | 11/13 matched (target 18) | `visit_type_path_mut`, `visit_type_mut` | 3/3 matched (target 5) | _none_ | - | 2 | 1021608.7 |
| 3 | `receiver` | `asynctrait.Receiver` | 0.19 | 8/18 matched (target 17) | `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_ident_mut`, `visit_path_mut`, `visit_item_mut`, `visit_macro_mut` | 3/3 matched | _none_ | - | 10 | 102108.1 |
| 4 | `parse` | `asynctrait.Item` | 0.20 | 1/1 matched (target 2) | _none_ | 1/1 matched (target 3) | _none_ | - | 0 | 208.0 |
| 5 | `verbatim` | `asynctrait.VerbatimFn` | 0.26 | 2/2 matched | _none_ | 1/1 matched | _none_ | - | 0 | 307.4 |
| 6 | `args` | `asynctrait.Args` | 0.33 | 3/3 matched (target 6) | _none_ | 1/1 matched | _none_ | - | 0 | 1000406.7 |
| 7 | `bound` | `asynctrait.Bound` | 0.35 | 4/4 matched (target 5) | _none_ | 2/2 matched | _none_ | - | 0 | 606.5 |

## Cheat Detection / Scoring Failures

_None detected._

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:

- `lifetime` -> `asynctrait.Lifetime` (0.05)
- `expand` -> `asynctrait.Expand` (0.13, 1 deps)
- `receiver` -> `asynctrait.Receiver` (0.19)
- `parse` -> `asynctrait.Item` (0.20)
- `verbatim` -> `asynctrait.VerbatimFn` (0.26)
- `args` -> `asynctrait.Args` (0.33, 1 deps)
- `bound` -> `asynctrait.Bound` (0.35)

## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| _None detected_ | | | |

## High Priority Missing Files

No missing files detected.

## Documentation Gaps

**Documentation line amount:** 12 / 0 lines (N/A)

Documentation gaps (>20%), complete list:

No significant documentation line-count gaps found; text/reference correspondence is separate.

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

