# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `bound` | `asynctrait.Bound` | 0.78 | 0 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | 0 | 6 | 602.2 |

## Cheat Detection / Scoring Failures

_None detected._

## Critical Issues (Function Similarity < 0.60 with Dependencies)

No critical issues with dependencies.

## Missing Files (by Dependents)

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `args` | `Args` | 1 | 3 | 1 | 4 | `args.rs` | `Args.kt` |
| 2 | `expand` | `Expand` | 1 | 13 | 3 | 16 | `expand.rs` | `Expand.kt` |
| 3 | `lifetime` | `Lifetime` | 0 | 13 | 2 | 15 | `lifetime.rs` | `Lifetime.kt` |
| 4 | `parse` | `Parse` | 0 | 1 | 1 | 2 | `parse.rs` | `Parse.kt` |
| 5 | `receiver` | `Receiver` | 0 | 18 | 3 | 21 | `receiver.rs` | `Receiver.kt` |
| 6 | `verbatim` | `Verbatim` | 0 | 2 | 1 | 3 | `verbatim.rs` | `Verbatim.kt` |

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

