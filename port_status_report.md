# Code Port - Progress Report

**Generated:** 2026-10-19
**Source:** tmp/async-trait/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/asynctrait

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 54/54 matched (target 91) | 100.0% |
| Class/type parity | 13/13 matched (target 21) | 100.0% |
| Combined symbol parity | 67/67 matched (target 112) | 100.0% |
| Average function body similarity | 0.18 | inline-code cosine |
| Average documentation similarity | 0.03 | doc text cosine |
| Missing source functions | 0 | 0% parity until ported |
| Missing source classes/types | 0 | 0% parity until ported |
| Missing source symbol files | 0 | 0 symbols |
| Cheat/scoring failures | 0 | forced to 0% |
| Total source files | 8 | 100% |
| Target units (paired) | 13 | - |
| Target files (total) | 13 | - |
| Porting progress | 8 | 100.0% (matched) |
| Missing files | 0 | 0.0% |

## Port Quality Analysis

**Average Function Similarity:** 0.18

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 8 files (100.0% of matched)
- Needs review (0.60-0.84): 0 files (0.0% of matched)
- Excellent (>=0.85): 0 files (0.0% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|
| 1 | `parse` | `asynctrait.Parse` | 0.10 | 1/1 matched (target 4) | _none_ | 1/1 matched (target 3) | _none_ | - | 0 | 209.0 |
| 2 | `lifetime` | `asynctrait.Lifetime` | 0.11 | 13/13 matched (target 21) | _none_ | 2/2 matched | _none_ | - | 0 | 1508.9 |
| 3 | `expand` | `asynctrait.Expand` | 0.11 | 13/13 matched (target 19) | _none_ | 3/3 matched (target 6) | _none_ | - | 0 | 1001608.9 |
| 4 | `args` | `asynctrait.Args` | 0.19 | 3/3 matched (target 5) | _none_ | 1/1 matched (target 2) | _none_ | - | 0 | 1000408.1 |
| 5 | `receiver` | `asynctrait.Receiver` | 0.22 | 18/18 matched (target 28) | _none_ | 3/3 matched | _none_ | - | 0 | 2107.8 |
| 6 | `verbatim` | `asynctrait.Verbatim` | 0.27 | 2/2 matched (target 3) | _none_ | 1/1 matched (target 2) | _none_ | - | 0 | 307.3 |
| 7 | `bound` | `asynctrait.Bound` | 0.29 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | - | 0 | 607.1 |

## Cheat Detection / Scoring Failures

_None detected._

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:

- `parse` -> `asynctrait.Parse` (0.10)
- `lifetime` -> `asynctrait.Lifetime` (0.11)
- `expand` -> `asynctrait.Expand` (0.11, 1 deps)
- `args` -> `asynctrait.Args` (0.19, 1 deps)
- `receiver` -> `asynctrait.Receiver` (0.22)
- `verbatim` -> `asynctrait.Verbatim` (0.27)
- `bound` -> `asynctrait.Bound` (0.29)

## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| _None detected_ | | | |

## High Priority Missing Files

No missing files detected.

## Documentation Gaps

Documentation correspondence is incomplete; it is reported separately from implementation scoring.

**Documentation line amount:** 46 / 430 lines (11%)

Documentation gaps (>20%), complete list:

- `lib` - 92% gap (430 → 35 lines)

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Matched

| Source | Target | Path |
|--------|--------|------|
| `lib` | `asynctrait.AsyncTrait` | `lib` |

