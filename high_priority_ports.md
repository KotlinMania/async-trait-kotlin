# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `expand` | `asynctrait.Expand [PROVENANCE-FALLBACK]` | 0.44 | 1 | 10/13 matched (target 16) | `to_tokens`, `visit_type_path_mut`, `visit_type_mut` | 3/3 matched (target 5) | _none_ | 3 | 16 | 1031605.6 |
| 2 | `args` | `asynctrait.Args [PROVENANCE-FALLBACK]` | 0.64 | 1 | 3/3 matched (target 5) | _none_ | 1/1 matched (target 2) | _none_ | 0 | 4 | 1000403.6 |
| 3 | `receiver` | `asynctrait.Receiver [PROVENANCE-FALLBACK]` | 0.33 | 0 | 7/16 matched | `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut` | 3/3 matched | _none_ | 9 | 19 | 91906.7 |
| 4 | `lifetime` | `asynctrait.Lifetime [PROVENANCE-FALLBACK]` | 0.19 | 0 | 4/12 matched | `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut` | 2/2 matched | _none_ | 8 | 14 | 81408.1 |
| 5 | `bound` | `asynctrait.Bound [PROVENANCE-FALLBACK]` | 0.78 | 0 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | 0 | 6 | 602.2 |
| 6 | `verbatim` | `asynctrait.Verbatim [PROVENANCE-FALLBACK]` | 0.82 | 0 | 2/2 matched (target 3) | _none_ | 1/1 matched (target 2) | _none_ | 0 | 3 | 301.8 |
| 7 | `parse` | `asynctrait.Parse [PROVENANCE-FALLBACK]` | 0.59 | 0 | 1/1 matched (target 3) | _none_ | 1/1 matched (target 3) | _none_ | 0 | 2 | 204.1 |
| 8 | `lib` | `asynctrait.AsyncTrait [STUB] [PROVENANCE-FALLBACK]` | 0.00 | 0 | 1/1 matched (target 15) | _none_ | 0/0 matched (target 1) | _none_ | 0 | 1 | 110.0 |

## Cheat Detection / Scoring Failures

- `lib` -> `asynctrait.AsyncTrait [STUB] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies

## Critical Issues (Function Similarity < 0.60 with Dependencies)

These files need immediate attention:

- **expand** → `asynctrait.Expand [PROVENANCE-FALLBACK]`
  - Function similarity: 0.44
  - Dependencies: 1
  - Functions: 10/13 matched (target 16)
  - Missing functions: `to_tokens`, `visit_type_path_mut`, `visit_type_mut`
  - Types: 3/3 matched (target 5)
  - Missing types: _none_
  - Lint issues: 1

## Missing Files (by Dependents)

No missing files detected.

