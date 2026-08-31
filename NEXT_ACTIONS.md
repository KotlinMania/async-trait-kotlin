# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 8/8 (100.0%)
- **Function parity:** 32/52 matched (target 81) — 61.5%
- **Class/type parity:** 13/13 matched (target 21) — 100.0%
- **Combined symbol parity:** 45/65 matched (target 102) — 69.2%
- **Average inline-code cosine:** 0.54 (function body across 7 matched files)
- **Average documentation cosine:** 0.00 (doc text across 7 matched files)
- **Cheat-zeroed Files:** 1
- **Critical Issues:** 5 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. expand

- **Target:** `asynctrait.Expand [PROVENANCE-FALLBACK]`
- **Similarity:** 0.44
- **Dependents:** 1
- **Priority Score:** 1031605.6
- **Functions:** 10/13 matched (target 16)
- **Missing functions:** `to_tokens`, `visit_type_path_mut`, `visit_type_mut`
- **Types:** 3/3 matched (target 5)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `async-trait/src/expand.rs` vs expected `expand.rs`
- **Proposed provenance header:** `// port-lint: source expand.rs` (current: `// port-lint: source async-trait/src/expand.rs`)
- **Lint issues:** 1

### 2. args

- **Target:** `asynctrait.Args [PROVENANCE-FALLBACK]`
- **Similarity:** 0.64
- **Dependents:** 1
- **Priority Score:** 1000403.6
- **Functions:** 3/3 matched (target 5)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `async-trait/src/args.rs` vs expected `args.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:async-trait/src/args.rs` vs expected `args.rs`
- **Proposed provenance header:** `// port-lint: source args.rs` (current: `// port-lint: source async-trait/src/args.rs`)
- **Proposed provenance header:** `// port-lint: tests args.rs` (current: `// port-lint: tests async-trait/src/args.rs`)
- **Lint issues:** 2

### 3. receiver

- **Target:** `asynctrait.Receiver [PROVENANCE-FALLBACK]`
- **Similarity:** 0.33
- **Dependents:** 0
- **Priority Score:** 91906.7
- **Functions:** 7/16 matched
- **Missing functions:** `visit_pat_ident_mut`, `visit_expr_path_mut`, `visit_type_path_mut`, `visit_receiver_mut`, `visit_item_mut`, `visit_macro_mut`, `visit_token_stream_impl`, `visit_ident_mut`, `visit_path_mut`
- **Types:** 3/3 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `async-trait/src/receiver.rs` vs expected `receiver.rs`
- **Proposed provenance header:** `// port-lint: source receiver.rs` (current: `// port-lint: source async-trait/src/receiver.rs`)
- **Lint issues:** 1

### 4. lifetime

- **Target:** `asynctrait.Lifetime [PROVENANCE-FALLBACK]`
- **Similarity:** 0.19
- **Dependents:** 0
- **Priority Score:** 81408.1
- **Functions:** 4/12 matched
- **Missing functions:** `new`, `visit_receiver_mut`, `visit_type_reference_mut`, `visit_generic_argument_mut`, `visit_type_impl_trait_mut`, `visit_type_ptr_mut`, `visit_type_bare_fn_mut`, `visit_expr_mut`
- **Types:** 2/2 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `async-trait/src/lifetime.rs` vs expected `lifetime.rs`
- **Proposed provenance header:** `// port-lint: source lifetime.rs` (current: `// port-lint: source async-trait/src/lifetime.rs`)
- **Lint issues:** 1

### 5. bound

- **Target:** `asynctrait.Bound [PROVENANCE-FALLBACK]`
- **Similarity:** 0.78
- **Dependents:** 0
- **Priority Score:** 602.2
- **Functions:** 4/4 matched (target 11)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `async-trait/src/bound.rs` vs expected `bound.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:async-trait/src/bound.rs` vs expected `bound.rs`
- **Proposed provenance header:** `// port-lint: source bound.rs` (current: `// port-lint: source async-trait/src/bound.rs`)
- **Proposed provenance header:** `// port-lint: tests bound.rs` (current: `// port-lint: tests async-trait/src/bound.rs`)
- **Lint issues:** 2

### 6. verbatim

- **Target:** `asynctrait.Verbatim [PROVENANCE-FALLBACK]`
- **Similarity:** 0.82
- **Dependents:** 0
- **Priority Score:** 301.8
- **Functions:** 2/2 matched (target 3)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `async-trait/src/verbatim.rs` vs expected `verbatim.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:async-trait/src/verbatim.rs` vs expected `verbatim.rs`
- **Proposed provenance header:** `// port-lint: source verbatim.rs` (current: `// port-lint: source async-trait/src/verbatim.rs`)
- **Proposed provenance header:** `// port-lint: tests verbatim.rs` (current: `// port-lint: tests async-trait/src/verbatim.rs`)
- **Lint issues:** 2

### 7. parse

- **Target:** `asynctrait.Parse [PROVENANCE-FALLBACK]`
- **Similarity:** 0.59
- **Dependents:** 0
- **Priority Score:** 204.1
- **Functions:** 1/1 matched (target 3)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `async-trait/src/parse.rs` vs expected `parse.rs`
- **Proposed provenance header:** `// port-lint: source parse.rs` (current: `// port-lint: source async-trait/src/parse.rs`)
- **Lint issues:** 1

### 8. lib

- **Target:** `asynctrait.AsyncTrait [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 110.0
- **Functions:** 1/1 matched (target 15)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `async-trait/src/lib.rs` vs expected `lib.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:async-trait/src/lib.rs` vs expected `lib.rs`
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source async-trait/src/lib.rs`)
- **Proposed provenance header:** `// port-lint: tests lib.rs` (current: `// port-lint: tests async-trait/src/lib.rs`)
- **Lint issues:** 2

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

