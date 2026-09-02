# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 8/8 (100.0%)
- **Function parity:** 31/51 matched (target 66) — 60.8%
- **Class/type parity:** 13/13 matched (target 20) — 100.0%
- **Combined symbol parity:** 44/64 matched (target 86) — 68.8%
- **Average inline-code cosine:** 0.56 (function body across 8 matched files)
- **Average documentation cosine:** 0.05 (doc text across 8 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 4 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. expand

- **Target:** `asynctrait.Expand`
- **Similarity:** 0.44
- **Dependents:** 1
- **Priority Score:** 1031605.6
- **Functions:** 10/13 matched (target 16)
- **Missing functions:** `to_tokens`, `visit_type_path_mut`, `visit_type_mut`
- **Types:** 3/3 matched (target 5)
- **Missing types:** _none_

### 2. args

- **Target:** `asynctrait.Args`
- **Similarity:** 0.64
- **Dependents:** 1
- **Priority Score:** 1000403.6
- **Functions:** 3/3 matched (target 5)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 3. receiver

- **Target:** `asynctrait.Receiver`
- **Similarity:** 0.33
- **Dependents:** 0
- **Priority Score:** 91906.7
- **Functions:** 7/16 matched
- **Missing functions:** `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut`
- **Types:** 3/3 matched
- **Missing types:** _none_

### 4. lifetime

- **Target:** `asynctrait.Lifetime`
- **Similarity:** 0.19
- **Dependents:** 0
- **Priority Score:** 81408.1
- **Functions:** 4/12 matched
- **Missing functions:** `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut`
- **Types:** 2/2 matched
- **Missing types:** _none_

### 5. bound

- **Target:** `asynctrait.Bound`
- **Similarity:** 0.78
- **Dependents:** 0
- **Priority Score:** 602.2
- **Functions:** 4/4 matched (target 11)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_

### 6. verbatim

- **Target:** `asynctrait.Verbatim`
- **Similarity:** 0.82
- **Dependents:** 0
- **Priority Score:** 301.8
- **Functions:** 2/2 matched (target 3)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 7. parse

- **Target:** `asynctrait.Parse`
- **Similarity:** 0.59
- **Dependents:** 0
- **Priority Score:** 204.1
- **Functions:** 1/1 matched (target 3)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Matched

| Source | Target | Path |
|--------|--------|------|
| `lib` | `asynctrait.AsyncTrait` | `lib` |

