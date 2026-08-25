# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `expand` | `asynctrait.Expand` | 0.45 | 1 | 10/13 matched (target 16) | `to_tokens`, `visit_type_path_mut`, `visit_type_mut` | 2/3 matched (target 4) | `AssociatedTypeImplTraits` | 4 | 16 | 1041605.5 |
| 2 | `args` | `asynctrait.Args` | 0.42 | 1 | 2/3 matched (target 4) | `parse` | 1/1 matched (target 2) | _none_ | 1 | 4 | 1010405.8 |
| 3 | `receiver` | `asynctrait.Receiver` | 0.33 | 0 | 7/16 matched | `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut` | 3/3 matched | _none_ | 9 | 19 | 91906.7 |
| 4 | `lifetime` | `asynctrait.Lifetime` | 0.19 | 0 | 4/12 matched | `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut` | 2/2 matched | _none_ | 8 | 14 | 81408.1 |
| 5 | `bound` | `asynctrait.Bound` | 0.78 | 0 | 4/4 matched (target 11) | _none_ | 2/2 matched (target 3) | _none_ | 0 | 6 | 602.2 |
| 6 | `verbatim` | `asynctrait.Verbatim` | 0.82 | 0 | 2/2 matched (target 3) | _none_ | 1/1 matched (target 2) | _none_ | 0 | 3 | 301.8 |
| 7 | `parse` | `asynctrait.Parse` | 0.59 | 0 | 1/1 matched (target 3) | _none_ | 1/1 matched (target 3) | _none_ | 0 | 2 | 204.1 |
| 8 | `lib` | `asynctrait.AsyncTrait` | 0.71 | 0 | 1/1 matched (target 2) | _none_ | 0/0 matched | _none_ | 0 | 1 | 102.9 |

## Cheat Detection / Scoring Failures

_None detected._

## Critical Issues (Function Similarity < 0.60 with Dependencies)

These files need immediate attention:

- **expand** → `asynctrait.Expand`
  - Function similarity: 0.45
  - Dependencies: 1
  - Functions: 10/13 matched (target 16)
  - Missing functions: `to_tokens`, `visit_type_path_mut`, `visit_type_mut`
  - Types: 2/3 matched (target 4)
  - Missing types: `AssociatedTypeImplTraits`

- **args** → `asynctrait.Args`
  - Function similarity: 0.42
  - Dependencies: 1
  - Functions: 2/3 matched (target 4)
  - Missing functions: `parse`
  - Types: 1/1 matched (target 2)
  - Missing types: _none_

## Missing Files (by Dependents)

No missing files detected.

