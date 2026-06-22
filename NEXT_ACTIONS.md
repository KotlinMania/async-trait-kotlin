# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 1/27 (3.7%)
- **Function parity:** 4/240 matched (target 5) — 1.7%
- **Class/type parity:** 2/109 matched (target 2) — 1.8%
- **Combined symbol parity:** 6/349 matched (target 7) — 1.7%
- **Average inline-code cosine:** 0.78 (function body across 1 matched files)
- **Average documentation cosine:** 0.00 (doc text across 1 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 0 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. bound

- **Target:** `asynctrait.Bound [PROVENANCE-FALLBACK]`
- **Similarity:** 0.78
- **Dependents:** 0
- **Priority Score:** 602.2
- **Functions:** 4/4 matched (target 5)
- **Missing functions:** _none_
- **Types:** 2/2 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `bound.rs` vs expected `bound.rs`
- **Proposed provenance header:** `// port-lint: source bound.rs` (current: `// port-lint: source bound.rs`)
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

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `src/lib.rs` | `Lib.kt` |
| `executor.mod` | `tests.executor.Mod` | 0 | `tests/executor/mod.rs` | `tests/executor/Mod.kt` |

