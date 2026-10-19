# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 8/8 (100.0%)
- **Function parity:** 54/54 matched (target 91) — 100.0%
- **Class/type parity:** 13/13 matched (target 21) — 100.0%
- **Combined symbol parity:** 67/67 matched (target 112) — 100.0%
- **Average inline-code cosine:** 0.18 (function body across 8 matched files)
- **Average documentation cosine:** 0.03 (doc text across 8 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 8 files with <0.60 function similarity
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
- **Similarity:** 0.11
- **Dependents:** 1
- **Priority Score:** 1001608.9
- **Functions:** 13/13 matched (target 19)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 6)
- **Missing types:** _none_

### 2. args

- **Target:** `asynctrait.Args`
- **Similarity:** 0.19
- **Dependents:** 1
- **Priority Score:** 1000408.1
- **Functions:** 3/3 matched (target 5)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 3. receiver

- **Target:** `asynctrait.Receiver`
- **Similarity:** 0.22
- **Dependents:** 0
- **Priority Score:** 2107.8
- **Functions:** 18/18 matched (target 28)
- **Missing functions:** _none_
- **Types:** 3/3 matched
- **Missing types:** _none_
- **Lint issues:** 4

### 4. lifetime

- **Target:** `asynctrait.Lifetime`
- **Similarity:** 0.11
- **Dependents:** 0
- **Priority Score:** 1508.9
- **Functions:** 13/13 matched (target 21)
- **Missing functions:** _none_
- **Types:** 2/2 matched
- **Missing types:** _none_

### 5. bound

- **Target:** `asynctrait.Bound`
- **Similarity:** 0.29
- **Dependents:** 0
- **Priority Score:** 607.1
- **Functions:** 4/4 matched (target 11)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_

### 6. verbatim

- **Target:** `asynctrait.Verbatim`
- **Similarity:** 0.27
- **Dependents:** 0
- **Priority Score:** 307.3
- **Functions:** 2/2 matched (target 3)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 7. parse

- **Target:** `asynctrait.Parse`
- **Similarity:** 0.10
- **Dependents:** 0
- **Priority Score:** 209.0
- **Functions:** 1/1 matched (target 4)
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

