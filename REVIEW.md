# Ragnala POS — Project Review (Pre-Implementation)

Version: 1.0
Status: Review of PRD.md v1.0 + DESIGN.md v1.0
Scope: Documentation only. No code.

---

# 1. Cross-Document Inconsistencies

## 1.1 Order status model differs between docs

PRD §9 Order Management defines 7 states:

```
Pending → Waiting Payment → Paid → Preparing → Ready → Completed → Archived
```

DESIGN.md Barista Journey defines 5:

```
Receive Order → Accept Payment → Prepare → Ready → Completed
```

Problems:
- `Pending` is undefined in the flow. No point in the customer journey produces a `Pending` order. Is `Pending` the pre-payment state at confirm time? Then it duplicates `Waiting Payment`.
- DESIGN omits `Archived` and `Pending` entirely.

**Fix:** single canonical state machine, one source of truth (PRD). Recommended:

```
Draft (unconfirmed cart, not yet an order)
  → Waiting Payment (customer confirmed, awaiting barista payment)
  → Paid (payment captured)
  → Preparing
  → Ready
  → Completed
  → Archived (retention/close-out)
+ Cancelled (new — see §2.3)
+ Voided (new — see §2.4)
```

## 1.2 Terminology: "Waiting Payment" vs "Accept Payment"

Same state, two names. Unify on one. PRD wins.

## 1.3 Typography rule vs receipts

DESIGN says "Never use ALL CAPS." Receipt headers/footers are configurable text printed on thermal paper. Does the rule apply to print? Clarify: screen-only rule, or normalize receipt text too.

## 1.4 "Avoid strong contrast" vs "High enough contrast"

DESIGN says avoid strong contrast AND requires readable contrast. These conflict without a floor. Palette check (WCAG AA, small text, on-surface #FFFFFF and background #F8F5EF):

| Token | Hex | Contrast on #FFFFFF | Contrast on #F8F5EF | AA pass (4.5:1)? |
|---|---|---|---|---|
| Primary Forest Green | #254F3D | ~11.0 | ~10.4 | ✅ |
| Secondary Coffee Brown | #6F4E37 | ~7.1 | ~6.6 | ✅ |
| Accent Leaf Green | #7BA66A | ~3.1 | ~2.8 | ❌ |
| Warning Warm Amber | #D9A441 | ~2.4 | ~2.1 | ❌ |
| Error Muted Red | #C45A5A | ~3.9 | ~3.5 | ❌ |
| Success Natural Green | #4CAF50 | ~3.3 | ~3.0 | ❌ |
| Text Primary | #2D2D2D | ~16.6 | ~15.5 | ✅ |
| Text Secondary | #6D6D6D | ~5.3 | ~4.9 | ✅ |

**Fix:** declare accent/warning/error/success as *non-text* colors (icons, borders, badges at large size) or darken them. Add a rule: text must be Text Primary/Secondary; semantic colors reserved for surfaces/icons. Also note: #4CAF50 is literally the Material Design green the doc says to avoid — swap for a desaturated natural green.

## 1.5 Dark theme has no values

DESIGN: "Dark Green, Dark Brown, Warm Gray, avoid pure black." No hexes. Implementer will guess. **Fix:** add dark palette tokens (background, surface, text, primary/secondary variants) to DESIGN.md.

## 1.6 Barista "2 taps" vs feature depth

DESIGN: "Everything within 2 taps" + navigation depth max 3. But "Manage products" = list → edit → modifier editor (3+ levels). These goals conflict. **Fix:** scope the 2-tap rule to the *order-taking core flow* (add item → pay), not management screens. State it explicitly.

## 1.7 Customer "Search products"

PRD §9 Ordering gives customers search. DESIGN customer mode = large photos, minimal text, 60-second ordering target. Typing on a tablet keyboard contradicts both. **Fix:** decide — customer search optional/deferred, barista search required. Recommend: customer gets category filter + scroll, no keyboard search in v1.

---

# 2. Missing Requirements

## 2.1 Role enforcement (critical)

PRD: "Barista Experience accessible only by staff." No mechanism defined. No owner/barista distinction, no PIN, no auth. Shared tablet = anyone can tap into settings/reports.

**Add:** barista PIN (4–6 digits, configurable, changeable), owner-level lock for Settings/Reports/Backup. No accounts needed (offline-first, v1 constraints exclude accounts) — PIN is enough.

## 2.2 Cash handling math

"Cash" payment listed but no change calculation, no tendered-amount input, no denominations. This is the single most common POS interaction.

**Add:** tender screen — amount due, quick-tender buttons (exact, 50k, 100k…), change due, confirmation.

## 2.3 Order cancellation (critical)

Undefined: customer abandons order mid-flow, barista cancels, customer walks away while tablet is in customer mode.

**Add:**
- Abandoned-order timeout: idle in customer mode for N minutes → auto-return to home, discard draft (with confirmation).
- `Cancelled` status for orders voided before payment.
- Cancel path in barista queue (with reason, see §2.8).

## 2.4 Void/refund (critical)

Undefined: paid order must be voided (wrong order, customer refund). Without this, no accurate reports, no cash reconciliation.

**Add:** `Voided` status + refund flow for paid orders. Void requires barista PIN + reason. Voided orders excluded from revenue but counted in reports as voided amount.

## 2.5 Payment failure / partial payment

Undefined: card declined (offline card?), customer short on cash, split payment (marked "optional" — decide, don't defer the schema).

**Add:** per-payment records (not a single payment field) so split/partial/multiple attempts are representable from day one. Offline constraint: QRIS static QR works offline (customer pays via their phone app); dynamic QRIS and card terminals typically need network — document that v1 card = manual entry confirmation, or exclude card. Decide explicitly.

## 2.6 Tax & service charge computation

Settings list Tax and Service Charge but no rules: percentage? fixed? applied to subtotal or subtotal+tax? rounding? inclusive/exclusive? displayed on receipt? per-item or per-order?

**Add:** computation order (e.g. subtotal → service charge → tax on subtotal+SC), rounding rule (IDR = whole rupiah, round half-up), receipt display, toggle per order? Keep global setting, not per-order.

## 2.7 Money representation (critical, do not defer)

Nowhere specified. Float math for money = rounding bugs. **Add architectural rule:** store all money as integer minor units (IDR has no decimals → integer rupiah; tax/SC fractions round once at the end). Never float.

## 2.8 Audit trail

Shared device, no user identity, stock adjustments and voids possible → untraceable changes.

**Add:** minimal audit log (timestamp, action, entity, delta, reason/note, PIN-user label). Needed for: stock adjustment reasons, voids, price changes. Owner-facing, kept local.

## 2.9 Receipt/printing flow

Printer Configuration in settings, but no print triggers. 

**Add:** print on payment (customer copy) + reprint; printer type (ESC/POS thermal, 58mm vs 80mm, Bluetooth/USB/network); paper size; logo printing yes/no; auto-print toggle. Receipt content: order number, items, modifiers, notes, totals, tax/SC breakdown, header/footer, timestamp.

## 2.10 Order numbering

Undefined. Needed for receipts, pickup, audit. **Add:** sequential order number per day (e.g. `#001` resets daily) + persistent global ID internally. Distinguish internal ID (never shown to customer) from order number (shown).

## 2.11 Name privacy / pickup identification

Customer name entered — shown on barista queue. What if customer refuses a name? DESIGN says customers never see IDs, but pickup needs *something*.

**Add:** name optional? fallback = order number on the "hand back" screen. Decide: name required or order-number fallback. Also trim/validate name (length cap, no control chars).

## 2.12 Inventory edges (critical)

- Recipe references a deleted ingredient → block delete if referenced, or cascade with warning.
- Negative stock policy: block sale / allow with warning / soft-block only if `availability` flag off. Decide.
- Fractional units: shots, grams vs whole units. Units table needed.
- No ingredient cost → "Estimated Profit" (PRD §9 Reports) cannot be true profit. **Fix:** add cost per unit on ingredients; profit = revenue − (COGS + expenses). Without cost, rename to "Estimated Gross" and say so.
- Low-stock surfacing: min-stock exists but no trigger defined. **Add:** low-stock list + badge on inventory screen; block customer ordering of sold-out items (hide or "sold out" state on menu).
- Stock adjustment reason field (ties to §2.8).

## 2.13 Reports definitions

"Daily/Weekly/Monthly Sales, Best Selling, Revenue, Expenses, Estimated Profit" — no metric definitions.

**Add:** what counts as revenue (exclude voided, include tax? net vs gross), reporting-day boundary (midnight? store close time?), cash vs card split in reports, X/Z or end-of-day close flow (cash drawer reconciliation, banking). Add: end-of-day summary screen.

## 2.14 Onboarding / first launch

Empty menu on first launch → customer mode shows nothing, barista has nothing to sell.

**Add:** first-run setup (store name, tax/SC defaults, printer, theme) + seed/demo data option + sample product images. Also: welcome screen in DESIGN journey exists but no screen spec.

## 2.15 Order editing after payment

Common café flow: drink ordered, customer adds another at pickup. Paid → Preparing orders: can barista append items? Undefined.

**Add:** decide — v1: no edits after payment; new order instead. Document it. (Simplest, consistent with constraints.)

## 2.16 Abandoned/unpaid order recovery

App killed mid-payment → orphan order stuck in Waiting Payment forever.

**Add:** startup scan: orders in `Waiting Payment` older than N minutes → flag for barista (recover or cancel). Also transactional writes: order + payments + stock deduction in one SQLite transaction (see §3).

## 2.17 Double-tap / duplicate submissions

Confirm button double-tap → duplicate order; Start New Order double-tap → two drafts.

**Add:** idempotency — disable button during commit, unique draft marker, guard in DB write.

## 2.18 Localization

QRIS implies Indonesia. Language(s) undefined: Indonesian? English? both? Customer-facing copy (warm language in DESIGN) must be authored in the chosen language(s).

**Add:** decide language; if both, string-resource strategy from day one.

## 2.19 Clock/time

Offline device: reports keyed to device clock. Wrong clock = wrong reports. **Add:** document that device time is trusted (offline constraint), consider warning if clock is obviously off; store timestamps UTC + local offset, or local time only. Keep simple: local time, one store.

## 2.20 Product image management

Product images required (DESIGN: photography critical) but no mechanics: source (camera/gallery), storage (SQLite BLOB vs file paths — backup implications, §3.2), size limits, downscaling, placeholder for imageless products.

**Add:** file-system storage with relative paths, downscale to ~1–2 MB max, include images in backup, placeholder illustration (leaf/coffee) for missing images.

## 2.21 Modifiers spec

PRD lists "Modifiers" and "Customize drinks" — no structure. **Add:** modifier groups (milk type, size, sweetness), required/optional, min/max selections, price delta per option, per-item note. Cap selections to keep customization ≤ 60 s.

## 2.22 Availability semantics

Product "Availability" flag exists. Define: hide from customer menu, or show as sold out? Does it block barista orders? Combine with stock (auto sold-out when below threshold?) — decide: manual flag only in v1, stock blocking v2.

## 2.23 Success criteria measurability

Criteria like "ordering under 60 s" — no measurement mechanism. Fine as goals, but add acceptance definition for at least: no lost transactions (restart test), backup/restore test, offline test. Add a test strategy (§3.6).

---

# 3. Architectural Risks

## 3.1 Money as float

See §2.7. Highest-severity bug class in POS software. Non-negotiable: integer minor units.

## 3.2 Images vs SQLite backup

If images are BLOBs: DB grows huge, backup/restore slow, in-memory pressure. If file paths: backup must bundle files, restore must re-link paths. PRD says "Export/Import Database" — ambiguous about images.

**Add:** define backup unit = DB + images directory (single archive file, e.g. ZIP). Atomic: use SQLite `VACUUM INTO` / backup API, never raw file copy of a live DB (corruption risk — PRD demands "prevent data corruption" but no mechanism stated).

## 3.3 Schema versioning & migrations

No mention. v1 → v3 roadmap implies schema changes. **Add:** `PRAGMA user_version` + migration chain; backup/restore must reject or migrate older-version files (restore a v1 backup into v2 app).

## 3.4 Single-writer concurrency & transactionality

One device, one DB — but order commit touches orders + payments + inventory + audit in one logical step. **Add:** single `beginTransaction`/`commit` (Room `@Transaction`), WAL mode, foreign keys ON. Idempotent commits.

## 3.5 UUIDs from day one (scalability)

v3 = multi-device + cloud sync. If v1 uses `AUTOINCREMENT` ints, every table gets re-keyed later. **Add:** UUID primary keys + `createdAt`/`updatedAt` timestamps + soft-delete flags now. Cheap now, expensive later. Auto-increment ints can remain as display sequence numbers (order numbers).

## 3.6 Testing strategy

Zero test requirements in PRD. Offline POS with money + inventory = testable logic. **Add:** unit tests for pricing/tax/inventory math, instrumentation tests for order flow, restart-recovery test (kills app mid-order, verifies no loss), backup/restore round-trip test. This is how success criteria get verified.

## 3.7 Dependency discipline

PRD: "Never introduce unnecessary dependencies." Recommended minimal stack: Kotlin + Jetpack Compose, Room (SQLite), Navigation, ViewModel; Coil (images); no DI framework needed (manual DI / service locator) — fewer deps, matches philosophy. State the stack in the PRD so the agent doesn't improvise.

## 3.8 Process death & rotation

Both orientations supported → rotation mid-order loses state unless saved. **Add:** ViewModel + SavedStateHandle, survive process death, restore draft cart.

## 3.9 Cold start ≤ 3 s

First launch after update: DB + image indexing on main thread kills this. **Add:** async init, splash = branded welcome (per DESIGN), lazy-load menu images (Coil placeholders).

---

# 4. UX Issues

1. **Name privacy** — customer name shown to barista queue; some customers won't share. Order-number fallback (§2.11).
2. **"Hand back the tablet" screen** — should show order summary + number so barista picks it up without hunting. Currently flow ends on a dead end.
3. **Empty states everywhere** — DESIGN gives one example; inventory, reports, queue, search all need warm empty states (or the "broken" feel the DESIGN forbids).
4. **Product without image** — placeholder design missing.
5. **Long category lists / many products** — no pattern for grouping (sticky headers, horizontal category chips). Customer mode needs large photos; decide grid vs list.
6. **Dark mode tokens** (§1.5) — "evening inside the café" needs concrete values, else implementer ships generic dark.
7. **Keyboard on tablet** — customer typing name = friction vs 60 s target. Consider optional name, or quick-name suggestions? Keep: simple text field + "skip" = order number.
8. **Receipt language/copy** — header/footer free text; default content unspecified.

---

# 5. Scalability Concerns

| Concern | Impact | Mitigation |
|---|---|---|
| Int primary keys | v3 sync/multi-device re-key | UUIDs now (§3.5) |
| Reports over years of data | Slow queries, no indexes spec | Index on order date/status; keep queries date-ranged |
| Image bloat | App size, backup size | Downscale, filesystem storage, archive pruning |
| DB growth (Archived) | Backup slow | Retention policy: archive-to-file after N months? Define |
| Single-device assumption | Correct for v1 | Keep schema sync-ready (§3.5), don't build device_id logic yet |

---

# 6. Decision Points (must resolve before implementation)

Ordered by impact:

1. **State machine** — canonical statuses incl. Cancelled/Voided (§1.1, §2.3, §2.4).
2. **Money + tax/SC math** — integer units, computation order, rounding (§2.6, §2.7).
3. **Payments model** — per-payment records; split in or out; card offline semantics (§2.5).
4. **Role enforcement** — PIN levels (§2.1).
5. **Ingredient cost / profit definition** — real profit or rename (§2.12).
6. **Customer search** — in or out (§1.7).
7. **Language(s)** — Indonesian/English (§2.18).
8. **Name optionality** — required vs order-number fallback (§2.11).
9. **Backup unit** — DB + images archive, atomic backup API (§3.2).
10. **UUID vs int keys** — UUID recommended (§3.5).
11. **Edit-after-payment** — no in v1 (§2.15).
12. **Post-payment add-ons** — new order only (§2.15).
13. **Dark palette values** (§1.5).
14. **Receipt default content** — template in docs.

---

# 7. Recommended Doc Updates (before code)

1. PRD: add canonical status state machine (§9 Order Management).
2. PRD: add Role Enforcement section (PIN).
3. PRD: add Payments detail — per-payment records, change calculation, failure paths.
4. PRD: add Cancellation/Void/Refund requirements.
5. PRD: add Tax/Service Charge computation rules.
6. PRD: add Receipt & Printing section (format, triggers, content).
7. PRD: add Inventory rules — negative stock, units, ingredient cost, low-stock surfacing.
8. PRD: add Order Numbering.
9. PRD: add Backup unit definition + atomic restore.
10. PRD: add Data model constraints — UUID keys, timestamps, integer money.
11. PRD: add Architecture section — recommended stack, transactionality, idempotency.
12. PRD: add Testing strategy mapping to success criteria.
13. PRD: add Onboarding/first-run.
14. DESIGN: add dark theme tokens, WCAG floor, semantic-color usage rule, receipt typography exception, 2-tap scope clarification.

---

# 8. Priority Summary

**Critical (block implementation):** money as integer, payment/cash math, state machine with cancel/void, PIN enforcement, transactional order commit, UUID keys, backup atomicity.

**Important (shape UX):** tax/SC rules, receipt flow, name optionality, abandoned-order timeout, edit-after-payment decision, onboarding, low-stock/sold-out behavior, language.

**Polish (design):** dark tokens, contrast floor, empty states, image placeholders, customer search decision.

---

# 9. Post-Implementation Code Audit (v2.0)

Version: 2.0
Status: Static code review of the implemented app. No build/tests executed during review.
Scope: PRD.md v1.1, DESIGN.md v1.0, and the Kotlin source under `app/src/main`.

## 9.1 Verified strengths

- Money is integer `Long`/`BigDecimal` throughout (PRD §15). No float money math in production code; the only `Float`/`Double` uses are receipt pixel math.
- Order state machine is centralized and enforced (`OrderFlow.kt`); illegal transitions rejected and tested.
- Order commit is a single Room transaction; stock is deducted on payment and restored on void atomically.
- Backup uses `VACUUM INTO` (consistent snapshot), validates schema version, and takes a safety-net before restore.
- PINs are salted-hashed with constant-time comparison; app is offline-clean with no network/analytics SDKs.
- Decent unit coverage (money, pricing, state machine, order service, PIN, backup) + a Room migration test.

## 9.2 High severity

- **H1 — Duplicate order on double-tap "Submit order".** `OrderConfirmViewModel` generates a fresh
  `draftId` UUID on every call, so the idempotency guard in `OrderService.confirmOrder` (which also runs
  outside the transaction) cannot dedupe two rapid taps. The submit button is never disabled during the write.
- **H2 — Void and Cancel flows unreachable from the UI.** `OrderService.void`/`cancel` exist and are tested
  but no screen calls them. A `PAID` order can never be voided; a `WAITING_PAYMENT` order can never be cancelled.
- **H3 — Inventory, Expenses, Reports, Backup lacking UI.** Services/repos/entities exist but are not exposed
  via `AppGraph` and have no screens in the `NavHost`. Startup recovery and draft purge are never invoked at launch.
- **H4 — PIN gate bypassable.** `BaristaDetailViewModel` hardcodes `pinVerified = true`; the gate is only the
  session-unlock at Barista entry, which Navigation restores across process death and resets on rotation
  (`remember`, not `rememberSaveable`). No auto-lock timeout is implemented.

## 9.3 Medium

- ~~Data-model / ordering / mapping of order numbering and the idempotency check run outside the transaction
  (TOCTOU); `orderNumber` has no unique index.~~ → Resolved as part of H1 (moved into the transaction).
- ~~No `@ForeignKey` constraints anywhere~~ → Resolved: `@ForeignKey` (ON DELETE CASCADE) on every child table;
  parent tables switched from `INSERT OR REPLACE` to Room `@Upsert` so updates no longer cascade-delete children.
  DB bumped to v3 with `MIGRATION_2_3`.
- Cart is a plain in-memory ViewModel; it does not survive process death (no `SavedStateHandle`). *open*
- The confirm screen recomputes totals with `BigDecimal(subtotal * scDec)` (floating multiply) which can
  disagree with the `Pricing.calculate` path by one rupiah for non-round percentages. *open*
- ~~Customer name is interpolated raw into a Nav route~~ → Resolved: name `Uri.encode`d before `navigate`.
- ~~PINs are SHA-256~~ → Resolved: PBKDF2WithHmacSHA256, 120k iterations, 256-bit, with per-key salt + iter,
  legacy SHA-256 fallback on pre-v3 stores.
- ~~Only Cash and QRIS are exposed in the barista UI~~ → Resolved: QRIS, Debit, Credit Card, Bank Transfer now
  selectable in the detail screen (FilterChip row).
- ~~No quick-tender buttons~~ → Resolved: Exact / Rp50.000 / Rp100.000 quick-tender in the cash flow.
- The abandoned-draft idle timeout and barista auto-lock are not implemented. *open*
- ~~Receipt … writes `US_ASCII`~~ → Resolved: receipt writes UTF-8. Header/footer config still unused. *partly open*
- ~~Backup restore does not sanitize ZIP entry names~~ → Resolved: `resolveInside` zip-slip guard + regression test.
- `BackupService.restore` closes and reopens the singleton database in place without resetting `AppGraph`. *open*

## 9.4 Low / Polish

- Light palette drifts from DESIGN's Warm Cream to pure white; greener brand hues than the spec.
- Release build leaves `isMinifyEnabled = false`.
- Stray `c/` directory (Gradle cache copy) and `.kotlin/` in the workspace root; not yet a git repo.
- Dead `OrderConfirmViewModel` instance created in the NAME route.
- COGS computed at confirm time (comment says payment time) with per-line N+1 recipe queries.

## 9.5 Fix plan (high-severity)

1. **H1** — stable persisted draft marker; move idempotency check + order-number allocation inside the
   transaction; disable the submit button during the write.
2. **H4** — require the Barista PIN at payment time in `BaristaDetailViewModel`/screen; don't trust the
   session flag alone; make `baristaUnlocked` config-change-safe.
3. **H2** — expose Cancel (WAITING_PAYMENT) and Void (PAID) actions with reason and PIN in the detail screen.
4. **H3** — expose Inventory, Expenses, Reports, and Backup/Restore screens; wire startup recovery scan.

### Fix status (implemented)

| ID | Resolution | Where |
|---|---|---|
| H1 | Idempotency guard + order-number allocation moved inside the Room transaction; a single stable `draftId` per confirm flow; submit button disabled while writing. Concurrency regression test added. | `OrderService.kt`, `OrderConfirmViewModel.kt`, `OrderConfirmScreen.kt`, `OrderServiceTest.kt` |
| H4 | Barista PIN verified in `BaristaDetailViewModel` at the moment of payment and void (honours the owner's daily disable flag); `baristaUnlocked` now `rememberSaveable` so rotation does not re-lock. | `BaristaDetailViewModel.kt`, `RagnalaApp.kt` |
| H2 | Cancel (WAITING_PAYMENT, reason required) and Void (PAID, PIN + reason) added to the detail screen with dialogs. | `BaristaDetailScreen.kt` |
| H3 | New `InventoryScreen`, `ExpenseScreen`, `ReportsScreen`, `BackupScreen` (+ ViewModels), `ReportsService`, DAO projection queries, `AppGraph` wiring, management menu entries, and startup recovery scan (flags stale WAITING_PAYMENT, purges abandoned drafts). | `ui/management/*`, `service/ReportsService.kt`, `Daos.kt`, `Entities.kt`, `RagnalaApp.kt`, `ManagementScreen.kt` |
| M2 | `@ForeignKey` (ON DELETE CASCADE) added to `modifier_options`, `product_modifier_groups`, `recipe_items`, `order_items`, `order_item_modifiers`, `payments`; parent upserts converted to Room `@Upsert` (the generated `INSERT … ON CONFLICT DO UPDATE` avoids the DELETE+INSERT that REPLACE uses, which otherwise cascade-deleted children on every order/product/ingredient update). DB `version = 3` + `MIGRATION_2_3`; schema `3.json` regenerated. | `Entities.kt`, `Daos.kt`, `RagnalaDatabase.kt` |
| M5 | Customer name encoded with `android.net.Uri.encode(name.trim())` before building the confirm route. | `RagnalaApp.kt` |
| M6 | `PinService.hash` → PBKDF2WithHmacSHA256 (`ITERATIONS = 120_000`, 256-bit, 64 hex); stores `{key}_salt` + `{key}_iter`; `verify` uses stored iter and falls back to legacy SHA-256 when no `_iter` (pre-v3 compat). | `PinService.kt`, `PinServiceTest.kt` |
| M7 | Non-cash methods exposed: QRIS, Debit, Credit Card, Bank Transfer via FilterChip row + `paymentMethodLabel`. | `BaristaDetailScreen.kt` |
| M8 | Quick-tender buttons (Exact / Rp50.000 / Rp100.000, filtered to ≥ total) added to the cash flow. | `BaristaDetailScreen.kt` |
| M10 | Receipt printer writes UTF-8 (all `US_ASCII` → `UTF_8`) so Indonesian text does not garble. | `BluetoothReceiptPrinter.kt` |
| M11 | Backup extract hardened with `resolveInside` guard (normalizes `/`, rejects empty/`..`/`../`, canonical-prefix check) + regression test for a zip-slip image entry. | `BackupService.kt`, `BackupServiceTest.kt` |

Verification: `:app:assembleDebug` clean; `:app:testDebugUnitTest` — 69 tests, 0 failures (incl. new M11 zip-slip
and M6 PBKDF2/legacy tests, and a defense-in-depth test that `payOrderCash(pinVerified=false)` still rejects at
the service layer). DB v3 schema validated by `3.json` export; app installed and running on the AVD (schema
migrated v2→v3 via `MIGRATION_2_3`). Still open (medium/low): cart `SavedStateHandle` survival, confirm-screen
floating `scDec` recompute, idle-timeout + barista auto-lock, `BackupService.restore` AppGraph reset, receipt
header/footer config, release minify, palette drift.
