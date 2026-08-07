# Ragnala POS — DESIGN.md

Version: 1.0

---

# Design Philosophy

Ragnala POS is **not a traditional Point of Sale application**.

It is an extension of the Ragnala Coffee & Botanee experience.

Every interaction should feel calm, natural, premium, and effortless.

The application should disappear into the experience, allowing customers to focus on coffee, food, and nature—not technology.

The design language should evoke the feeling of sitting inside Ragnala itself.

---

# Core Design Principles

## Nature First

Every visual element should be inspired by nature.

Think of:

- Wood
- Stone
- Leaves
- Coffee beans
- Soil
- Water
- Natural sunlight

Avoid synthetic-looking interfaces.

---

## Slow Living

The interface should never feel rushed.

Interactions should feel intentional.

Animations should be soft.

Typography should breathe.

Whitespace should be generous.

---

## Minimalism

Remove everything that isn't necessary.

Every button should have a purpose.

Every screen should answer one question.

Less clutter.

More clarity.

---

## Premium Experience

Premium doesn't mean luxury.

Premium means thoughtful.

Beautiful spacing.

High quality imagery.

Balanced typography.

Simple interactions.

---

## Customer First

Customers should never feel like they're using cashier software.

They should feel like they're browsing a beautiful café menu.

---

# Emotional Goals

The application should make users feel:

- Relaxed
- Comfortable
- Curious
- Welcome
- Calm
- Confident

Avoid making users feel:

- Stressed
- Confused
- Rushed
- Overwhelmed

---

# Visual Keywords

Nature

Coffee

Wood

Leaves

Organic

Calm

Minimal

Premium

Warm

Soft

Quiet

Fresh

Earth

Botanical

Slow Living

---

# Color Palette

## Primary

Forest Green

Purpose:
Brand identity

Approximation:

#254F3D

---

## Secondary

Coffee Brown

Purpose:
Buttons
Highlights

Approximation:

#6F4E37

---

## Background

Warm Cream

Approximation

#F8F5EF

---

## Surface

Soft White

#FFFFFF

---

## Accent

Leaf Green

#7BA66A

---

## Success

Natural Green

#4CAF50

---

## Warning

Warm Amber

#D9A441

---

## Error

Muted Red

#C45A5A

---

## Text Primary

#2D2D2D

---

## Text Secondary

#6D6D6D

---

## Color Usage Rules

Semantic colors (Accent, Success, Warning, Error) are for non-text surfaces: icons, badges, borders, large filled elements. They do NOT meet WCAG AA contrast for small text on the declared backgrounds (Leaf Green #7BA66A ≈ 3.1:1, Amber #D9A441 ≈ 2.4:1, Muted Red #C45A5A ≈ 3.9:1, Natural Green #4CAF50 ≈ 3.3:1 on white).

All text uses Text Primary / Text Secondary only. Any text drawn in a semantic color must be large (≥ 18pt / 24px) or bold (≥ 14pt / 18.7px).

Do not use #4CAF50 — it is the stock Material Design green this document otherwise rejects. Use a desaturated natural green instead (e.g. #6A9A5B) when a success surface is needed.

# Design Style

Avoid strong contrast.

Avoid neon colors.

Avoid gradients unless extremely subtle.

Prefer flat colors with soft shadows.

---

# Typography

Use a modern sans-serif font.

Recommended:

- Inter
- Plus Jakarta Sans
- Manrope

Headings

Large

Elegant

Readable

Body

Simple

Friendly

Button text

Medium weight

Never use ALL CAPS.

Screen-only rule: receipt text is printed as configured and is exempt.

---

# Border Radius

Cards

20px

Buttons

16px

Bottom Sheets

28px

Dialogs

24px

Images

20px

---

# Shadows

Very soft.

Almost invisible.

Example

Y: 6

Blur: 18

Opacity: 10%

Avoid Material Design heavy elevation.

---

# Spacing System

Use an 8pt grid.

Spacing:

4

8

16

24

32

40

48

64

Never place elements too close together.

Whitespace is part of the design.

---

# Icons

Use outline icons.

Rounded style.

Recommended:

Material Symbols Rounded

or

Phosphor Icons

Avoid filled icons unless necessary.

---

# Photography

Photography is extremely important.

Menu photos should be:

Large

Professional

Warm lighting

Real

Natural

No stock photos.

Food should always look handcrafted.

---

# Customer Mode

Goal:

Feel like browsing a premium café menu.

Characteristics:

Large photos

Large buttons

Minimal text

Friendly wording

No technical information

No database-like layouts

The customer should never see:

Settings

Reports

Inventory

Transactions

IDs

Internal status

---

# Barista Mode

Goal:

Fast operation.

Low cognitive load.

Everything within 2 taps.

Scope: the 2-tap rule applies to the order-taking core flow (add item → pay). Management screens (products, inventory, reports, settings) may exceed it.

Characteristics:

Clear information hierarchy.

Readable numbers.

Large touch targets.

Efficient workflow.

---

# Motion Design

Animations should feel natural.

Recommended duration:

150–250 ms

Preferred animations:

Fade

Scale

Slide

Avoid:

Bounce

Elastic

Flash

Excessive motion

---

# Buttons

Primary

Filled

Rounded

Large

Secondary

Outlined

Soft colors

Danger

Muted red

Never use aggressive colors.

---

# Cards

Cards should feel like printed menu sections.

Soft shadows.

Rounded corners.

Lots of padding.

Never look like spreadsheets.

---

# Bottom Sheets

Preferred over dialogs.

Feels more modern.

Large rounded corners.

Easy to dismiss.

---

# Navigation

Simple.

Predictable.

No deep nesting.

Maximum navigation depth:

3

---

# Customer Language

Use warm, human language.

Instead of:

Checkout

Use:

Review Order

Instead of:

Submit

Use:

Confirm Order

Instead of:

Waiting

Use:

We're preparing your order.

---

# Empty States

Should never feel broken.

Example:

"No orders yet."

↓

"Your next coffee adventure starts here."

---

# Error Messages

Never blame the user.

Bad:

Invalid Input

Better:

Please enter your name.

---

# Sound Design

Optional.

Soft click sounds.

Receipt sound.

No loud notifications.

---

# Accessibility

Minimum touch target:

48x48 dp

Readable fonts.

High enough contrast.

Support dark mode.

---

# Dark Theme

Dark mode should feel like evening inside the café.

Dark Green

Dark Brown

Warm Gray

Avoid pure black.

Reference values (no pure black):

- Dark background: #141A17 (deep forest, never #000000)
- Dark surface: #1C2420
- Dark surface raised: #24302A
- Dark text primary: #EDE8E0 (warm off-white)
- Dark text secondary: #A8A29A
- Dark accent: #8FB58A (leaf green raised for dark surfaces)
- Dark divider: #2E3A34

Success / Warning / Error surfaces in dark mode follow the same rule as light: non-text only.

---

# Customer Journey

Welcome

↓

Browse Menu

↓

Customize

↓

Review Order

↓

Enter Name

↓

Confirm

↓

"Please hand the tablet back to the barista."

---

# Barista Journey

Receive Order

↓

Accept Payment

↓

Prepare

↓

Ready

↓

Completed

↓

Ready for Next Customer

---

# UI Inspirations

Apple Store App

Notion

Linear

Aesop

Blue Bottle Coffee

Starbucks Reserve

Muji

Minimalist Japanese Design

Organic Scandinavian Design

---

# Things to Avoid

❌ Bright blue interfaces

❌ Heavy Material Design

❌ Neon colors

❌ Complex dashboards

❌ Tiny buttons

❌ Spreadsheet layouts

❌ Crowded screens

❌ Excessive animations

❌ Generic POS appearance

---

# Ragnala Design Language (RDL)

Every screen should answer this question:

"Does this feel like sitting inside Ragnala Coffee & Botanee?"

If the answer is no,

redesign it.

Technology should be invisible.

The experience should always come first.
