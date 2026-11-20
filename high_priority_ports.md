# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `expand` | `asynctrait.Expand` | 0.13 | 1 | 11/13 matched (target 18) | `visit_type_path_mut`, `visit_type_mut` | 3/3 matched (target 5) | _none_ | 2 | 16 | 1021608.7 |
| 2 | `args` | `asynctrait.Args` | 0.33 | 1 | 3/3 matched (target 6) | _none_ | 1/1 matched | _none_ | 0 | 4 | 1000406.7 |
| 3 | `receiver` | `asynctrait.Receiver` | 0.19 | 0 | 8/18 matched (target 17) | `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_ident_mut`, `visit_path_mut`, `visit_item_mut`, `visit_macro_mut` | 3/3 matched | _none_ | 10 | 21 | 102108.1 |
| 4 | `lifetime` | `asynctrait.Lifetime` | 0.05 | 0 | 4/13 matched | `visit_lifetime`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_reference_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut` | 2/2 matched | _none_ | 9 | 15 | 91509.5 |
| 5 | `bound` | `asynctrait.Bound` | 0.35 | 0 | 4/4 matched (target 5) | _none_ | 2/2 matched | _none_ | 0 | 6 | 606.5 |
| 6 | `verbatim` | `asynctrait.VerbatimFn` | 0.26 | 0 | 2/2 matched | _none_ | 1/1 matched | _none_ | 0 | 3 | 307.4 |
| 7 | `parse` | `asynctrait.Item` | 0.20 | 0 | 1/1 matched (target 2) | _none_ | 1/1 matched (target 3) | _none_ | 0 | 2 | 208.0 |

## Cheat Detection / Scoring Failures

_None detected._

## Critical Issues (Function Similarity < 0.60 with Dependencies)

These files need immediate attention:

- **expand** → `asynctrait.Expand`
  - Function similarity: 0.13
  - Dependencies: 1
  - Functions: 11/13 matched (target 18)
  - Missing functions: `visit_type_path_mut`, `visit_type_mut`
  - Types: 3/3 matched (target 5)
  - Missing types: _none_

- **args** → `asynctrait.Args`
  - Function similarity: 0.33
  - Dependencies: 1
  - Functions: 3/3 matched (target 6)
  - Missing functions: _none_
  - Types: 1/1 matched
  - Missing types: _none_

## Missing Files (by Dependents)

No missing files detected.

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

