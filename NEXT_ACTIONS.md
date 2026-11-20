# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 7/8 (87.5%)
- **Function parity:** 33/54 matched (target 63) — 61.1%
- **Class/type parity:** 13/13 matched (target 17) — 100.0%
- **Combined symbol parity:** 46/67 matched (target 80) — 68.7%
- **Average inline-code cosine:** 0.22 (function body across 7 matched files)
- **Average documentation cosine:** 0.00 (doc text across 7 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 7 files with <0.60 function similarity
- **Needs Review:** 0 files with 0.60-0.84 function similarity
- **Excellent:** 0 files with >=0.85 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. expand

- **Target:** `asynctrait.Expand`
- **Similarity:** 0.13
- **Dependents:** 1
- **Priority Score:** 1021608.7
- **Functions:** 11/13 matched (target 18)
- **Missing functions:** `visit_type_path_mut`, `visit_type_mut`
- **Types:** 3/3 matched (target 5)
- **Missing types:** _none_

### 2. args

- **Target:** `asynctrait.Args`
- **Similarity:** 0.33
- **Dependents:** 1
- **Priority Score:** 1000406.7
- **Functions:** 3/3 matched (target 6)
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 3. receiver

- **Target:** `asynctrait.Receiver`
- **Similarity:** 0.19
- **Dependents:** 0
- **Priority Score:** 102108.1
- **Functions:** 8/18 matched (target 17)
- **Missing functions:** `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_ident_mut`, `visit_path_mut`, `visit_item_mut`, `visit_macro_mut`
- **Types:** 3/3 matched
- **Missing types:** _none_

### 4. lifetime

- **Target:** `asynctrait.Lifetime`
- **Similarity:** 0.05
- **Dependents:** 0
- **Priority Score:** 91509.5
- **Functions:** 4/13 matched
- **Missing functions:** `visit_lifetime`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_reference_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut`
- **Types:** 2/2 matched
- **Missing types:** _none_

### 5. bound

- **Target:** `asynctrait.Bound`
- **Similarity:** 0.35
- **Dependents:** 0
- **Priority Score:** 606.5
- **Functions:** 4/4 matched (target 5)
- **Missing functions:** _none_
- **Types:** 2/2 matched
- **Missing types:** _none_

### 6. verbatim

- **Target:** `asynctrait.VerbatimFn`
- **Similarity:** 0.26
- **Dependents:** 0
- **Priority Score:** 307.4
- **Functions:** 2/2 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 7. parse

- **Target:** `asynctrait.Item`
- **Similarity:** 0.20
- **Dependents:** 0
- **Priority Score:** 208.0
- **Functions:** 1/1 matched (target 2)
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

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

