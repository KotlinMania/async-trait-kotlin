# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `expand` | `asynctrait.Expand` | 0.11 | 1 | 13/13 matched (target 19) | _none_ | 3/3 matched (target 6) | _none_ | 0 | 16 | 1001608.9 |
| 2 | `args` | `asynctrait.Args` | 0.19 | 1 | 3/3 matched (target 5) | _none_ | 1/1 matched (target 2) | _none_ | 0 | 4 | 1000408.1 |
| 3 | `receiver` | `asynctrait.Receiver` | 0.22 | 0 | 18/18 matched (target 28) | _none_ | 3/3 matched | _none_ | 0 | 21 | 2107.8 |
| 4 | `lifetime` | `asynctrait.Lifetime` | 0.11 | 0 | 13/13 matched (target 21) | _none_ | 2/2 matched | _none_ | 0 | 15 | 1508.9 |
| 5 | `bound` | `asynctrait.Bound` | 0.29 | 0 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | 0 | 6 | 607.1 |
| 6 | `verbatim` | `asynctrait.Verbatim` | 0.27 | 0 | 2/2 matched (target 3) | _none_ | 1/1 matched (target 2) | _none_ | 0 | 3 | 307.3 |
| 7 | `parse` | `asynctrait.Parse` | 0.10 | 0 | 1/1 matched (target 4) | _none_ | 1/1 matched (target 3) | _none_ | 0 | 2 | 209.0 |

## Cheat Detection / Scoring Failures

_None detected._

## Critical Issues (Function Similarity < 0.60 with Dependencies)

These files need immediate attention:

- **expand** → `asynctrait.Expand`
  - Function similarity: 0.11
  - Dependencies: 1
  - Functions: 13/13 matched (target 19)
  - Missing functions: _none_
  - Types: 3/3 matched (target 6)
  - Missing types: _none_

- **args** → `asynctrait.Args`
  - Function similarity: 0.19
  - Dependencies: 1
  - Functions: 3/3 matched (target 5)
  - Missing functions: _none_
  - Types: 1/1 matched (target 2)
  - Missing types: _none_

## Missing Files (by Dependents)

No missing files detected.

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Matched

| Source | Target | Path |
|--------|--------|------|
| `lib` | `asynctrait.AsyncTrait` | `lib` |

