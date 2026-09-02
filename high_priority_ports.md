# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `async-trait.expand` | `asynctrait.Expand [PROVENANCE-FALLBACK]` | 0.44 | 1 | 10/13 matched (target 16) | `to_tokens`, `visit_type_path_mut`, `visit_type_mut` | 3/3 matched (target 5) | _none_ | 3 | 16 | 1031605.6 |
| 2 | `async-trait.args` | `asynctrait.Args [PROVENANCE-FALLBACK]` | 0.64 | 1 | 3/3 matched (target 5) | _none_ | 1/1 matched (target 2) | _none_ | 0 | 4 | 1000403.6 |
| 3 | `async-trait.receiver` | `asynctrait.Receiver [PROVENANCE-FALLBACK]` | 0.33 | 0 | 7/16 matched | `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut` | 3/3 matched | _none_ | 9 | 19 | 91906.7 |
| 4 | `async-trait.lifetime` | `asynctrait.Lifetime [PROVENANCE-FALLBACK]` | 0.19 | 0 | 4/12 matched | `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut` | 2/2 matched | _none_ | 8 | 14 | 81408.1 |
| 5 | `async-trait.bound` | `asynctrait.Bound [PROVENANCE-FALLBACK]` | 0.78 | 0 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | 0 | 6 | 602.2 |
| 6 | `async-trait.verbatim` | `asynctrait.Verbatim [PROVENANCE-FALLBACK]` | 0.82 | 0 | 2/2 matched (target 3) | _none_ | 1/1 matched (target 2) | _none_ | 0 | 3 | 301.8 |
| 7 | `async-trait.parse` | `asynctrait.Parse [PROVENANCE-FALLBACK]` | 0.59 | 0 | 1/1 matched (target 3) | _none_ | 1/1 matched (target 3) | _none_ | 0 | 2 | 204.1 |

## Cheat Detection / Scoring Failures

_None detected._

## Critical Issues (Function Similarity < 0.60 with Dependencies)

These files need immediate attention:

- **async-trait.expand** → `asynctrait.Expand [PROVENANCE-FALLBACK]`
  - Function similarity: 0.44
  - Dependencies: 1
  - Functions: 10/13 matched (target 16)
  - Missing functions: `to_tokens`, `visit_type_path_mut`, `visit_type_mut`
  - Types: 3/3 matched (target 5)
  - Missing types: _none_
  - Lint issues: 1

## Missing Files (by Dependents)

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `tests.compiletest` | `asynctrait.tests.Compiletest` | 0 | 1 | 0 | 1 | `async-trait/tests/compiletest.rs` | `asynctrait/tests/Compiletest.kt` |
| 2 | `executor.mod` | `asynctrait.tests.executor.Mod` | 0 | 5 | 0 | 5 | `async-trait/tests/executor/mod.rs` | `asynctrait/tests/executor/Mod.kt` |
| 3 | `tests.test` | `asynctrait.tests.Test` | 0 | 144 | 62 | 206 | `async-trait/tests/test.rs` | `asynctrait/tests/Test.kt` |
| 4 | `ui.arg-implementation-detail` | `asynctrait.tests.ui.Arg-implementation-detail` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/arg-implementation-detail.rs` | `asynctrait/tests/ui/Arg-implementation-detail.kt` |
| 5 | `ui.bare-trait-object` | `asynctrait.tests.ui.Bare-trait-object` | 0 | 2 | 1 | 3 | `async-trait/tests/ui/bare-trait-object.rs` | `asynctrait/tests/ui/Bare-trait-object.kt` |
| 6 | `ui.consider-restricting` | `asynctrait.tests.ui.Consider-restricting` | 0 | 3 | 4 | 7 | `async-trait/tests/ui/consider-restricting.rs` | `asynctrait/tests/ui/Consider-restricting.kt` |
| 7 | `ui.delimiter-span` | `asynctrait.tests.ui.Delimiter-span` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/delimiter-span.rs` | `asynctrait/tests/ui/Delimiter-span.kt` |
| 8 | `ui.lifetime-defined-here` | `asynctrait.tests.ui.Lifetime-defined-here` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/lifetime-defined-here.rs` | `asynctrait/tests/ui/Lifetime-defined-here.kt` |
| 9 | `ui.lifetime-span` | `asynctrait.tests.ui.Lifetime-span` | 0 | 5 | 4 | 9 | `async-trait/tests/ui/lifetime-span.rs` | `asynctrait/tests/ui/Lifetime-span.kt` |
| 10 | `ui.missing-async-in-impl` | `asynctrait.tests.ui.Missing-async-in-impl` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/missing-async-in-impl.rs` | `asynctrait/tests/ui/Missing-async-in-impl.kt` |
| 11 | `ui.missing-async-in-trait` | `asynctrait.tests.ui.Missing-async-in-trait` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/missing-async-in-trait.rs` | `asynctrait/tests/ui/Missing-async-in-trait.kt` |
| 12 | `ui.missing-body` | `asynctrait.tests.ui.Missing-body` | 0 | 1 | 2 | 3 | `async-trait/tests/ui/missing-body.rs` | `asynctrait/tests/ui/Missing-body.kt` |
| 13 | `ui.must-use` | `asynctrait.tests.ui.Must-use` | 0 | 3 | 2 | 5 | `async-trait/tests/ui/must-use.rs` | `asynctrait/tests/ui/Must-use.kt` |
| 14 | `ui.no-attribute-macro` | `asynctrait.tests.ui.No-attribute-macro` | 0 | 2 | 2 | 4 | `async-trait/tests/ui/no-attribute-macro.rs` | `asynctrait/tests/ui/No-attribute-macro.kt` |
| 15 | `ui.self-span` | `asynctrait.tests.ui.Self-span` | 0 | 3 | 3 | 6 | `async-trait/tests/ui/self-span.rs` | `asynctrait/tests/ui/Self-span.kt` |
| 16 | `ui.send-not-implemented` | `asynctrait.tests.ui.Send-not-implemented` | 0 | 4 | 1 | 5 | `async-trait/tests/ui/send-not-implemented.rs` | `asynctrait/tests/ui/Send-not-implemented.kt` |
| 17 | `ui.type-mismatch` | `asynctrait.tests.ui.Type-mismatch` | 0 | 3 | 2 | 5 | `async-trait/tests/ui/type-mismatch.rs` | `asynctrait/tests/ui/Type-mismatch.kt` |
| 18 | `ui.unreachable` | `asynctrait.tests.ui.Unreachable` | 0 | 3 | 2 | 5 | `async-trait/tests/ui/unreachable.rs` | `asynctrait/tests/ui/Unreachable.kt` |
| 19 | `ui.unsupported-self` | `asynctrait.tests.ui.Unsupported-self` | 0 | 2 | 1 | 3 | `async-trait/tests/ui/unsupported-self.rs` | `asynctrait/tests/ui/Unsupported-self.kt` |

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Matched

| Source | Target | Path |
|--------|--------|------|
| `async-trait.lib` | `asynctrait.AsyncTrait` | `async-trait/src/lib` |

