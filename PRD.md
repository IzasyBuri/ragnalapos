# Ragnala POS
## Product Requirements Document (PRD)

Version: 1.1

Status: Draft

Author: Project Owner

Revision 1.1: amendments per REVIEW.md v1.0. All decisions recorded in §18 Decision Log.

---

# 1. Product Overview

## Product Name

Ragnala POS

## Tagline

An offline-first Point of Sale application designed exclusively for Ragnala Coffee & Botanee.

## Description

Ragnala POS is an Android application that combines a modern Point of Sale system with a customer self-ordering experience. Unlike traditional POS systems, Ragnala POS is designed to become part of the café experience rather than simply acting as a cashier application.

The application operates entirely offline using a local SQLite database. No internet connection, cloud services, or subscriptions are required for the core functionality.

The same device can seamlessly switch between Customer Mode and Barista Mode, allowing a single tablet to serve both customers and staff.

---

# 2. Vision

Build a beautiful, reliable, offline-first POS that reflects the atmosphere and philosophy of Ragnala Coffee & Botanee while simplifying daily operations for baristas and creating an enjoyable ordering experience for customers.

---

# 3. Objectives

The application should:

- Eliminate handwritten ordering.
- Reduce ordering mistakes.
- Speed up payment processing.
- Simplify inventory tracking.
- Generate daily sales reports.
- Operate entirely offline.
- Be simple enough that new employees require minimal training.

---

# 4. Target Platform

Primary Platform

Android Tablet

Secondary Platform

Android Phone

Minimum Android Version

Android 10

Landscape Support

Yes

Portrait Support

Yes

Offline Support

Required

Cloud Required

No

---

# 5. Target Users

## Owner

Responsibilities

- View reports
- Manage products
- Manage inventory
- Configure application

---

## Barista

Responsibilities

- Receive orders
- Process payment
- Prepare drinks
- Complete orders

---

## Customer

Responsibilities

- Browse menu
- Customize items
- Add products to cart
- Enter customer name
- Confirm order

No customer account is required.

---

# 6. Core Experience

The application contains two primary experiences.

## Customer Experience

Designed to feel like browsing a premium café menu.

Accessible only after the barista starts a new order.

Focuses entirely on ordering.

---

## Barista Experience

Designed for speed and operational efficiency.

Accessible only by staff.

Contains all management features.

---

# 7. Product Philosophy

The application should never feel like supermarket cashier software.

It should feel like a digital extension of the café itself.

Design decisions must prioritize:

- simplicity
- elegance
- clarity
- warmth
- calm interactions

---

# 8. Customer Ordering Flow

Customer arrives

↓

Barista taps

Start New Order

↓

Tablet is handed to customer

↓

Customer browses menu

↓

Customer customizes products

↓

Customer enters name

↓

Customer confirms order

↓

Screen displays

"Please hand the tablet back to the barista."

↓

Barista processes payment

↓

Order enters preparation queue

↓

Drink completed

↓

Order completed

---

# 9. Functional Requirements

## Product Management

The application shall support:

- Categories
- Products
- Product Images
- Descriptions
- Prices
- Availability
- Modifiers
- Recipes

Modifier structure: modifier groups (e.g. milk type, size, sweetness) with optional/required selection, min/max selection count, and optional price delta per option. Per-item free-text notes supported. Selection counts capped per group to keep customization within the 60-second ordering target.

## Ordering

Customers shall be able to:

- Browse menu
- Search products (barista mode; customer search deferred to v2 — see §18 D1)
- Filter by category
- Customize drinks
- Add notes
- Change quantity
- Remove items
- Review cart
- Confirm order

## Payments

The application shall support:

- Cash
- QRIS
- Debit
- Credit Card
- Bank Transfer

Payments are recorded as individual payment records per order, so multiple payments against one order are representable from day one. The v1 UI uses one payment method per order; the split-payment UI is deferred to v2 (data model ready — §18 D2).

Cash flow:

- Tender screen shows amount due.
- Quick-tender buttons (exact, 50,000, 100,000, custom amount).
- Change due calculated and shown; confirmed before order proceeds.

QRIS: static QR code displayed on the tablet. Works offline — customer pays via their phone banking app. Barista manually confirms receipt. No dynamic QR generation in v1.

Card and Bank Transfer: offline manual confirmation flow in v1. No network terminal integration (§18 D3).

Payment failure: a failed or unconfirmed attempt does not change order state. Attempts are logged. No order moves to Paid until a payment is confirmed.

## Order Management

Canonical state machine (single source of truth — replaces the ambiguous Pending flow):

```
Draft            — cart being built in customer mode; not yet an order
  ↓ Customer confirms
Waiting Payment  — order created; awaiting barista payment
  ↓ Payment confirmed
Paid             — payment captured
  ↓ Barista starts preparation
Preparing        — drinks being made
  ↓
Ready            — ready for pickup
  ↓
Completed        — handed to customer
  ↓
Archived         — retained for reporting; no longer active
```

Additional terminal states:

- Cancelled — order voided before payment (customer walked away, barista cancels). Reason required. Excluded from revenue.
- Voided — paid order refunded/voided. Requires barista PIN and a reason (§9 Role Enforcement). Excluded from revenue; reported as voided amount.

Rules:

- Valid transitions: Draft → Waiting Payment; Waiting Payment → Paid or Cancelled; Paid → Preparing → Ready → Completed → Archived; Paid → Voided.
- No order edits after payment. Additional items = new order (§18 D5).
- Abandoned drafts: idle timeout in customer mode (configurable, default 10 minutes) returns the device to the home screen and discards the draft after a confirmation prompt.
- Startup recovery: on launch, orders stuck in Waiting Payment longer than N minutes (configurable, default 15) are flagged for the barista to recover or cancel.

## Inventory

Inventory shall support:

- Ingredients
- Current Stock
- Minimum Stock
- Stock Adjustment
- Automatic Stock Reduction
- Manual Restocking

Rules:

- Stock adjustment and restocking require a reason (recorded in audit log — §9 Role Enforcement).
- Measurement units: unit table (ml, g, pcs, shot, scoop). Fractional quantities allowed.
- Negative stock is allowed with a visible low-stock warning. Automatic stock-based sales blocking is deferred to v2 (§18 D10).
- Product availability is a manual flag in v1. Unavailable products are hidden from the customer menu; barista mode shows them marked unavailable.
- Low stock (at or below minimum) surfaces as a badge and a list on the inventory screen.

## Recipes

Each menu item may contain:

Ingredients

Quantity

Measurement Unit

These values are used for automatic stock deduction.

Each ingredient also carries a cost per unit (inventory cost). This enables true cost-of-goods reporting (§18 D11). Deleting an ingredient referenced by a recipe is blocked until the reference is removed.

## Expenses

Support:

Daily expenses

Expense category

Amount

Notes

Date

## Reports

Generate:

Daily Sales

Weekly Sales

Monthly Sales

Best Selling Products

Revenue

Expenses

Estimated Profit

Metric definitions:

- Revenue = sum of confirmed payments on Paid/Completed/Archived orders, including tax and service charge, excluding Cancelled and Voided orders.
- Profit = Revenue − Cost of Goods Sold − Expenses.
- Cost of Goods Sold = sum of ingredient cost consumed via recipes on sold orders.
- Reporting day boundary: local midnight by default; configurable store close time.

Additional report features:

- Payment-method breakdown (cash vs QRIS vs card vs transfer).
- Voided and cancelled amounts shown separately.
- End-of-day summary screen: day totals, cash drawer count, voids, low-stock list.

## Backup

Support:

Export Database

Import Database

Manual Backup

Manual Restore

Backup unit: a single archive (ZIP) containing the SQLite database and the product image directory (§18 D9).

- Backups are created with the SQLite backup API / VACUUM INTO — a consistent snapshot. Never a raw file copy of a live database.
- Restore validates the schema version: older backups are migrated, newer versions are rejected.
- Restore is destructive: it prompts for confirmation and automatically creates a backup of the current state first.
- Manual backup/restore only in v1.

## Order Numbering

- Internal order ID: UUID (never shown to customers).
- Order number: sequential per day, resets daily (#001, #002…). Shown on receipts, the "hand back" screen, and the barista queue.

## Receipts & Printing

- Triggers: auto-print on payment confirmation (toggleable), manual reprint from order history.
- Receipt content: store header/footer (from Settings), order number, timestamp, items with modifiers and notes, subtotal, service charge, tax, total, payment method, change due.
- Printer: ESC/POS thermal. Paper width configurable (58mm / 80mm). Connection: Bluetooth or USB. Logo print toggle.
- Receipt text is printed as configured; the no-ALL-CAPS rule applies to screens, not print (§18 D14, DESIGN.md).

## Settings

Application Settings

Store Information

Tax

Service Charge

Receipt Header

Receipt Footer

Printer Configuration

Theme

Tax and Service Charge are percentage-based, applied globally (not per order):

- Subtotal → service charge on subtotal → tax on (subtotal + service charge).
- Rounding: to whole rupiah, half-up, applied once at the total. No intermediate rounding.
- Both shown as line items on the receipt.

## Role Enforcement

No user accounts in v1 (per §11 constraints). Access control is PIN-based:

- Barista PIN (4–6 digits) — required to enter Barista Mode, process payment, void orders.
- Owner PIN (4–6 digits, distinct) — required for Settings, Reports, Backup/Restore, product and inventory management, stock adjustments. Default set during first-run setup (§17), changeable with the current PIN.
- Barista Mode auto-locks after inactivity (configurable, default 5 minutes).
- Customer Mode always returns to the home screen after an order completes.
- Audit log (local): timestamp, action, entity, delta, reason, PIN-user label. Records: stock adjustments, voids, cancellations, price changes, PIN changes, restores.

---

# 10. Non Functional Requirements

The application must:

Work completely offline

Open within three seconds

Be responsive

Support dark mode

Be touch-friendly

Require minimal training

Store all data locally

Prevent data corruption

Support backup and restore

Survive process death and app restart without losing transactions

Prevent duplicate order submission (idempotent commits, button disabled during write)

---

# 11. Constraints

Version 1 must NOT include:

Cloud Sync

Multi Device

Online Ordering

Customer Accounts

Loyalty Program

Membership

Kitchen Display

Multi Branch

Employee Scheduling

Push Notifications

Order editing after payment (v1: additional items require a new order)

Customer-facing product search (v1: barista search only)

Dynamic QR generation

Network terminal integration for cards

---

# 12. Future Roadmap

## Version 2

QR Ordering

Loyalty

Customer Queue Display

Kitchen Display

Discount Campaigns

Table Service

Customer product search

Split-payment UI

Automatic stock-based sales blocking

---

## Version 3

Cloud Synchronization

Multiple Devices

Multiple Branches

Owner Dashboard

Remote Reports

Analytics

---

# 13. Success Criteria

The project is considered successful if:

Customers can complete an order without assistance.

Average ordering time is under 60 seconds.

Average payment time is under 30 seconds.

The application runs without internet.

No transaction is lost after restarting the application.

Inventory updates automatically after each completed order.

Backup and restore work reliably.

---

# 14. AI Coding Agent Instructions

This project will be developed collaboratively with an AI Coding Agent.

When implementing features:

- Always prioritize simplicity over complexity.
- Never introduce unnecessary dependencies.
- Maintain a modular architecture.
- Write readable and maintainable code.
- Prefer reusable components.
- Avoid premature optimization.
- Preserve backward compatibility whenever possible.
- Follow existing documentation before making implementation decisions.
- If documentation conflicts with implementation, documentation takes precedence.

The AI Coding Agent should treat this PRD as the primary source of product requirements.

---

# 15. Data Model & Architecture

## Money

All money stored as integer minor units — for IDR, integer rupiah. Tax and service charge fractions are computed once and rounded half-up at the total. Floating-point math is forbidden for money anywhere in the codebase.

## Identifiers

- UUID primary keys for all business entities (orders, payments, products, categories, ingredients, modifiers, expenses, audit entries).
- Integer rowids may exist internally but never serve as business keys.
- All tables carry createdAt / updatedAt timestamps.
- Soft delete for products, categories, and ingredients.

## Time

Device local time is trusted (offline constraint, single store). No timezone logic in v1. Timestamps stored as local wall time.

## Database

- Room (SQLite). WAL mode. Foreign keys ON.
- Schema versioning via PRAGMA user_version with sequential migrations.
- Restore migrates older backups, rejects newer ones (§9 Backup).
- Order commit is a single transaction: order + payments + inventory deduction + audit entries commit or roll back together.

## Images

- Stored on the filesystem under the app data directory; the database holds relative paths.
- Downscaled to ≤ 1–2 MB per image.
- Bundled into the backup archive (§9 Backup).
- Placeholder illustration (leaf/coffee motif per DESIGN.md) for products without images.

## Technology Stack (minimal dependencies)

- Kotlin
- Jetpack Compose
- Room (SQLite)
- Navigation
- ViewModel + SavedStateHandle
- Coil (image loading)
- Manual DI — no dependency injection framework

No analytics, crash-reporting, or network SDKs in v1.

## State Resilience

- Draft cart survives rotation and process death via ViewModel + SavedStateHandle.
- Commit actions are idempotent: unique draft marker prevents duplicate order creation.
- Startup recovery scan for orphaned Waiting Payment orders (§9 Order Management).

---

# 16. Testing Strategy

- Unit tests: tax/service-charge math, rounding, pricing, change calculation, inventory deduction, stock adjustment rules.
- Instrumentation tests: full customer ordering flow, payment flow, cancel and void flows, low-stock surfacing.
- Recovery test: kill the app mid-order, relaunch, verify no transaction loss (maps to §13).
- Backup test: export → restore round-trip; schema version migration test (restore older backup into newer app version).
- Offline test: complete a full order in airplane mode.

---

# 17. Onboarding & First Run

- First-run setup: store name, tax and service charge defaults, printer configuration, theme, Owner PIN, Barista PIN.
- Option to load seed/demo data with sample products and images.
- Home screen of an idle device shows the branded welcome (per DESIGN.md Customer Journey), not an empty state.

---

# 18. Decision Log

Decisions made during review (REVIEW.md v1.0) and their resolutions:

| # | Decision | Resolution | Rationale |
|---|---|---|---|
| D1 | Customer search | Deferred to v2 | Keyboard on tablet conflicts with 60 s ordering target and large-photo customer mode |
| D2 | Split payment | Data model supports multiple payments; split UI v2 | Schema cheap now, UI complexity not worth v1 |
| D3 | Card/Bank Transfer offline | Manual confirmation flow, no terminal integration | Offline constraint; card terminals typically need network |
| D4 | Customer name | Optional; fallback = order number on the hand-back screen | Some customers won't share a name; pickup still needs an identifier |
| D5 | Edits after payment | Not allowed in v1; new order instead | Simplest, consistent with constraints |
| D6 | Language | Bilingual string resources; Indonesian default, English available | QRIS context implies Indonesia; DESIGN warm copy needed in both |
| D7 | Money | Integer rupiah, round half-up once at total | Eliminates float rounding bugs — highest-severity POS failure class |
| D8 | Keys | UUID primary keys from day one | v3 multi-device sync would force re-keying int tables |
| D9 | Backup unit | ZIP archive = DB + images, atomic snapshot via SQLite backup API | Raw copies of live DBs corrupt; images are part of the data |
| D10 | Stock policy | Negative stock allowed with warning; auto-block v2 | Café workflow tolerance; keeps v1 simple |
| D11 | Profit | True profit: revenue − COGS (ingredient cost) − expenses | "Estimated Profit" without ingredient cost is fake |
| D12 | Abandoned orders | 10 min idle timeout; startup recovery scan for Waiting Payment | Prevents dead drafts and orphaned paid orders |
| D13 | Status model | Canonical state machine in §9 Order Management incl. Cancelled and Voided | PRD had 7 states, DESIGN had 5, Pending had no source — now one source of truth |
| D14 | Receipt text | Printed as configured; no-ALL-CAPS rule is screen-only | Receipts are configurable text on thermal paper |
| D15 | Access control | PIN-based roles (Barista / Owner), no accounts | Offline-first; accounts excluded by v1 constraints |
