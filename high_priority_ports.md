# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `bound` | `asynctrait.Bound [PROVENANCE-FALLBACK]` | 0.78 | 0 | 4/4 matched (target 5) | _none_ | 2/2 matched | _none_ | 0 | 6 | 602.2 |

## Cheat Detection / Scoring Failures

_None detected._

## Critical Issues (Function Similarity < 0.60 with Dependencies)

No critical issues with dependencies.

## Missing Files (by Dependents)

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `args` | `Args` | 1 | 3 | 1 | 4 | `src/args.rs` | `Args.kt` |
| 2 | `expand` | `Expand` | 1 | 13 | 3 | 16 | `src/expand.rs` | `Expand.kt` |
| 3 | `ui.lifetime-defined-here` | `tests.ui.Lifetime-defined-here` | 0 | 2 | 2 | 4 | `tests/ui/lifetime-defined-here.rs` | `tests/ui/Lifetime-defined-here.kt` |
| 4 | `parse` | `Parse` | 0 | 1 | 1 | 2 | `src/parse.rs` | `Parse.kt` |
| 5 | `receiver` | `Receiver` | 0 | 18 | 3 | 21 | `src/receiver.rs` | `Receiver.kt` |
| 6 | `verbatim` | `Verbatim` | 0 | 2 | 1 | 3 | `src/verbatim.rs` | `Verbatim.kt` |
| 7 | `tests.compiletest` | `tests.Compiletest` | 0 | 1 | 0 | 1 | `tests/compiletest.rs` | `tests/Compiletest.kt` |
| 8 | `tests.test` | `tests.Test` | 0 | 144 | 62 | 206 | `tests/test.rs` | `tests/Test.kt` |
| 9 | `ui.arg-implementation-detail` | `tests.ui.Arg-implementation-detail` | 0 | 2 | 2 | 4 | `tests/ui/arg-implementation-detail.rs` | `tests/ui/Arg-implementation-detail.kt` |
| 10 | `ui.bare-trait-object` | `tests.ui.Bare-trait-object` | 0 | 2 | 1 | 3 | `tests/ui/bare-trait-object.rs` | `tests/ui/Bare-trait-object.kt` |
| 11 | `ui.consider-restricting` | `tests.ui.Consider-restricting` | 0 | 3 | 4 | 7 | `tests/ui/consider-restricting.rs` | `tests/ui/Consider-restricting.kt` |
| 12 | `ui.delimiter-span` | `tests.ui.Delimiter-span` | 0 | 2 | 2 | 4 | `tests/ui/delimiter-span.rs` | `tests/ui/Delimiter-span.kt` |
| 13 | `lifetime` | `Lifetime` | 0 | 13 | 2 | 15 | `src/lifetime.rs` | `Lifetime.kt` |
| 14 | `ui.lifetime-span` | `tests.ui.Lifetime-span` | 0 | 5 | 4 | 9 | `tests/ui/lifetime-span.rs` | `tests/ui/Lifetime-span.kt` |
| 15 | `ui.missing-async-in-impl` | `tests.ui.Missing-async-in-impl` | 0 | 2 | 2 | 4 | `tests/ui/missing-async-in-impl.rs` | `tests/ui/Missing-async-in-impl.kt` |
| 16 | `ui.missing-async-in-trait` | `tests.ui.Missing-async-in-trait` | 0 | 2 | 2 | 4 | `tests/ui/missing-async-in-trait.rs` | `tests/ui/Missing-async-in-trait.kt` |
| 17 | `ui.missing-body` | `tests.ui.Missing-body` | 0 | 1 | 2 | 3 | `tests/ui/missing-body.rs` | `tests/ui/Missing-body.kt` |
| 18 | `ui.must-use` | `tests.ui.Must-use` | 0 | 3 | 2 | 5 | `tests/ui/must-use.rs` | `tests/ui/Must-use.kt` |
| 19 | `ui.no-attribute-macro` | `tests.ui.No-attribute-macro` | 0 | 2 | 2 | 4 | `tests/ui/no-attribute-macro.rs` | `tests/ui/No-attribute-macro.kt` |
| 20 | `ui.self-span` | `tests.ui.Self-span` | 0 | 3 | 3 | 6 | `tests/ui/self-span.rs` | `tests/ui/Self-span.kt` |
| 21 | `ui.send-not-implemented` | `tests.ui.Send-not-implemented` | 0 | 4 | 1 | 5 | `tests/ui/send-not-implemented.rs` | `tests/ui/Send-not-implemented.kt` |
| 22 | `ui.type-mismatch` | `tests.ui.Type-mismatch` | 0 | 3 | 2 | 5 | `tests/ui/type-mismatch.rs` | `tests/ui/Type-mismatch.kt` |
| 23 | `ui.unreachable` | `tests.ui.Unreachable` | 0 | 3 | 2 | 5 | `tests/ui/unreachable.rs` | `tests/ui/Unreachable.kt` |
| 24 | `ui.unsupported-self` | `tests.ui.Unsupported-self` | 0 | 2 | 1 | 3 | `tests/ui/unsupported-self.rs` | `tests/ui/Unsupported-self.kt` |

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

