# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `expand` | `asynctrait.Expand [PROVENANCE-FALLBACK]` | 0.45 | 1 | 10/13 matched (target 16) | `to_tokens`, `visit_type_path_mut`, `visit_type_mut` | 2/3 matched (target 4) | `AssociatedTypeImplTraits` | 4 | 16 | 1041605.5 |
| 2 | `args` | `asynctrait.Args [ZERO] [PROVENANCE-FALLBACK]` | 0.00 | 1 | 2/3 matched | `parse` | 1/1 matched | _none_ | 1 | 4 | 1010410.0 |
| 3 | `receiver` | `asynctrait.Receiver [PROVENANCE-FALLBACK]` | 0.33 | 0 | 7/16 matched | `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut` | 3/3 matched | _none_ | 9 | 19 | 91906.7 |
| 4 | `lifetime` | `asynctrait.Lifetime [PROVENANCE-FALLBACK]` | 0.19 | 0 | 4/12 matched | `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut` | 2/2 matched | _none_ | 8 | 14 | 81408.1 |
| 5 | `bound` | `asynctrait.Bound [PROVENANCE-FALLBACK]` | 0.78 | 0 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | 0 | 6 | 602.2 |
| 6 | `verbatim` | `asynctrait.Verbatim [PROVENANCE-FALLBACK]` | 0.82 | 0 | 2/2 matched | _none_ | 1/1 matched | _none_ | 0 | 3 | 301.8 |
| 7 | `parse` | `asynctrait.Parse [PROVENANCE-FALLBACK]` | 0.59 | 0 | 1/1 matched (target 3) | _none_ | 1/1 matched (target 3) | _none_ | 0 | 2 | 204.1 |
| 8 | `lib` | `asynctrait.AsyncTrait [ZERO] [PROVENANCE-FALLBACK]` | 0.00 | 0 | 1/1 matched (target 2) | _none_ | 0/0 matched | _none_ | 0 | 1 | 110.0 |

## Cheat Detection / Scoring Failures

- `args` -> `asynctrait.Args [ZERO] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. Args.kt: snake_case identifier `async_trait` in Kotlin comments; Args.kt: Rust attribute syntax in Kotlin comments
- `lib` -> `asynctrait.AsyncTrait [ZERO] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. AsyncTrait.kt: snake_case identifier `async_trait` in Kotlin comments; AsyncTrait.kt: Rust lifetime explanation in Kotlin comments; AsyncTrait.kt: Rust-only type/unsafe terminology in Kotlin comments

## Critical Issues (Function Similarity < 0.60 with Dependencies)

These files need immediate attention:

- **expand** → `asynctrait.Expand [PROVENANCE-FALLBACK]`
  - Function similarity: 0.45
  - Dependencies: 1
  - Functions: 10/13 matched (target 16)
  - Missing functions: `to_tokens`, `visit_type_path_mut`, `visit_type_mut`
  - Types: 2/3 matched (target 4)
  - Missing types: `AssociatedTypeImplTraits`
  - Lint issues: 1

- **args** → `asynctrait.Args [ZERO] [PROVENANCE-FALLBACK]`
  - Function similarity: 0.00
  - Dependencies: 1
  - Functions: 2/3 matched
  - Missing functions: `parse`
  - Types: 1/1 matched
  - Missing types: _none_
  - Scoring failure: Args.kt: snake_case identifier `async_trait` in Kotlin comments; Args.kt: Rust attribute syntax in Kotlin comments
  - Lint issues: 1

## Missing Files (by Dependents)

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `tests.compiletest` | `tests.Compiletest` | 0 | 1 | 0 | 1 | `tests/compiletest.rs` | `tests/Compiletest.kt` |
| 2 | `tests.test` | `tests.Test` | 0 | 144 | 62 | 206 | `tests/test.rs` | `tests/Test.kt` |
| 3 | `ui.arg-implementation-detail` | `tests.ui.Arg-implementation-detail` | 0 | 2 | 2 | 4 | `tests/ui/arg-implementation-detail.rs` | `tests/ui/Arg-implementation-detail.kt` |
| 4 | `ui.bare-trait-object` | `tests.ui.Bare-trait-object` | 0 | 2 | 1 | 3 | `tests/ui/bare-trait-object.rs` | `tests/ui/Bare-trait-object.kt` |
| 5 | `ui.consider-restricting` | `tests.ui.Consider-restricting` | 0 | 3 | 4 | 7 | `tests/ui/consider-restricting.rs` | `tests/ui/Consider-restricting.kt` |
| 6 | `ui.delimiter-span` | `tests.ui.Delimiter-span` | 0 | 2 | 2 | 4 | `tests/ui/delimiter-span.rs` | `tests/ui/Delimiter-span.kt` |
| 7 | `ui.lifetime-defined-here` | `tests.ui.Lifetime-defined-here` | 0 | 2 | 2 | 4 | `tests/ui/lifetime-defined-here.rs` | `tests/ui/Lifetime-defined-here.kt` |
| 8 | `ui.lifetime-span` | `tests.ui.Lifetime-span` | 0 | 5 | 4 | 9 | `tests/ui/lifetime-span.rs` | `tests/ui/Lifetime-span.kt` |
| 9 | `ui.missing-async-in-impl` | `tests.ui.Missing-async-in-impl` | 0 | 2 | 2 | 4 | `tests/ui/missing-async-in-impl.rs` | `tests/ui/Missing-async-in-impl.kt` |
| 10 | `ui.missing-async-in-trait` | `tests.ui.Missing-async-in-trait` | 0 | 2 | 2 | 4 | `tests/ui/missing-async-in-trait.rs` | `tests/ui/Missing-async-in-trait.kt` |
| 11 | `ui.missing-body` | `tests.ui.Missing-body` | 0 | 1 | 2 | 3 | `tests/ui/missing-body.rs` | `tests/ui/Missing-body.kt` |
| 12 | `ui.must-use` | `tests.ui.Must-use` | 0 | 3 | 2 | 5 | `tests/ui/must-use.rs` | `tests/ui/Must-use.kt` |
| 13 | `ui.no-attribute-macro` | `tests.ui.No-attribute-macro` | 0 | 2 | 2 | 4 | `tests/ui/no-attribute-macro.rs` | `tests/ui/No-attribute-macro.kt` |
| 14 | `ui.self-span` | `tests.ui.Self-span` | 0 | 3 | 3 | 6 | `tests/ui/self-span.rs` | `tests/ui/Self-span.kt` |
| 15 | `ui.send-not-implemented` | `tests.ui.Send-not-implemented` | 0 | 4 | 1 | 5 | `tests/ui/send-not-implemented.rs` | `tests/ui/Send-not-implemented.kt` |
| 16 | `ui.type-mismatch` | `tests.ui.Type-mismatch` | 0 | 3 | 2 | 5 | `tests/ui/type-mismatch.rs` | `tests/ui/Type-mismatch.kt` |
| 17 | `ui.unreachable` | `tests.ui.Unreachable` | 0 | 3 | 2 | 5 | `tests/ui/unreachable.rs` | `tests/ui/Unreachable.kt` |
| 18 | `ui.unsupported-self` | `tests.ui.Unsupported-self` | 0 | 2 | 1 | 3 | `tests/ui/unsupported-self.rs` | `tests/ui/Unsupported-self.kt` |

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `executor.mod` | `tests.executor.Mod` | 0 | `tests/executor/mod.rs` | `tests/executor/Mod.kt` |

