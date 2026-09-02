# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 8/27 (29.6%)
- **Function parity:** 31/242 matched (target 66) — 12.8%
- **Class/type parity:** 13/109 matched (target 20) — 11.9%
- **Combined symbol parity:** 44/351 matched (target 86) — 12.5%
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

### 1. async-trait.expand

- **Target:** `asynctrait.Expand [PROVENANCE-FALLBACK]`
- **Similarity:** 0.44
- **Dependents:** 1
- **Priority Score:** 1031605.6
- **Functions:** 10/13 matched (target 16)
- **Missing functions:** `to_tokens`, `visit_type_path_mut`, `visit_type_mut`
- **Types:** 3/3 matched (target 5)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `expand.rs` vs expected `expand.rs`
- **Proposed provenance header:** `// port-lint: source expand.rs` (current: `// port-lint: source expand.rs`)
- **Lint issues:** 1

### 2. async-trait.args

- **Target:** `asynctrait.Args [PROVENANCE-FALLBACK]`
- **Similarity:** 0.64
- **Dependents:** 1
- **Priority Score:** 1000403.6
- **Functions:** 3/3 matched (target 5)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `args.rs` vs expected `args.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:args.rs` vs expected `args.rs`
- **Proposed provenance header:** `// port-lint: source args.rs` (current: `// port-lint: source args.rs`)
- **Proposed provenance header:** `// port-lint: tests args.rs` (current: `// port-lint: tests args.rs`)
- **Lint issues:** 2

### 3. async-trait.receiver

- **Target:** `asynctrait.Receiver [PROVENANCE-FALLBACK]`
- **Similarity:** 0.33
- **Dependents:** 0
- **Priority Score:** 91906.7
- **Functions:** 7/16 matched
- **Missing functions:** `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut`
- **Types:** 3/3 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `receiver.rs` vs expected `receiver.rs`
- **Proposed provenance header:** `// port-lint: source receiver.rs` (current: `// port-lint: source receiver.rs`)
- **Lint issues:** 1

### 4. async-trait.lifetime

- **Target:** `asynctrait.Lifetime [PROVENANCE-FALLBACK]`
- **Similarity:** 0.19
- **Dependents:** 0
- **Priority Score:** 81408.1
- **Functions:** 4/12 matched
- **Missing functions:** `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut`
- **Types:** 2/2 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `lifetime.rs` vs expected `lifetime.rs`
- **Proposed provenance header:** `// port-lint: source lifetime.rs` (current: `// port-lint: source lifetime.rs`)
- **Lint issues:** 1

### 5. async-trait.bound

- **Target:** `asynctrait.Bound [PROVENANCE-FALLBACK]`
- **Similarity:** 0.78
- **Dependents:** 0
- **Priority Score:** 602.2
- **Functions:** 4/4 matched (target 11)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `bound.rs` vs expected `bound.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:bound.rs` vs expected `bound.rs`
- **Proposed provenance header:** `// port-lint: source bound.rs` (current: `// port-lint: source bound.rs`)
- **Proposed provenance header:** `// port-lint: tests bound.rs` (current: `// port-lint: tests bound.rs`)
- **Lint issues:** 2

### 6. async-trait.verbatim

- **Target:** `asynctrait.Verbatim [PROVENANCE-FALLBACK]`
- **Similarity:** 0.82
- **Dependents:** 0
- **Priority Score:** 301.8
- **Functions:** 2/2 matched (target 3)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `verbatim.rs` vs expected `verbatim.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:verbatim.rs` vs expected `verbatim.rs`
- **Proposed provenance header:** `// port-lint: source verbatim.rs` (current: `// port-lint: source verbatim.rs`)
- **Proposed provenance header:** `// port-lint: tests verbatim.rs` (current: `// port-lint: tests verbatim.rs`)
- **Lint issues:** 2

### 7. async-trait.parse

- **Target:** `asynctrait.Parse [PROVENANCE-FALLBACK]`
- **Similarity:** 0.59
- **Dependents:** 0
- **Priority Score:** 204.1
- **Functions:** 1/1 matched (target 3)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `parse.rs` vs expected `parse.rs`
- **Proposed provenance header:** `// port-lint: source parse.rs` (current: `// port-lint: source parse.rs`)
- **Lint issues:** 1

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
| `async-trait.lib` | `asynctrait.AsyncTrait` | `async-trait/src/lib` |

