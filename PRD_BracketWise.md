# BracketWise: Product Requirements Document

**Version:** 2.2 | **Status: FINAL. Approved for build.** | **Owner:** Jill
**Product name:** BracketWise (formerly the working title "RoomWise"; see section 33)
**Decisions:** every decision is resolved in section 32. Changes to a resolved decision require a new ADR and a version bump.

> **Persistent in-app disclaimer (verbatim, shown on every screen):**
> "Educational planning estimate only. Not tax, legal, or investment advice. Verify contribution room through CRA records and consult a qualified professional before making financial decisions."

---

## Revisions

### Version 2.2 (this version)
- **First-home target removed.** The target amount, target year, and target comparison are gone. FHSA room, eligibility, and contribution logic are unchanged. A single household checkbox, **"I plan to use my FHSA for a qualifying first home purchase"** (`fhsaFirstHomePlan`), replaces the goal: it only switches the modeled FHSA assumptions (qualifying withdrawals tax-free, and allocation rule R1). No amount, no date.
- **Rounding-remainder edge case fixed.** Percent-to-dollar conversion now uses a **capacity-aware largest-remainder method**: the budget is preserved to the cent, and a positive cent adjustment is never placed in a destination without remaining room (section 16.7). A new validation code and test cases cover tight-room and fractional-percentage cases.
- **Explanation fields made accurate for partial increments.** The `estimatedTaxSavingsPer100` and single `savingsRate` fields are removed. Each merged step now returns the **actual contributed amount, actual estimated tax savings, an effective rate (clearly labelled), and the min/max increment rate** (section 16.6).

### Version 2.1
Save and share moved to later scope (stateless default); allocator simplified to a rule-based waterfall with side-by-side metrics and no combined score; all open decisions resolved; product name set to BracketWise.

### Version 2.0 (corrections from Version 1)
- **Corrected:** exact contribution room is no longer computed (user-entered from CRA records); part-year tax logic removed (tax-aware mode requires full-year residency); "mirrors Schedule 1" claims replaced by "simplified planning estimate"; marginal rate uses the exact deduction increment; "refund" language and reinvestment removed from the base projection; "no PII" corrected; examples recomputed.
- **Reduced:** projection to 5 annual steps; exactly two compared scenarios; rate limiting, keep-warm pings, native-image work removed.
- **Retained:** per-person tax engine, couple mode, explainable allocation, Spring Boot + React/TypeScript, golden tests, Playwright/Newman/CI, live deployment, Docker.
- **Deferred:** accounts, other provinces/years, children and benefits, CPP/EI, part-year residency, exact room reconstruction, Home Buyers' Plan, spousal RRSP.

---

## 1. Product summary

BracketWise is an educational Ontario household savings scenario planner. For a single person or a couple, it compares how a user-defined annual savings amount could be allocated among **TFSA, RRSP, and FHSA** accounts under **simplified 2026 income-tax assumptions**. Each partner's tax is computed separately and aggregated afterwards. The app shows a user-defined allocation next to a **rule-based tax-aware modeled allocation**, then compares the two on separate metrics: contributions by account, estimated current-year tax savings, five-year projected balances, estimated after-tax account value, and unused room.

## 2. Problem statement

Newcomers, early-career workers, and couples repeatedly ask: "If we can save $X a month, which accounts should it go to, and who should contribute?" Existing calculators are generic, single-person, or opaque. Users want to see **how a choice changes outcomes under assumptions they can edit**, and why a suggested allocation was produced, without sharing credentials or filing a return.

## 3. Positioning and disclaimer

**Positioning statement:**
"An educational Ontario household savings scenario planner that compares how individuals and couples could allocate a user-defined savings amount among TFSA, RRSP, and FHSA accounts under simplified 2026 income-tax assumptions."

**BracketWise is not:** tax-return software, a replacement for CRA calculations, a financial adviser, an investment recommendation engine, an exact contribution-room calculator, a take-home-pay calculator, or a guarantee of refunds, returns, or tax outcomes.

**Language rules (enforced in copy review and an automated test):**
- Output is called **"Simplified estimated income tax."** Never "take-home pay," "exact tax payable," or "tax refund."
- Deduction benefits are called **estimated tax savings**, **estimated tax reduction**, or **tax savings if saved**. Never "refund."
- The modeled scenario is labelled **"Tax-aware modeled allocation under these assumptions."** Never "best," "optimal," or "winner."
- Results never present a combined score or ranking of the two scenarios.
- Projections are labelled **illustrations, not predictions.**
- 2026 is a **planning year.** The app does not claim to reproduce final 2026 personal tax-return forms.

## 4. Users and personas

| Persona | Need | Success looks like |
|---|---|---|
| **P1: Single early-career Ontario resident** | Understand how a monthly savings amount might split across TFSA, RRSP, FHSA | Sees allocations, estimated tax savings, five-year illustration |
| **P2: Ontario couple** | Coordinate who contributes where given different incomes | Sees per-partner tax, a household allocation, and why one partner uses RRSP room |
| **P3: Newcomer, Canadian tax resident for the entire modeled tax year** | Understand account roles and plan within entered room | Gets the full experience after confirming full-year residency; otherwise education plus a general projection |
| **P4: Recruiter or interviewer** | Understand and use the demo in under 60 seconds | "Try an example" yields a complete comparison in two clicks |

## 5. Product principles

1. **Compare under assumptions; do not pronounce.** Two scenarios, separate metrics, visible assumptions.
2. **Explain every step** from the exact values the engine used, without display shortcuts that imply uniformity.
3. **User-entered facts, engine-computed estimates.** Contribution room is input; tax and projections are estimates.
4. **Separate facts, assumptions, estimates, and future work** (table below), in UI and docs.
5. **Source-backed.** No tax parameter without an official source URL and retrieval date.
6. **Bounded depth.** Substantial domain logic, small surface area, stateless by default.
7. **Finish a coherent product** before adding breadth.

| Class | Meaning | Examples |
|---|---|---|
| **Fact** | Sourced rule or parameter | Bracket thresholds from official pages |
| **Assumption** | Chosen by user or stated by the app | Return rate, RRSP withdrawal-tax rate, FHSA first-home planning checkbox, repeated annual contributions, 2026 parameters held constant |
| **Estimate** | Engine output under facts and assumptions | Simplified estimated income tax, estimated tax savings, projected balances |
| **Future work** | Explicitly out of scope | Part-year tax, room reconstruction, accounts, save and share, first-home target comparison |

---

## 6. Core release

Requirement IDs (FR-n) are used in sections 11, 23, and 26.

1. Guest access with a "Try an example" household.
2. Individual and couple modes.
3. Ontario only.
4. 2026 planning estimates only.
5. Separate financial calculations for each partner.
6. Household-level aggregation only after individual calculations.
7. User-entered employment income for the modeled tax year.
8. User-entered current available TFSA room.
9. User-entered current available RRSP deduction limit.
10. User-entered current available FHSA participation room.
11. Monthly planned savings.
12. User-defined allocation scenario.
13. Explainable tax-aware allocation scenario (rule-based).
14. Side-by-side comparison of exactly two scenarios.
15. Five-year projection using annual steps.
16. Editable investment-return and RRSP withdrawal-tax assumptions.
17. Estimated federal and Ontario income tax per person.
18. Estimated tax savings from RRSP and FHSA deductions.
19. Account-room validation.
20. Explanation of every modeled allocation.
21. React customer workflow with responsive results and accessible tables.
22. Documented REST API.
23. Docker-based local execution.
24. Live frontend and backend deployment.

## 7. Portfolio-complete release

Technical evidence added to the core without new product features:
- **PC-1** JUnit suite, Jest and React Testing Library suite, one Playwright flow, and a Postman/Newman collection, all run in GitHub Actions.
- **PC-2** README with architecture diagram, measured cold-start time, demo GIF, and limitations.
- **PC-3** Architecture decision records and a GitHub issue, milestone, and release history.
- **PC-4** Security headers, CORS allowlist, dependency updates.
- **PC-5** OpenAPI documentation committed and checked in CI.

## 8. Future enhancements (must not block completion)

- **Save and share:** local browser save; URL-fragment share links; PostgreSQL anonymous share links with expiry (Phase 8, post-release).
- **First-home target:** target amount and date with surplus/shortfall comparison (removed in v2.2).
- Accounts and authentication; other provinces or years; part-year residency proration.
- Children and benefits; CPP/CPP2/EI; inflation-indexed parameters.
- Monthly or longer projections; goals of any kind; goal-by-date solving.
- Spousal RRSP and income splitting; PDF/CSV export; French localization.

## 9. Explicit non-goals (first portfolio-complete release)

Authentication and user accounts; any saving or sharing of scenarios (server or browser); **first-home purchase targets, amounts, or dates (only the FHSA planning checkbox exists)**; additional provinces; additional tax years; children and benefit calculations; CPP, CPP2, EI; tax filing; CRA integration; bank integration; exact contribution-room reconstruction; self-employment; capital gains and dividends; foreign income; Home Buyers' Plan; spousal RRSP; debt optimization; mortgage qualification; security recommendations; investment selection; AI financial advice; Monte Carlo simulation; mobile applications; microservices; Kafka; Kubernetes.

---

## 10. Customer journey

1. **Landing:** one-paragraph explanation, disclaimer, "Try an example" and "Start my own."
2. **Household type:** single or couple.
3. **Person details:** label (optional), income for 2026, newcomer flag, optional timeline dates, **full-year tax residency confirmation**.
4. **Available room:** TFSA room, RRSP deduction limit, FHSA eligibility and room, with guidance to obtain values from CRA records.
5. **Savings and FHSA assumption:** monthly savings; checkbox "I plan to use my FHSA for a qualifying first home purchase" (enabled only when at least one person is FHSA-eligible).
6. **Assumptions:** expected annual return and RRSP withdrawal-tax rate (defaults visible and editable).
7. **User-defined allocation:** percentage per eligible account per person, with live total and live room check.
8. **Results:** side-by-side comparison, tax estimate, room summary, estimated tax savings, five-year projection, explanation, assumptions and limits.

Form state lives in memory for the session; reloading resets it ("Try an example" restores a full household in one click). Nothing is stored.

---

## 11. Functional requirements

| ID | Requirement | Acceptance criteria |
|---|---|---|
| **FR-1** | Guest access, example household | No login; "Try an example" fills all steps and reaches results in at most two clicks |
| **FR-2** | Individual and couple modes | Single shows one person form, couple two; wrong person count rejected by API |
| **FR-3** | Ontario only | `province` other than `"ON"` rejected (`UNSUPPORTED_PROVINCE`) |
| **FR-4** | 2026 planning only | `taxYear` other than `2026` rejected (`UNSUPPORTED_TAX_YEAR`); UI says "2026 planning estimate" |
| **FR-5** | Separate per-partner calculations | Each partner's tax uses only their own income and deductions (swap test) |
| **FR-6** | Household aggregation after individual calculation | Household totals equal the sum of person totals (property test) |
| **FR-7** | Income input | `estimatedEmploymentIncomeFor2026` required, 0 to 1,000,000, at most 2 decimals |
| **FR-8** | TFSA room input | `availableTfsaRoom` required (0 allowed) |
| **FR-9** | RRSP deduction limit input | `availableRrspDeductionLimit` required (0 allowed) |
| **FR-10** | FHSA room input | `fhsaEligible` and `availableFhsaParticipationRoom` required; room must be 0 when not eligible |
| **FR-11** | Monthly savings | 0 to 50,000; annual budget = monthly x 12 |
| **FR-12** | User-defined allocation | Percentages (2 decimals) totalling exactly 100.00; converted to cents by the **capacity-aware largest-remainder method** (16.7); total equals the budget to the cent; no destination exceeds room; rejected otherwise |
| **FR-13** | Tax-aware modeled allocation | Deterministic rule-based allocation per section 16; each step names the rule applied |
| **FR-14** | Two-scenario comparison | Side-by-side metrics table and charts with accessible equivalents; **no combined score** |
| **FR-15** | Five-year annual projection | Section 17; year-end balances per person and account |
| **FR-16** | Editable assumptions | Return and RRSP withdrawal-tax rate editable with validated ranges; shown in results |
| **FR-17** | Per-person tax estimate | Federal, Ontario (after reduction), surtax, OHP, total, average rate; labelled "Simplified estimated income tax" |
| **FR-18** | Estimated tax savings | Computed from exact deduction totals; never exceeds tax before deductions |
| **FR-19** | Account-room validation | Allocator never exceeds entered room; user allocation above room is a field-level error |
| **FR-20** | Explanations | Each merged step returns the **actual amount, actual estimated tax savings, effective savings rate, and min/max increment rate**; no per-$100 field; step totals reconcile to scenario totals (16.6) |
| **FR-21** | React workflow | Wizard, results tabs, responsive layout, keyboard operable, charts with accessible tables |
| **FR-22** | Documented REST API | OpenAPI served at `/swagger-ui` and committed to `/docs` |
| **FR-23** | Docker local run | `docker compose up` starts frontend and backend; documented in the README |
| **FR-24** | Live deployment | Public frontend and backend URLs; E2E passes against the live site |
| **FR-25** | Part-year restriction | If any person's `canadianTaxResidentForFullYear` is false: tax, tax savings, and modeled allocation are not computed; education, user-defined projection, and a warning are returned |
| **FR-26** | FHSA first-home planning assumption | One household checkbox (`fhsaFirstHomePlan`); when true, FHSA qualifying withdrawals are assumed tax-free and rule R1 applies; when false, FHSA is treated like RRSP; requires at least one FHSA-eligible person; no amount, no date |
| **FR-27** | Stateless operation | No server persistence; no use of `localStorage`, `sessionStorage`, IndexedDB, or cookies by the app |

---

## 12. Input specification

Monetary inputs are JSON **numbers** with at most two decimals. Monetary outputs are JSON **strings** (exact decimals).

### 12.1 Household

| Field | Type | Rule | Default |
|---|---|---|---|
| `mode` | `"SINGLE"` or `"COUPLE"` | required | none |
| `province` | string | must be `"ON"` | `"ON"` |
| `taxYear` | int | must be `2026` | `2026` |
| `monthlySavings` | number | 0 to 50,000 | none |
| `fhsaFirstHomePlan` | boolean | applies to every FHSA-eligible person; true requires at least one FHSA-eligible person (`FIRST_HOME_PLAN_WITHOUT_ELIGIBLE_FHSA`) | `false` |
| `assumptions.expectedAnnualReturn` | number (fraction) | 0.00 to 0.12 | 0.04 (assumption, not a forecast) |
| `assumptions.rrspWithdrawalTaxRate` | number (fraction) | 0.00 to 0.50 | 0.20 (assumption) |

**`fhsaFirstHomePlan` semantics:** it records the user's stated planning assumption only. It does **not** verify first-time-buyer status or qualifying-withdrawal rules, and it carries no amount or date. UI help text: "This only changes the modeled assumptions: FHSA withdrawals are treated as tax-free and FHSA is used first. It does not check your eligibility."

### 12.2 Person (1 for single, 2 for couple; ids `"A"`, `"B"`)

| Field | Type | Rule |
|---|---|---|
| `label` | string, optional | 0 to 20 chars; defaults "Person A"/"Person B"; UI discourages real names; never logged |
| `isNewcomer` | boolean | informational and for UI guidance |
| `arrivalDate` | date, optional | informational timeline context only |
| `graduationDate` | date, optional | timeline context only |
| `employmentStartDate` | date, optional | timeline context only; **does not prorate income** |
| `canadianTaxResidentForFullYear` | boolean | required; false disables tax-based optimization (FR-25) |
| `estimatedEmploymentIncomeFor2026` | number | required; 0 to 1,000,000 |
| `availableTfsaRoom` | number | required; 0 to 1,000,000 |
| `availableRrspDeductionLimit` | number | required; 0 to 1,000,000 |
| `fhsaEligible` | boolean | required |
| `availableFhsaParticipationRoom` | number | required; 0 to 1,000,000; must be 0 if `fhsaEligible` is false |

### 12.3 User-defined allocation (optional in the API; always sent by the UI)

Array of `{ "person": "A"|"B", "account": "TFSA"|"RRSP"|"FHSA", "percent": number }`. Omitted destinations equal 0%. Percent has at most 2 decimals; the total must equal exactly 100.00. If omitted entirely, the response contains only the modeled scenario.

---

## 13. Calculation model

### 13.1 Rounding and numeric rules
- All money uses `BigDecimal`; no `double` or `float` in domain code.
- Intermediate bracket and credit arithmetic is exact (no rounding).
- **Rounding boundaries (scale 2, `HALF_UP`):** each tax component (federal tax, Ontario tax after credits and reduction, Ontario surtax, Ontario Health Premium) when computed; per-person total = sum of rounded components; estimated tax savings = difference of two rounded totals; each account's year-end balance; hypothetical balances; after-tax account values; effective and min/max rates (4 decimals, display only).
- **Percent-to-dollar conversion is not `HALF_UP` rounding.** It uses the capacity-aware largest-remainder method in 16.7, which preserves the budget to the cent.
- The modeled allocator works in exact cent amounts (the budget and rooms are in cents; the last increment is `min(increment, budgetLeft, roomLeft)`), so it needs no remainder adjustment.
- Growth factors use integer powers of `(1 + r)` with exact `BigDecimal` arithmetic.

### 13.2 Per-person simplified tax calculation
Inputs: income `I`, total deduction `D = RRSP contribution + FHSA contribution`.

1. `taxable = max(0, I - D)`.
2. **Federal:** progressive brackets on `taxable`; minus non-refundable credits (federal basic personal amount including any verified high-income phase-down rule; Canada employment amount limited by income) at the lowest federal rate; floor at 0.
3. **Ontario:** progressive brackets on `taxable`; minus the Ontario basic personal amount credit; floor at 0; **Ontario tax reduction** where the verified rule applies; floor at 0.
4. **Ontario surtax** on Ontario tax after credits and reduction, above verified thresholds.
5. **Ontario Health Premium** from the verified band table on the verified income basis.
6. `total = federal + ontario + surtax + OHP`.
7. `averageEstimatedTaxRate = total / I` when `I > 0`, else 0.

Exact formulas for steps 2 to 5 come from the official sources at verification time and are recorded in `docs/TAX_SOURCES.md` (section 14.3). This PRD intentionally does not restate them.

### 13.3 Estimated tax savings (exact increment, no +$1 approximation)
For a person with current deduction `D` and a proposed additional deductible contribution `delta`:

`estimatedTaxSavings = tax(I, D) - tax(I, D + delta)`

where `tax` is the full rounded total from 13.2. The same `delta` is the allocator's increment (which may be smaller than the configured increment, 16.4). A scenario's per-person savings is `tax(I, 0) - tax(I, D_final)`; it never exceeds `tax(I, 0)`.

**Telescoping property:** because each `tax` value is a rounded total and savings are differences of consecutive totals, the sum of a person's increment savings equals `tax(I, 0) - tax(I, D_final)` exactly. This is the basis for the explanation reconciliation in 16.6.

**Increment savings rate** = `estimatedTaxSavings / delta`, compared exactly (cross-multiplication) for decisions. Because savings are cent-rounded, the rate of a small partial increment can differ slightly from the bracket rate (16.6).

### 13.4 What the app does *not* do with savings
Estimated tax savings are **not** reinvested in the base projection and are **not** called refunds (section 17.4).

---

## 14. Tax-model limitations

### 14.1 Included
Federal brackets, Ontario brackets, federal basic personal amount, Canada employment amount, Ontario basic personal amount, Ontario surtax, Ontario Health Premium, Ontario tax reduction (supported cases), RRSP and FHSA deduction effects.

### 14.2 Excluded (displayed on the results screen)
CPP, CPP2, EI, tuition credits, child and family benefits, capital gains, dividends, rental income, foreign income, self-employment, disability-related credits, medical credits, donations, spousal transfers, and any refundable or non-refundable credit not listed in 14.1.

### 14.3 Source verification (mandatory before implementation)
Every numeric tax parameter must be **re-verified immediately before implementation** from official sources: **CRA**, **Department of Finance Canada**, and the **Ontario government**. No parameter is copied from memory, search snippets, or third-party calculators.

| Parameter group | Verify | Official source type |
|---|---|---|
| Federal brackets and rates, 2026 | thresholds, rates, lowest-bracket rate | CRA / Department of Finance |
| Federal basic personal amount | amount, high-income phase-down | CRA |
| Canada employment amount | amount and limiting rule | CRA |
| Ontario brackets and rates | thresholds, rates | Ontario / CRA ON428 guidance |
| Ontario basic personal amount | amount | Ontario / CRA |
| Ontario surtax | thresholds and rates | Ontario / CRA |
| Ontario Health Premium | band table and income basis | Ontario / CRA |
| Ontario tax reduction | parameters and formula | Ontario / CRA |
| Final vs. announced status of 2026 values | which values are final | CRA / Finance |

Each configuration value carries `source` (URL) and `retrievedOn` (date). `docs/TAX_SOURCES.md` lists every value, source, retrieval date, and the worksheet backing its tests. Every numeric tax parameter stays marked **"requires official-source verification"** until a maintainer records the source and date. If a 2026 value is not final, the app shows the config version and describes it as a planning parameter.

### 14.4 Residency limitation
Part-year residency involves additional rules outside this version. The interface says so, disables tax-based optimization when full-year residency is not confirmed, and never infers residency from dates.

---

## 15. Contribution-room handling

**Authoritative inputs per person:** `availableTfsaRoom`, `availableRrspDeductionLimit`, `availableFhsaParticipationRoom`, `fhsaEligible`, `canadianTaxResidentForFullYear`.

- The interface tells users to obtain room values from **their latest CRA records**. Wording stays general; no CRA field names are asserted without verification.
- A separate **"How these accounts work"** educational page explains TFSA, RRSP, and FHSA conceptually (including that first-home withdrawal rules exist and are not checked by the app). It is structurally separate from the input forms and states that rules can change and the page is not a calculator.
- **Not modeled:** previous TFSA withdrawals, non-resident TFSA contributions, pension adjustments, pension-adjustment reversals, past-service pension adjustments, RRSP-to-FHSA transfers, FHSA excess amounts, deduction carryforward beyond the entered limit.
- **Enforcement:** the allocator never allocates more than the entered room for any destination; a user-defined allocation above room is rejected (`ALLOCATION_EXCEEDS_ROOM` or `ALLOCATION_ROUNDING_EXCEEDS_ROOM`).
- **Unused room** per destination is reported for both scenarios.
- **Years 2 to 5:** future room is not modeled (17.2).

---

## 16. Allocation-engine specification (rule-based)

The tax-aware modeled allocation is a **short waterfall of explicit rules**, not an optimizer and not a scoring formula. It exists to give a transparent, explainable alternative to compare against the user's own allocation.

### 16.1 Inputs
Per-person income and room (section 12), annual budget `B = 12 x monthlySavings`, `fhsaFirstHomePlan`, `w = rrspWithdrawalTaxRate`, increment `inc` (configuration value, default **$100**).

### 16.2 Destinations
Up to six: A TFSA, A RRSP, A FHSA, B TFSA, B RRSP, B FHSA (B absent in single mode; FHSA absent for a person who is not eligible).

### 16.3 Treatment of accounts (assumptions shown in the UI)
- **TFSA:** non-deductible; no tax savings.
- **RRSP:** deductible; withdrawals assumed taxed at `w`.
- **FHSA when `fhsaFirstHomePlan` is true:** deductible; qualifying withdrawals **assumed tax-free**.
- **FHSA when `fhsaFirstHomePlan` is false:** deductible; treated like RRSP (withdrawals taxed at `w`). No other FHSA withdrawal rules are modeled.

### 16.4 Rules (applied to every increment, in this order; the first rule with a candidate applies)

Let `delta_d = min(inc, budgetLeft, roomLeft_d)` for a candidate destination `d` (so increments can be smaller than `inc`). A destination's **increment savings rate** is `estimatedTaxSavings(delta_d) / delta_d` from the person's current deduction total (13.3).

| Rule | Applies when | Choice | Tie-break |
|---|---|---|---|
| **R1: FHSA first** | `fhsaFirstHomePlan` is true and an eligible FHSA has room | FHSA destination with the **highest increment savings rate** | Person A before B |
| **R2: Deductible above withdrawal rate** | A deductible destination (RRSP; and FHSA when `fhsaFirstHomePlan` is false) has room and its increment savings rate is **greater than `w`** | Highest increment savings rate | FHSA before RRSP, then A before B |
| **R3: TFSA** | A TFSA has room | TFSA (non-deductible) | A before B |
| **R4: Remaining budget** | Budget remains and only deductible destinations with a rate **not above `w`** have room | Highest increment savings rate | FHSA before RRSP, then A before B |

**Stop condition:** when no destination has room, stop. `unallocated = budgetLeft` with reason `ROOM_EXHAUSTED` ("Entered available room across eligible accounts is less than your savings budget").

**Properties guaranteed:** never exceeds the budget; never exceeds entered room; deduction savings recomputed from each person's current deduction after every increment (so crossing a bracket boundary changes the rate mid-allocation); fully deterministic; the **expected return does not influence allocation** (it affects only projections).

### 16.5 Why this rule set (and what it is not)
- R1 reflects a stated product assumption: when the user ticks the first-home planning checkbox, FHSA is the only destination that is both deductible and assumed tax-free at withdrawal.
- R2 compares **today's deduction rate** with the **assumed future withdrawal tax rate**, a common planning heuristic. Changing `w` visibly changes the allocation, which is the point of the comparison.
- R3 and R4 make the budget usable without inventing preferences between accounts.
- **Not claimed:** optimality, maximum wealth, or a best choice. The heuristic implicitly values current-year tax savings that the base projection does not reinvest; the "tax savings if saved" hypothetical (17.4) shows that sensitivity. R1 prioritizes FHSA even when another person's RRSP deduction rate is higher; this is a documented policy, not a computed result.

### 16.6 Explanation (accurate for partial increments and changing rates)

**Merging.** Consecutive increments to the same destination under the same rule form one **step**, even when the savings rate changes within the step. A step is therefore a *total*, never a uniform per-$100 statement. There is **no `per100` field anywhere.**

**Step fields**
| Field | Meaning |
|---|---|
| `step`, `person`, `account`, `rule` | identity and rule applied |
| `amount` | actual dollars contributed in the step (sum of increments, including any smaller final increment) |
| `incrementCount` | number of increments merged |
| `deductible` | false for TFSA |
| `estimatedTaxSavings` | actual sum of the increments' exact estimated tax savings (`"0.00"` for TFSA) |
| `effectiveSavingsRate` | `estimatedTaxSavings / amount`, 4 decimals; labelled **"effective"** in the UI; `null` when not deductible |
| `savingsRateMin`, `savingsRateMax` | lowest and highest increment savings rate within the step, 4 decimals; `null` when not deductible |
| `nextIncrement` | for R2 or R4 steps that ended because the rule no longer applied: `{amount, estimatedTaxSavings, savingsRate}` of the next increment that was *not* taken; otherwise `null` |
| `reason` | template text built from the fields above |

**Display rule.** The UI shows "Effective rate X% (range Y% to Z%)", or only the effective rate when min equals max. It never shows a single rate as if every increment were identical.

**Reconciliation invariants (tested)**
1. Sum of step `amount` values per destination equals that destination's allocation.
2. Sum of step `estimatedTaxSavings` per person equals that person's modeled `estimatedTaxSavings` (by the telescoping property, 13.3).
3. `savingsRateMin <= effectiveSavingsRate <= savingsRateMax` after identical 4-decimal rounding.
4. Increment rates come from cent-rounded savings, so a small partial increment can show a rate slightly off the bracket rate. This is expected and is documented in the UI footnote on the explanation tab.

**Reason templates** cite the actual figures, for example:
- R1: "FHSA is used first: contributions are deductible and your first-home planning assumption treats qualifying withdrawals as tax-free."
- R2: "RRSP is used while each increment's estimated tax savings rate exceeds your assumed 20.00% RRSP withdrawal-tax rate. The next increment ($100.00) would have saved an estimated $15.00 (15.00%), which does not exceed 20.00%."
- R3: "No remaining deductible destination exceeds the assumed withdrawal-tax rate, so TFSA is used."

**Worked example of a merged step with changing rates and a partial final increment** (synthetic fixture, 20.3; single person, income 54,000; RRSP deduction limit 4,250.50; no FHSA; assumed withdrawal-tax rate 10%): the first 40 increments of $100 each save $30.00 (30.00%), the next 2 save $15.00 each (15.00%), and the final $50.50 increment saves $7.57 (14.99%, the cent rounding of $7.575). The merged step is:

```json
{ "step": 1, "person": "A", "account": "RRSP", "rule": "R2_DEDUCTIBLE_ABOVE_WITHDRAWAL_RATE",
  "amount": "4250.50", "incrementCount": 43, "deductible": true,
  "estimatedTaxSavings": "1237.57", "effectiveSavingsRate": "0.2912",
  "savingsRateMin": "0.1499", "savingsRateMax": "0.3000", "nextIncrement": null,
  "reason": "RRSP room was filled in 43 increments (42 of $100.00 and a final $50.50). Estimated tax savings rates ranged from 14.99% to 30.00%, all above your assumed 10.00% withdrawal-tax rate." }
```
Check: 40 x 30.00 + 2 x 15.00 + 7.57 = 1,237.57.

### 16.7 User-defined scenario: capacity-aware percent-to-dollar conversion

**Goal:** convert percentages to cent amounts whose total equals the budget **exactly**, without ever placing a positive rounding adjustment into a destination that has no remaining room.

**Method (largest remainder, capacity-aware, positive adjustments only)**
1. Work in integer cents. For each destination with `percent > 0`: `raw_d = budgetCents x percent / 100` (exact, may have a fractional cent), `base_d = floor(raw_d)`.
2. **Overage check:** if any `base_d` exceeds `room_d`, reject with `ALLOCATION_EXCEEDS_ROOM` (a clear overage, before any adjustment).
3. `leftover = budgetCents - sum(base_d)`. Because percentages total exactly 100.00, `0 <= leftover < number of destinations with percent > 0`.
4. Give **one cent at a time** to eligible destinations, ordered by **largest fractional remainder** (`raw_d - base_d`), then by **canonical destination order** (A TFSA, A RRSP, A FHSA, B TFSA, B RRSP, B FHSA). A destination is **eligible** only if `percent > 0` and `base_d + 1 cent <= room_d` (and it has not already received a cent in this pass).
5. If cents remain after every eligible destination has received one, or no destination is eligible, reject with `ALLOCATION_ROUNDING_EXCEEDS_ROOM` ("This allocation cannot be placed within your entered room after cent rounding").
6. Destinations with `percent = 0` never receive an adjustment.

**Guarantees:** the sum equals the budget to the cent; every amount is within one cent of its raw value (`base_d` or `base_d + 1 cent`); no amount exceeds room; no negative adjustments; result independent of the order of the input array; deterministic.

**Worked examples** (percentages total 100.00 in each):

| Case | Budget | Percentages and rooms | Result |
|---|---|---|---|
| Fractional cents, ample room | 100.01 | 33.33 / 33.33 / 33.34; ample room | raw 33.333333, 33.333333, 33.343334; base 33.33, 33.33, 33.34 (sum 100.00); leftover 1 cent goes to the largest fractional part (third): **33.33, 33.33, 33.35** |
| Same, third destination at its limit | 100.01 | same percentages; third room exactly 33.34 | third is ineligible; next largest remainder ties (first and second, 0.3333 cent each); canonical order picks the first: **33.34, 33.33, 33.34** |
| Rounding cannot fit | 1,000.01 | 50.00 / 50.00; both rooms exactly 500.00 | raw 500.005 each, base 500.00 each, leftover 1 cent, no eligible destination: **rejected, `ALLOCATION_ROUNDING_EXCEEDS_ROOM`** |
| Floor already over room | 1,000.00 | 60.00 / 40.00; first room 599.99 | base 600.00 exceeds 599.99: **rejected, `ALLOCATION_EXCEEDS_ROOM`** |

After conversion, the user-defined scenario uses the **same** tax-savings and projection functions as the modeled scenario.

### 16.8 Comparison output (separate metrics, no combined score)
For each scenario: contributions by person and account; estimated current-year tax savings (and five-year cumulative, constant by assumption); five-year projected pretax balances; estimated after-tax account value (RRSP, and FHSA when `fhsaFirstHomePlan` is false, reduced by `w`); unused room; unallocated savings; assumptions used. A generated **"What differs"** list states factual differences (dollar shifts by account type, metric differences) without ranking or recommending.

---

## 17. Projection specification

### 17.1 Method
- **H = 5** annual steps; year 1 = 2026 (the planning year).
- **Contributions at the start of each year**, growth `r` applied annually, balances reported at **year end**: `balance_t = round((balance_(t-1) + C) x (1 + r))` per person and account.
- Contribution `C` for each destination is its **year-1 allocation, repeated each year**.

### 17.2 Assumptions shown in the UI
- Future contribution room is **not modeled**; repeated contributions may exceed future limits. Warning `FUTURE_ROOM_NOT_MODELED` is always returned.
- Income, tax parameters, and tax savings are held at year-1 values (no salary growth, no indexation).
- Existing balances are not modeled.
- Return is constant and not a forecast; no volatility or sequence risk.

### 17.3 Outputs
Person-level and account-level balances per year; household pretax totals per year; at year 5, pretax balance and **estimated after-tax value** per account.

### 17.4 Values kept separate
1. **Projected account balances** from the original savings budget only.
2. **Estimated current-year tax savings** from deductible contributions, plus five-year cumulative (year-1 savings x 5).
3. *Hypothetical, display-only:* **"If estimated tax savings were also saved"** (`taxSavingsIfSaved`): each year's savings saved at that year's end and grown at `r` untaxed for the remaining years, rounded annually. Not used by the allocator and never added to the other values.

### 17.5 Not in the core
Monthly simulation, ten-year horizon, salary growth, inflation-adjusted brackets, Monte Carlo, volatility, return sequences, automatic future room, **first-home targets, goal comparison, or accessible-savings calculations**.

### 17.6 FHSA first-home planning assumption
`fhsaFirstHomePlan` changes only (a) the FHSA after-tax valuation (tax-free when true; reduced by `w` when false) and (b) whether allocation rule R1 applies. It has no date and no amount, and it never changes tax calculations or room limits.

---

## 18. Frontend requirements

**Stack:** React, TypeScript, Vite, React Hook Form, Zod, TanStack Query, Recharts, shadcn/ui (Radix primitives) with Tailwind.

| Requirement | Detail |
|---|---|
| Customer-app feel | Landing page, stepper with progress, plain language, inline help; not dashboard-first |
| Flow | Landing, household type, person and income, available room, savings and FHSA assumption, assumptions, user-defined allocation, results |
| Forms | RHF + Zod; Zod mirrors backend rules including cross-field rules (percent total, room exceeded, FHSA room without eligibility, first-home plan without an eligible person) with live feedback; the first-home checkbox is disabled when no person is FHSA-eligible |
| Results tabs | **Comparison** (side-by-side metrics), **Tax**, **Room**, **Allocation**, **Tax savings**, **Projection**, **Explanation**, **Assumptions and limits** |
| Explanation tab | Shows each step's actual amount, actual estimated tax savings, effective rate, and range; footnote on cent rounding for small increments; never a per-$100 figure |
| Charts | Recharts for allocation and projection; **every chart has an accessible data table** |
| Accessibility | Labelled inputs, errors tied to fields, focus management between steps, sufficient contrast, keyboard operation, no meaning by color alone; target WCAG 2.1 AA, checked with axe |
| Responsive | Usable at 360 px width; tables scroll inside their container |
| Part-year state | Banner; tax tabs replaced by education; modeled scenario disabled; user-defined projection shown |
| Budget exceeds room | If the budget exceeds total entered room, the UI warns before calculating and offers "Run tax-aware allocation only" (omits the user allocation; modeled-only results with unallocated explanation) |
| Allocation entry | Live total and per-destination room check; if the total is 100.00 but cent rounding cannot fit, the API error `ALLOCATION_ROUNDING_EXCEEDS_ROOM` is shown on the allocation step with a plain-language fix ("reduce the percentage in a full account by 0.01") |
| Loading and errors | Spinner with copy; after 3 seconds on the first call: "The server is waking up (free hosting); this can take about a minute." Up to 3 retries with backoff; problem-details errors mapped to field errors |
| Statelessness | Form state in memory only; reload resets; no browser storage APIs |
| Disclaimer | Persistent footer plus first-visit notice |
| Language rules | Automated scan of UI strings for banned terms ("refund", "take-home", "best", "optimal", "winner", "recommended allocation") |
| Config footer | Shows `configVersion` and the planning-year caveat |

---

## 19. Backend architecture

**Stack:** Java 17+, Spring Boot, Maven, Docker. Immutable records; thin controllers; backend validation is authoritative.

```
/backend/src/main/java/.../
  api/          controllers, request/response DTOs, problem-details handler
  config/       configuration loader and startup validation
  domain/
    tax/        per-person tax engine
    room/       room validation (user-entered values), eligibility rules
    allocation/ percent conversion, rule-based allocator, explanations
    projection/ five-year annual projection
```
(No `persistence` or `goal` package in the default release.)

**Rules**
- `domain/*` has **no Spring dependencies**; pure functions over immutable records.
- Controllers validate (Bean Validation plus custom cross-field validators), call `CalculationService`, and map results to DTOs.
- Problem details per RFC 7807 with `errors[]` of `{field, code, message}`.
- **Fail fast:** the application refuses to start if any required configuration field, `source`, `retrievedOn`, or `version` is missing.
- Jackson strict mode: unknown properties rejected; money parsed as `BigDecimal`; more than 2 decimals rejected.

**Configuration files** (`src/main/resources/tax/`), each with `effectiveTaxYear`, `version`, `source` (official URL), `retrievedOn`: `tax-2026.json` (federal brackets and rates, Ontario brackets and rates, selected credit amounts, Ontario surtax thresholds and rates, OHP bands, Ontario tax-reduction parameters) and `planning-defaults.json` (allocation increment, default assumptions, validation ranges).

**Docker:** multi-stage backend image; `docker-compose.yml` runs backend and a static-served frontend.

---

## 20. API contract (v1)

Base path `/api/v1`. OpenAPI generated and committed to `/docs/openapi.yaml`.

| Method | Path | Purpose | Phase |
|---|---|---|---|
| GET | `/health` | liveness for warm-up and platform checks | 0 |
| GET | `/meta` | supported province and year, `configVersion`, source list, limits | 5 |
| POST | `/calculate` | full stateless calculation; the request contains every input needed to reproduce the result | 3 (thin), 5 (complete) |

(Scenario-saving endpoints are future work, section 21.)

### 20.1 Error model
| Status | When | Example `code` |
|---|---|---|
| 400 | Validation failure | `UNSUPPORTED_PROVINCE`, `UNSUPPORTED_TAX_YEAR`, `NEGATIVE_VALUE`, `WRONG_PERSON_COUNT`, `MISSING_ROOM_VALUE`, `ALLOCATION_EXCEEDS_ROOM`, `ALLOCATION_ROUNDING_EXCEEDS_ROOM`, `ALLOCATION_TOTAL_NOT_100`, `BUDGET_EXCEEDS_TOTAL_ROOM`, `INVALID_ASSUMPTION`, `FHSA_ROOM_WITHOUT_ELIGIBILITY`, `FIRST_HOME_PLAN_WITHOUT_ELIGIBLE_FHSA`, `UNKNOWN_PROPERTY`, `MALFORMED_JSON` |
| 413 | Body over 16 KB | `PAYLOAD_TOO_LARGE` |
| 415 | Wrong content type | `UNSUPPORTED_MEDIA_TYPE` |
| 500 | Unexpected | generic message, no stack trace, request ID |

Part-year residency is a **business restriction, not an error**: `200` with warning `TAX_OPTIMIZATION_DISABLED_PART_YEAR` (FR-25).

### 20.2 Response shape
`configVersion`, `warnings[]`, `assumptions`, `perPersonTax`, `availableRoom`, `userAllocation`, `modeledAllocation`, `estimatedTaxSavings`, `projection`, `explanation`.

### 20.3 Example fixture (synthetic, **not real 2026 values**)

The examples use a **synthetic test configuration** (`configVersion: "synthetic-fixture-1"`) so every number is reproducible by hand. It exists only for documentation and mechanics tests and is never loaded in production.

| Synthetic parameter | Value |
|---|---|
| Federal brackets | 10% up to 50,000; 20% above |
| Federal credits | basic amount 10,000 plus employment amount min(income, 1,000), at 10% |
| Ontario brackets | 5% up to 50,000; 10% above |
| Ontario credit | basic amount 10,000 at 5% |
| Ontario tax reduction | parameters zero (never applies) |
| Surtax | 20% of Ontario tax above 3,000 |
| Health premium | 0 if taxable income is 20,000 or less; 300 above |

Production golden tests use the verified 2026 configuration and independently computed worksheets, never this fixture.

### 20.4 Example request (couple, first-home planning checked, user-defined allocation)

```json
{
  "mode": "COUPLE",
  "province": "ON",
  "taxYear": 2026,
  "monthlySavings": 1500,
  "fhsaFirstHomePlan": true,
  "assumptions": { "expectedAnnualReturn": 0.05, "rrspWithdrawalTaxRate": 0.20 },
  "persons": [
    {
      "id": "A", "label": "Person A", "isNewcomer": true, "arrivalDate": "2024-09-01",
      "canadianTaxResidentForFullYear": true,
      "estimatedEmploymentIncomeFor2026": 54000,
      "availableTfsaRoom": 6000, "availableRrspDeductionLimit": 8000,
      "fhsaEligible": true, "availableFhsaParticipationRoom": 3000
    },
    {
      "id": "B", "label": "Person B", "isNewcomer": true, "arrivalDate": "2024-09-01",
      "canadianTaxResidentForFullYear": true,
      "estimatedEmploymentIncomeFor2026": 45000,
      "availableTfsaRoom": 9000, "availableRrspDeductionLimit": 5000,
      "fhsaEligible": true, "availableFhsaParticipationRoom": 4000
    }
  ],
  "userAllocation": [
    { "person": "A", "account": "TFSA", "percent": 20.00 },
    { "person": "A", "account": "RRSP", "percent": 20.00 },
    { "person": "A", "account": "FHSA", "percent": 10.00 },
    { "person": "B", "account": "TFSA", "percent": 30.00 },
    { "person": "B", "account": "RRSP", "percent": 10.00 },
    { "person": "B", "account": "FHSA", "percent": 10.00 }
  ]
}
```
Annual budget = 1,500 x 12 = **18,000**. Total entered room = 35,000, so the budget fits. Each percentage x 18,000 is an exact cent amount, so no remainder adjustment occurs.

### 20.5 Example response (abridged; values computed from the synthetic fixture)

```json
{
  "configVersion": "synthetic-fixture-1",
  "warnings": [
    { "code": "FUTURE_ROOM_NOT_MODELED", "message": "Years 2 to 5 repeat year-1 contributions; future contribution room is not modeled." },
    { "code": "SIMPLIFIED_ESTIMATE", "message": "Simplified estimated income tax; excludes CPP, EI, and most credits." }
  ],
  "assumptions": {
    "expectedAnnualReturn": "0.05", "rrspWithdrawalTaxRate": "0.20",
    "horizonYears": 5, "contributionTiming": "START_OF_YEAR",
    "fhsaFirstHomePlan": true, "fhsaTreatment": "TAX_FREE_QUALIFYING_WITHDRAWAL",
    "allocationIncrement": "100.00", "annualBudget": "18000.00", "allocationMethod": "RULE_BASED_V1"
  },
  "perPersonTax": [
    { "person": "A", "components": { "federal": "4700.00", "ontario": "2400.00", "ontarioSurtax": "0.00", "ontarioHealthPremium": "300.00" },
      "simplifiedEstimatedIncomeTax": "7400.00", "averageEstimatedTaxRate": "0.1370" },
    { "person": "B", "components": { "federal": "3400.00", "ontario": "1750.00", "ontarioSurtax": "0.00", "ontarioHealthPremium": "300.00" },
      "simplifiedEstimatedIncomeTax": "5450.00", "averageEstimatedTaxRate": "0.1211" }
  ],
  "availableRoom": [
    { "person": "A", "tfsa": "6000.00", "rrspDeductionLimit": "8000.00", "fhsa": "3000.00", "fhsaEligible": true },
    { "person": "B", "tfsa": "9000.00", "rrspDeductionLimit": "5000.00", "fhsa": "4000.00", "fhsaEligible": true }
  ],
  "userAllocation": {
    "allocations": [
      { "person": "A", "account": "TFSA", "percent": "20.00", "amount": "3600.00" },
      { "person": "A", "account": "RRSP", "percent": "20.00", "amount": "3600.00" },
      { "person": "A", "account": "FHSA", "percent": "10.00", "amount": "1800.00" },
      { "person": "B", "account": "TFSA", "percent": "30.00", "amount": "5400.00" },
      { "person": "B", "account": "RRSP", "percent": "10.00", "amount": "1800.00" },
      { "person": "B", "account": "FHSA", "percent": "10.00", "amount": "1800.00" }
    ],
    "unusedRoom": { "A": { "tfsa": "2400.00", "rrsp": "4400.00", "fhsa": "1200.00" }, "B": { "tfsa": "3600.00", "rrsp": "3200.00", "fhsa": "2200.00" } },
    "unallocated": "0.00"
  },
  "modeledAllocation": {
    "label": "Tax-aware modeled allocation under these assumptions",
    "allocations": [
      { "person": "A", "account": "FHSA", "amount": "3000.00" },
      { "person": "B", "account": "FHSA", "amount": "4000.00" },
      { "person": "A", "account": "RRSP", "amount": "1000.00" },
      { "person": "A", "account": "TFSA", "amount": "6000.00" },
      { "person": "B", "account": "TFSA", "amount": "4000.00" }
    ],
    "unusedRoom": { "A": { "tfsa": "0.00", "rrsp": "7000.00", "fhsa": "0.00" }, "B": { "tfsa": "5000.00", "rrsp": "5000.00", "fhsa": "0.00" } },
    "unallocated": "0.00", "unallocatedReason": null
  },
  "estimatedTaxSavings": {
    "userDefined": {
      "perPerson": [
        { "person": "A", "totalDeductions": "5400.00", "taxAfterDeductions": "5990.00", "estimatedTaxSavings": "1410.00" },
        { "person": "B", "totalDeductions": "3600.00", "taxAfterDeductions": "4910.00", "estimatedTaxSavings": "540.00" }
      ],
      "annualTotal": "1950.00", "cumulativeFiveYears": "9750.00"
    },
    "modeled": {
      "perPerson": [
        { "person": "A", "totalDeductions": "4000.00", "taxAfterDeductions": "6200.00", "estimatedTaxSavings": "1200.00" },
        { "person": "B", "totalDeductions": "4000.00", "taxAfterDeductions": "4850.00", "estimatedTaxSavings": "600.00" }
      ],
      "annualTotal": "1800.00", "cumulativeFiveYears": "9000.00"
    }
  },
  "projection": {
    "startYear": 2026, "horizonYears": 5,
    "userDefined": {
      "householdPretaxByYear": ["18900.00", "38745.00", "59582.27", "81461.37", "104434.44"],
      "yearFiveBalances": [
        { "person": "A", "account": "TFSA", "pretax": "20886.88", "estimatedAfterTax": "20886.88" },
        { "person": "A", "account": "RRSP", "pretax": "20886.88", "estimatedAfterTax": "16709.50" },
        { "person": "A", "account": "FHSA", "pretax": "10443.45", "estimatedAfterTax": "10443.45" },
        { "person": "B", "account": "TFSA", "pretax": "31330.33", "estimatedAfterTax": "31330.33" },
        { "person": "B", "account": "RRSP", "pretax": "10443.45", "estimatedAfterTax": "8354.76" },
        { "person": "B", "account": "FHSA", "pretax": "10443.45", "estimatedAfterTax": "10443.45" }
      ],
      "estimatedAfterTaxAccountValue": "98168.37",
      "taxSavingsIfSaved": "10774.99"
    },
    "modeled": {
      "householdPretaxByYear": ["18900.00", "38745.00", "59582.26", "81461.39", "104434.47"],
      "yearFiveBalances": [
        { "person": "A", "account": "TFSA", "pretax": "34811.48", "estimatedAfterTax": "34811.48" },
        { "person": "A", "account": "RRSP", "pretax": "5801.92", "estimatedAfterTax": "4641.54" },
        { "person": "A", "account": "FHSA", "pretax": "17405.75", "estimatedAfterTax": "17405.75" },
        { "person": "B", "account": "TFSA", "pretax": "23207.66", "estimatedAfterTax": "23207.66" },
        { "person": "B", "account": "FHSA", "pretax": "23207.66", "estimatedAfterTax": "23207.66" }
      ],
      "estimatedAfterTaxAccountValue": "103274.09",
      "taxSavingsIfSaved": "9946.14"
    }
  },
  "explanation": {
    "steps": [
      { "step": 1, "person": "A", "account": "FHSA", "rule": "R1_FHSA_FIRST",
        "amount": "3000.00", "incrementCount": 30, "deductible": true,
        "estimatedTaxSavings": "900.00", "effectiveSavingsRate": "0.3000",
        "savingsRateMin": "0.3000", "savingsRateMax": "0.3000", "nextIncrement": null,
        "reason": "FHSA is used first: contributions are deductible and your first-home planning assumption treats qualifying withdrawals as tax-free. A has the higher savings rate." },
      { "step": 2, "person": "B", "account": "FHSA", "rule": "R1_FHSA_FIRST",
        "amount": "4000.00", "incrementCount": 40, "deductible": true,
        "estimatedTaxSavings": "600.00", "effectiveSavingsRate": "0.1500",
        "savingsRateMin": "0.1500", "savingsRateMax": "0.1500", "nextIncrement": null,
        "reason": "A's FHSA room is used, so B's FHSA room is next under the same first-home planning assumption." },
      { "step": 3, "person": "A", "account": "RRSP", "rule": "R2_DEDUCTIBLE_ABOVE_WITHDRAWAL_RATE",
        "amount": "1000.00", "incrementCount": 10, "deductible": true,
        "estimatedTaxSavings": "300.00", "effectiveSavingsRate": "0.3000",
        "savingsRateMin": "0.3000", "savingsRateMax": "0.3000",
        "nextIncrement": { "amount": "100.00", "estimatedTaxSavings": "15.00", "savingsRate": "0.1500" },
        "reason": "RRSP is used while each increment's estimated tax savings rate exceeds your assumed 20.00% RRSP withdrawal-tax rate. The next increment ($100.00) would have saved an estimated $15.00 (15.00%), which does not exceed 20.00%." },
      { "step": 4, "person": "A", "account": "TFSA", "rule": "R3_TFSA",
        "amount": "6000.00", "incrementCount": 60, "deductible": false,
        "estimatedTaxSavings": "0.00", "effectiveSavingsRate": null,
        "savingsRateMin": null, "savingsRateMax": null, "nextIncrement": null,
        "reason": "No remaining deductible destination exceeds the assumed withdrawal-tax rate, so TFSA is used. TFSA is treated alike for both people; A is filled first by the fixed tie-break." },
      { "step": 5, "person": "B", "account": "TFSA", "rule": "R3_TFSA",
        "amount": "4000.00", "incrementCount": 40, "deductible": false,
        "estimatedTaxSavings": "0.00", "effectiveSavingsRate": null,
        "savingsRateMin": null, "savingsRateMax": null, "nextIncrement": null,
        "reason": "A's TFSA room is used, so the remaining budget goes to B's TFSA." }
    ],
    "whatDiffers": [
      "Contributions by account type (per year): RRSP $5,400.00 (yours) vs. $1,000.00 (modeled); FHSA $3,600.00 vs. $7,000.00; TFSA $9,000.00 vs. $10,000.00.",
      "Estimated current-year tax savings: $1,950.00 (yours) vs. $1,800.00 (modeled).",
      "Estimated after-tax account value at year 5: $98,168.37 (yours) vs. $103,274.09 (modeled), a difference of $5,105.72 under the 20.00% withdrawal-tax assumption."
    ]
  }
}
```

**Consistency checks used to produce this example:** each scenario allocates exactly 18,000; every destination is within entered room; tax after deductions is below tax before; five-year cumulative savings equal 5 x annual; modeled steps follow rules R1 to R3 in order; **step amounts sum to each destination's allocation, and step savings sum per person to the modeled per-person savings (A: 900 + 300 = 1,200; B: 600)**; pretax household totals differ by cents between scenarios only because each account balance is rounded separately. No combined score is computed.

### 20.6 Example: part-year restriction (FR-25)
The same request with person B's `canadianTaxResidentForFullYear: false` returns `200` with `warnings[0].code = "TAX_OPTIMIZATION_DISABLED_PART_YEAR"`; `perPersonTax`, `estimatedTaxSavings`, and `modeledAllocation` are `null`; the user-defined projection is returned from the entered allocation and assumptions only.

### 20.7 Example: modeled-only run (budget exceeds total room)
A request with `userAllocation` omitted and a budget above total entered room returns `200` with only the modeled scenario and `unallocated > 0` with `unallocatedReason: "ROOM_EXHAUSTED"`. The same request **with** `userAllocation` returns `400 BUDGET_EXCEEDS_TOTAL_ROOM`.

### 20.8 Example: first-home plan without an eligible FHSA
A request with `fhsaFirstHomePlan: true` where every person has `fhsaEligible: false` returns `400 FIRST_HOME_PLAN_WITHOUT_ELIGIBLE_FHSA`.

---

## 21. Persistence decision

**Decision (final): the default release is stateless.** There is no server persistence and no browser storage. Calculation is a pure function of the request. This keeps sensitive household inputs off servers and out of browsers, removes consent, expiry, deletion, and abuse-control work, and avoids a second cold-starting free service. PostgreSQL evidence already exists elsewhere on the résumé.

### 21.1 Later scope (Phase 8, post-release, optional, never blocks v1.0.0)
- **8a: Local save and URL-fragment share link.** Explicit "Save on this device" and "Copy link" actions; versioned schema validated with Zod on load; link in the URL fragment so it never reaches server logs; consent text that anyone with the link can see the inputs; a "Clear saved data" control; link disabled if over a safe length.
- **8b: PostgreSQL anonymous sharing (only after 8a, only if wanted).** Inputs only, random 128-bit ID, 30-day expiry, results never stored, no names, emails, SINs, account numbers, or credentials; explicit consent; never log request bodies; unknown and expired links both return 404; rate limiting on the save endpoint only; manual deletion is a later enhancement. Age, income, arrival dates, contribution room, and household details are sensitive financial information even without direct identifiers.

---

## 22. Security and privacy

| Control | Detail |
|---|---|
| Statement | "No direct identifiers are collected. User-entered household and financial details may still be sensitive and should be minimized, protected, and automatically deleted when persistence is enabled." |
| CORS | Strict allowlist: the production frontend origin and `localhost` for development; no wildcard |
| Request size | Backend rejects bodies over 16 KB (413) before deserialization |
| Unknown properties | Rejected (`UNKNOWN_PROPERTY`) |
| Logging | Never log request bodies or `label`; log request ID, route, status, latency |
| Errors | Generic 500 messages in production; no stack traces |
| Secrets | None required in the core release; any future secret via environment variables; secret scanning on the repository |
| Dependencies | Dependabot for Maven and npm; CI dependency vulnerability check |
| Headers | CSP, `X-Content-Type-Options`, `Referrer-Policy`, frame protections on the frontend host; basic equivalents on the API |
| Rate limiting | Not in the default release (no persistence endpoint); only for the save endpoint if Phase 8b is built |
| Client storage | None in the default release (tested) |

---

## 23. Test strategy

### 23.1 JUnit 5 (backend)
| Area | Cases |
|---|---|
| Tax brackets | Every supported bracket boundary (below, at, above) for federal and Ontario |
| Surtax | Boundaries at each verified threshold |
| OHP | Boundaries at every verified band edge |
| Ontario tax reduction | Applies, partially applies, does not apply |
| Edge incomes | Zero income; very high income including any verified federal basic-amount phase-down |
| Deductions | RRSP-only, FHSA-only, combined; deduction larger than income (taxable income floors at 0) |
| Couples | Partner calculations independent; swapping A and B swaps outputs |
| Allocation rules | Each of R1 to R4 selected in the documented order; **rule-conformance property test** (replay each step and assert it is the first applicable rule); rate exactly equal to `w` goes to TFSA (not R2); crossing a bracket boundary changes the rate mid-allocation; **monotonicity** (raising `w` never increases R2 allocations); FHSA treated like RRSP when `fhsaFirstHomePlan` is false (R1 absent, after-tax value reduced by `w`); ineligible FHSA excluded; checkbox true with no eligible person rejected |
| Partial increments | Room not a multiple of the increment (e.g. 4,250.50); final increment smaller than `inc`; budget not a multiple of `inc`; room smaller than `inc` |
| Explanation accuracy | Merged step spanning changing rates (the 4,250.50 worked example: 1,237.57 savings, effective 0.2912, range 0.1499 to 0.3000); no `per100` field exists; reconciliation invariants 1 to 3 (16.6) for generated inputs |
| Constraints | Allocation never exceeds room; never exceeds budget; deterministic tie-breaking; `unallocated` and reason reported |
| **Percent conversion (16.7)** | Sum equals budget to the cent for generated inputs; fractional-cent cases (33.33/33.33/33.34 on 100.01); **tight room** (destination exactly at room cannot receive a cent; next destination does); cent rejected when no destination can accept (`ALLOCATION_ROUNDING_EXCEEDS_ROOM`); floor-over-room rejected (`ALLOCATION_EXCEEDS_ROOM`); zero-percent destinations never adjusted; ties resolved by canonical order; **input-order independence** (shuffled array gives identical amounts); amounts within one cent of raw; large budgets (600,000) and tiny percentages (0.01) |
| Tax savings | `estimatedTaxSavings <= tax(I, 0)`; exact increment (not a marginal-rate approximation); telescoping sum equals total savings |
| Projection | Zero-return fixture (balances equal contributions x years); hand-computed fixture; year-end rounding |
| Rounding | Half-up cents cases at each documented boundary |
| Validation | One test per validation code in section 20.1 |

Golden tax expectations come from **independently computed worksheets** in `docs/tax-worksheets/`, tied to the verified configuration. Tests on the synthetic fixture verify mechanics only.

**Mockito** only at real service boundaries: controller slice tests mock `CalculationService`; configuration-loading failure paths use a stubbed source. Pure domain calculations are never mocked.

### 23.2 Jest and React Testing Library
Single vs. couple conditional forms; field validation (including percent total and room exceeded); first-home checkbox disabled when no person is FHSA-eligible and included in the request when ticked; example-data loading; user-defined allocation entry; display of the rounding-rejection message; results from a fixture response; **explanation rendering of actual amount, actual savings, effective rate, and range (and absence of any per-$100 text)**; part-year restricted state; modeled-only state when budget exceeds room; error and loading states including the cold-start message; accessible result tables paired with each chart; banned-term scan of UI strings; assertion that no browser storage API is called.

### 23.3 Playwright (one polished flow)
1. Open the application.
2. Load the example Ontario couple.
3. Edit income and contribution-room values.
4. Enter a monthly savings amount.
5. Define a user allocation.
6. Run the calculation.
7. Compare the user-defined and tax-aware scenarios (side-by-side metrics).
8. Inspect an allocation explanation (actual amount, estimated savings, effective rate and range).
9. Toggle the first-home planning checkbox and confirm the modeled allocation and explanation update (FHSA rule R1 appears or disappears).
10. Assert that no `localStorage`, `sessionStorage`, or cookies were set by the app.

### 23.4 Postman/Newman
Valid single request; valid couple request; negative values; excess allocation; **rounding-rejection (two 50.00% destinations at exactly 500.00 room with budget 1,000.01)**; **fractional-percentage success (33.33/33.33/33.34)**; unsupported year; unsupported province; first-home plan without an eligible FHSA; part-year warning and restriction; missing required room; malformed JSON; unknown fields; oversized payload; deterministic repeated calculation (identical response ignoring request ID).

### 23.5 Traceability (every core requirement has a phase and a test)
| Requirement | Phase | Primary tests |
|---|---|---|
| FR-1 Guest + example | 6 | Jest example load; Playwright step 2 |
| FR-2 Single/couple | 2, 6 | JUnit couples; Jest conditional forms; Newman valid single/couple |
| FR-3 Ontario only | 5 | JUnit validation; Newman unsupported province |
| FR-4 2026 only | 5 | JUnit validation; Newman unsupported year |
| FR-5 Separate calculations | 1, 2 | JUnit partner independence |
| FR-6 Aggregation after | 2, 4 | JUnit household sum property |
| FR-7 Income | 1, 5 | JUnit edge incomes; validation tests |
| FR-8, 9, 10 Room inputs | 2 | JUnit room validation; Newman missing room; Jest forms |
| FR-11 Monthly savings | 3 | JUnit budget limits; partial-increment cases; Jest |
| FR-12 User allocation | 3, 6 | JUnit percent-conversion suite; Newman rounding cases; Jest allocation; Playwright step 5 |
| FR-13 Modeled allocation | 3 | JUnit rule tests, conformance and monotonicity properties |
| FR-14 Two-scenario comparison | 3, 6 | Jest results; Playwright step 7 |
| FR-15 Projection | 4 | JUnit zero-return and hand fixtures |
| FR-16 Assumptions | 4, 6 | JUnit range validation; Jest |
| FR-17 Tax estimate | 1 | JUnit boundaries and worksheets |
| FR-18 Tax savings | 2 | JUnit exact-increment, cap, and telescoping tests |
| FR-19 Room validation | 2, 3 | JUnit never-exceed; Newman excess allocation |
| FR-20 Explanations | 3, 6 | JUnit accuracy and reconciliation tests; Jest rendering; Playwright step 8 |
| FR-21 React workflow | 6 | Jest; Playwright; axe checks |
| FR-22 REST API docs | 5 | OpenAPI check in CI; Newman suite |
| FR-23 Docker local | 0, 7 | CI Docker build and compose smoke test |
| FR-24 Live deployment | 7 | Post-deploy smoke test; Playwright against the live URL |
| FR-25 Part-year restriction | 3, 5 | JUnit restriction; Newman; Jest state |
| FR-26 FHSA first-home planning assumption | 3, 4, 6 | JUnit R1 and FHSA-treatment toggle; Newman ineligible case; Jest checkbox; Playwright step 9 |
| FR-27 Stateless | 6, 7 | Jest storage-API assertion; Playwright step 10 |

---

## 24. CI and deployment

### 24.1 GitHub Actions (every pull request and `main`)
Backend compile and JUnit; frontend ESLint; TypeScript type check; Jest; production frontend build; Docker builds for backend and frontend; start the compose stack; Newman collection; one Playwright workflow against the compose stack; dependency vulnerability check. Required checks on `main`. Jenkins is not used (already demonstrated professionally).

### 24.2 Deployment
- **Frontend:** static host (Vercel), API base URL via environment variable.
- **Backend:** Docker container on a free-tier web service (Render).
- **PostgreSQL:** only if Phase 8b is built.
- **Free-tier facts to re-verify before deploying** (checked October 2026): Render free web services spin down after 15 minutes without inbound traffic and take about a minute to wake, with 512 MB RAM and 0.1 CPU; Render free Postgres expires after 30 days (do not use it); Neon free compute suspends after 5 minutes idle.
- **Cold-start handling:** clear loading message, up to 3 retries with backoff, `/health` call on page load, **measured cold-start time recorded in the README**. Light JVM tuning only if the measured time is unacceptable.
- **Not included:** keep-warm pings, native-image work, distributed rate limiting, infrastructure added only for keywords.

---

## 25. Git and GitHub development plan

- **One issue per meaningful feature or defect**, labelled by phase and type; acceptance criteria copied from section 11.
- **Feature branches** (`feat/tax-engine-boundaries`, `fix/rounding-half-up`); small, descriptive commits (conventional-commit style); squash or rebase merges.
- **Pull requests** per phase milestone, even solo, with review notes: what changed, what was verified, what was deliberately left out.
- **Tagged releases:** `v0.1.0` (Phase 0), `v0.2.0` (vertical slice), `v0.5.0` (Phase 5), `v1.0.0` (Phase 7), `v1.1.0` (Phase 8a, if built).
- **ADRs** in `docs/adr/`: ADR-001 monorepo; ADR-002 user-entered room; ADR-003 BigDecimal and rounding boundaries; ADR-004 rule-based allocator and tie-breaking; ADR-005 annual projection and repeated contributions; ADR-006 stateless default (save/share deferred); ADR-007 hosting and cold-start handling; ADR-008 configuration-as-data with source citations; ADR-009 product name; **ADR-010 capacity-aware largest-remainder percent conversion; ADR-011 explanation fields (actual totals, effective rate, range); ADR-012 first-home checkbox replaces the target**.
- **GitHub milestones:** M0 to M7 map one-to-one to Phases 0 to 7; M8 optional.
- **No fake collaboration or manufactured activity:** history reflects real work; no backdated or empty commits.

---

## 26. Implementation phases

Sizes are relative: **S** small, **M** medium, **L** large. No dates.

| Phase | Size | Depends on | Scope | Exit criteria | Cuttable items |
|---|---|---|---|---|---|
| **0. Foundation** | S | none | Monorepo, CI skeleton, Docker, `/health`, frontend shell, README skeleton, **name availability check** (section 32) | CI green; compose runs; health reachable; `v0.1.0` | none |
| **1. Tax config and engine** | L | 0, **source verification gate** | Verified configuration with sources; federal, Ontario, surtax, OHP, reduction; BigDecimal and rounding; worksheets | Boundary and worksheet tests green; startup fails on incomplete config; `TAX_SOURCES.md` complete | Ontario tax-reduction edge cases beyond a documented supported range |
| **2. Room inputs and tax savings** | M | 1 | Room validation, eligibility, deduction effects, exact-increment savings (telescoping), couple independence | Validation and savings tests green; property tests pass | none |
| **3. Allocation (+ vertical slice)** | L | 2 | Capacity-aware percent conversion, rule-based allocator R1 to R4 with partial increments, accurate explanations, FHSA planning checkbox, thin `/calculate`, minimal frontend page for "Try an example" | Rule, conversion, and explanation-accuracy tests green; **Vertical slice `v0.2.0`: the example household runs end to end locally and deployed, showing both allocations and explanations** | none |
| **4. Projection** | S | 3 | Five-year annual projection, FHSA after-tax valuation by checkbox, hypothetical tax-savings-if-saved | Zero-return and hand fixtures green | Hypothetical "if saved" display |
| **5. REST API** | M | 3, 4 | Complete `/calculate`, `/meta`, validation codes, problem details, OpenAPI, Postman/Newman, size cap | Newman suite green in CI; OpenAPI committed | none |
| **6. React workflow** | L | 5 | Full wizard, results tabs, side-by-side comparison, charts with tables, responsive design, part-year and modeled-only states | Jest suite green; axe checks pass; usable at 360 px | Extra charts |
| **7. Hardening and launch** | L | 6 | Playwright flow, CI gates, Docker finalized, security headers, deployment, README, demo, measured cold start, `v1.0.0` | Live E2E passes; README lets a stranger run it locally; definition of done met | Demo GIF polish |
| **8. Save and share (optional, post-release)** | M | 7 | 8a local save and URL-fragment link; then 8b PostgreSQL sharing if wanted | Own tests green; privacy page updated; Playwright flow extended | Entire phase; 8b independent of 8a |

**Rule:** do not start phase N+1 with phase N's required tests red.

---

## 27. Cut order

If scope must be reduced, cut in this order:
1. Additional charts (keep tables).
2. User-editable advanced assumptions (keep fixed defaults, shown).
3. Projection detail beyond annual totals.

Save and share (all forms) and first-home targets are outside the default scope and are not part of the cut order.

**Never cut:** source-backed tax configuration; per-person tax calculations; user-entered account room; allocation validation (including capacity-aware rounding); accurate explanations; core JUnit tests; React workflow; Jest tests; one Playwright flow; Postman collection; Docker; CI; live deployment.

---

## 28. Definition of done

- [ ] Every numeric tax parameter has an official source URL and retrieval date, verified immediately before implementation.
- [ ] Live frontend and backend URLs work with no login; Playwright passes against the live site.
- [ ] `docker compose up` runs the full app locally from the README alone.
- [ ] Golden tax tests (worksheet-backed) green; allocation rule, percent-conversion, and explanation-accuracy tests green; allocator never exceeds budget or room; percent conversion preserves the budget to the cent.
- [ ] Banned-term scan passes (no "refund", "take-home", "best", "optimal", "winner" in the UI); no combined score or ranking appears in the UI or API; no per-$100 explanation field exists.
- [ ] No browser storage or server persistence in the default release (tested).
- [ ] Persistent disclaimer visible; limitations page complete.
- [ ] Measured cold-start time and measured test counts recorded in the README (only measured figures are claimed).
- [ ] CI green on `main` with required checks.
- [ ] OpenAPI committed and consistent with the Newman collection.
- [ ] ADRs written; milestones M0 to M7 closed; `v1.0.0` tagged.

---

## 29. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Tax parameters wrong or not final for 2026 | Credibility loss | Source-backed config, worksheets, planning-year language, config version displayed |
| Users treat estimates as advice | Harm and credibility | Persistent disclaimer, language rules, banned-term test, no ranking or "best" label |
| Allocation rules challenged as arbitrary | Interview challenge | Rules are explicit, ordered, documented with rationale; no optimality claim; sensitivity to `w` demonstrated; limitations stated (16.5) |
| R1 prioritizes FHSA over a higher-rate RRSP deduction | Interview challenge | Documented policy tied to the user's first-home planning checkbox, shown in explanations |
| Explanation implies uniform rates across a merged step | Misleading display | Actual totals, effective rate, min/max range; no per-$100 field; reconciliation tests |
| Cent rounding changes small-increment rates | Confusing explanation | Documented footnote and worked example (14.99% vs 15.00%) |
| Rounding remainder pushes a destination over room | Invalid allocation | Capacity-aware largest-remainder method; explicit rejection code; tight-room tests |
| Repeated contributions exceed future room | Misleading projection | Always-on warning; labelled illustration |
| Free-tier cold start | Weak first impression | Loading message, retries, measured time; no keep-warm hacks |
| Scope creep | Never finishes | Release levels, cut order, non-goals, save/share and first-home target deferred |
| Residency misunderstanding | Wrong estimates | Explicit full-year confirmation; restricted mode otherwise |
| Name conflict | Rebranding | Availability check in Phase 0; fallback name decided (section 32) |

---

## 30. Interview discussion points

- Why user-entered contribution room instead of computing it (data authority, scope honesty).
- Pure domain core, configuration-as-data, fail-fast startup validation.
- `BigDecimal`, defined rounding boundaries, and the telescoping property of exact-increment tax savings.
- **Largest-remainder cent allocation with capacity constraints:** why `HALF_UP` per item can break a budget, why adjustments are positive-only, and how tight-room cases are handled or rejected.
- Why a transparent rule-based allocator instead of an "optimizer": explainability, determinism, no unprovable claims; how the withdrawal-tax assumption changes results; what the rules do not capture.
- **Explanation design:** reporting actual totals, an effective rate, and a range instead of a convenient per-$100 figure; reconciliation invariants as tests.
- Rule-conformance and monotonicity property tests as a substitute for an optimality proof.
- Showing separate metrics instead of a combined score, and why.
- Part-year residency and first-home targets as deliberate scope boundaries; the checkbox as a minimal assumption switch.
- Stateless by design and privacy reasoning; what Phase 8 would add and why it is deferred.
- Testing pyramid: worksheet-backed golden tests, property tests, contract tests, one E2E.
- Free-tier cold starts and honest handling.

---

## 31. Résumé bullet templates (placeholders only for measured results)

**BracketWise: Ontario Household Savings Scenario Planner** | Java, Spring Boot, React, TypeScript, Docker, GitHub Actions
- Built a Spring Boot and React/TypeScript planner that compares user-defined and rule-based TFSA/RRSP/FHSA allocations for singles and couples under a simplified, source-cited 2026 federal and Ontario tax model with `BigDecimal` arithmetic.
- Implemented a deterministic, explainable rule-based allocator with exact-increment tax-savings calculations, cent-exact capacity-aware percentage conversion, and a five-year annual projection, covered by rule-conformance, monotonicity, and reconciliation property tests ([N] generated cases).
- Wrote [N] JUnit tests (bracket, surtax, health-premium, and rounding boundaries from independently computed worksheets), [N] Jest/React Testing Library tests, a Playwright end-to-end flow, and a Newman suite of [N] API checks in GitHub Actions ([X]% measured coverage).
- Designed a documented stateless REST API (OpenAPI, RFC 7807 errors, strict validation, request-size cap) and deployed the Dockerized backend and static frontend publicly with a measured cold start of [X] seconds.

Use a bullet only after the feature is implemented and the figure is measured. Add a persistence bullet only if Phase 8 is built.

---

## 32. Decision log (all resolved)

**Status: FINAL. Approved for build (v2.2).** No open decisions remain. Two items are entry gates (tasks with an owner), not open questions.

| # | Decision | Resolution | Effect |
|---|---|---|---|
| 1 | How tax savings relate to allocation | **No combined objective.** The modeled allocation is rule-based (section 16); tax savings, after-tax account value, and balances are shown as separate metrics; the hypothetical "tax savings if saved" is display-only | Section 16, 17.4 |
| 2 | Persistence | Default release is **stateless**; local save, URL-fragment share, and PostgreSQL sharing are later scope (Phase 8) | FR-27 |
| 3 | Default assumptions | Return **4%**, RRSP withdrawal-tax rate **20%**, both editable and labelled assumptions | Section 12.1 |
| 4 | Years 2 to 5 contributions | **Repeat year-1 contributions** with a permanent `FUTURE_ROOM_NOT_MODELED` warning; no per-account future-room inputs | Section 17 |
| 5 | Budget exceeds total room | API: `400 BUDGET_EXCEEDS_TOTAL_ROOM` when a user allocation is present; modeled-only run (omitted `userAllocation`) reports `unallocated`. UI offers "Run tax-aware allocation only" | Sections 18, 20.7 |
| 6 | Supported income range | **0 to 1,000,000**; federal basic-amount phase-down is implemented if the verified rules apply in that range | Sections 12.2, 13.2 |
| 7 | Verified tax parameters | **Entry gate for Phase 1** (owner: Jill): collect and record every value, source URL, and retrieval date before the engine is built | Section 14.3, `TAX_SOURCES.md` |
| 8 | Product name | **BracketWise**; fallback **Headroom CA**. **Entry gate in Phase 0**: check GitHub, domain, and app-store availability and record the result in ADR-009; non-blocking for engineering | Section 33 |
| 9 | First-home planning input | **A single household checkbox** (`fhsaFirstHomePlan`), no amount, no date, no target comparison; applies to FHSA-eligible people; requires at least one eligible person | FR-26, sections 12.1, 17.6 |
| 10 | FHSA when the checkbox is unchecked | Treated as a deductible, taxable-withdrawal account like RRSP; R1 off | Section 16.3 |
| 11 | Percent-to-dollar rounding | **Capacity-aware largest-remainder method**, positive one-cent adjustments only, never into a destination without room, canonical tie order, rejection with `ALLOCATION_ROUNDING_EXCEEDS_ROOM` when no destination can accept a cent; modeled allocator needs no adjustment (works in exact cents) | Section 16.7, ADR-010 |
| 12 | Explanation fields | Return **actual amount, actual estimated tax savings, effective rate (labelled), min and max increment rate, and next-increment boundary**; no per-$100 field; merged steps may span rates; reconciliation invariants tested | Section 16.6, ADR-011 |

**Change control:** modifying any resolved decision requires a new ADR and a PRD version bump.

---

## 33. Project name

**Final: BracketWise.** The two paths are the two compared scenarios; "two" also fits couples; "CA" signals Canada. "RoomWise" is retired because it reads as a housing or roommate product.

Conceptual evaluation only (trademark, domain, and store availability are checked in Phase 0, not here):

| Name | Communicates | Role |
|---|---|---|
| **BracketWise** | Scenario comparison, couples, Canada | **Final** |
| **Headroom CA** | Financial headroom and contribution room, Canada | **Fallback** if BracketWise conflicts |
| Allotted | Savings allocation | Alternative |
| NorthPath Planner | Canadian household planning | Alternative |
| SideBySide Savings | Scenario comparison, couples | Alternative |
