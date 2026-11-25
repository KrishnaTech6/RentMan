# Rent Manager: database schema (v1)

Room 3 (`androidx.room3`) over SQLite with `BundledSQLiteDriver`. The tables below are Room entities in `:data` (`AddressEntity`, `RenterEntity`, and so on), mapped to the domain models by internal mapper functions. Entities hold only primitives (`Long`, `Int`, `String`), so no type converters are needed. See `ARCHITECTURE.md` §4.

**Built so far:** `address`, `renter`, `fixed_charge`, `bill` (without `sentAt`), `bill_line`, `payment`, `rent_change`, `backup_snapshot` and `app_settings` (written by the Settings screen: rate, reminder time and repeat. `lastBackupAt` is kept as it is until backup exists). Everything else below is designed but not built yet. The schema is still version 1 (see "Schema changes").

## Principles
1. **Money is `INTEGER` paise.** Electricity is `units × ratePaise`, both integers, so there is no rounding anywhere.
2. **Money history is immutable.** A bill is a snapshot of its lines, rate and readings. Changing a rent, rate or charge later never touches an old bill.
3. **Derived values are never stored.** Not pending, not units used, not a bill total, not "days late". They're computed from rows, so nothing can drift.
4. **History is never destroyed.** Foreign keys are `RESTRICT`. A renter who leaves is marked inactive, not deleted. A deleted payment is voided, so Undo and an audit trail both work.
5. **Dates are plain integers.** `LocalDate` is epoch days, `Instant` is epoch millis, and a billing month is `YYYYMM` (for example `202611`). It sorts correctly and is indexable. Enums are stored by name, and names must never be renamed.
6. **One atomic write per user action.** Bill + lines, rent increase + rent change, and restore all run in a single transaction.

## Tables

### `address`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK autogen | |
| name | TEXT NOT NULL | Unique, case-insensitive (`NOCASE`). e.g. "AIMS G-703". |
| archived | INTEGER NOT NULL DEFAULT 0 | Hidden from the picker, kept for history. |
| createdAt | INTEGER NOT NULL | epoch millis |

### `renter`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK autogen | |
| addressId | INTEGER NOT NULL | FK → address, RESTRICT. Index. |
| unit | TEXT | Flat or floor label, e.g. "Ground floor", "1st floor back". Tells renters apart when an address has more than one. |
| name | TEXT NOT NULL | |
| phone | TEXT NOT NULL | Digits with country code, e.g. `919876543210`. Goes straight into the `wa.me` link. |
| monthlyRentPaise | INTEGER NOT NULL | Current rent. Changes only through a rent change. |
| dueDay | INTEGER NOT NULL | 1–31, clamped to the month's length when used. |
| agreementStart | INTEGER NOT NULL | epoch day |
| raisePercent | INTEGER NOT NULL DEFAULT 10 | `0` means never ask. |
| raiseEveryMonths | INTEGER NOT NULL DEFAULT 11 | |
| raiseSnoozedUntil | INTEGER | `YYYYMM`. "Not now" hides the card until the next month. |
| billsElectricity | INTEGER NOT NULL | 0 means the renter pays directly and the reading step is skipped. |
| customRatePaise | INTEGER | Null means use the global rate. |
| startingReading | INTEGER | Required when `billsElectricity` is 1. |
| firstBillMonth | INTEGER NOT NULL | `YYYYMM`. The first month this renter should be billed (default: the month they're added). Unbilled months are counted from here, not from `agreementStart`. |
| remindersMutedMonth | INTEGER | `YYYYMM`, the month of the renter's **latest bill** when "Stop this month" was tapped. See "Reminder mute" below. |
| active | INTEGER NOT NULL DEFAULT 1 | 0 once the renter has left (see `leftOn`). Every active-renter list, bill to-do, raise offer and reminder filters on this. |
| leftOn | INTEGER | epoch day the renter was marked as left. Null while they live there. Moving back clears both. |
| createdAt | INTEGER NOT NULL | |

Indexes: `(addressId)`, `(active)`.
An address can have several active renters (for example 3 at one address, 1 at each of the others). `unit` is required when the address already has an active renter, and two active renters at one address can't share a `unit`. SQLite partial indexes aren't expressible in Room, so `SaveRenterUseCase` checks this inside the save transaction.

### `fixed_charge`
`id`, `renterId` (FK RESTRICT, index), `label`, `amountPaise`, `active`. Editing a renter updates the charges that are still on the form, adds new ones and switches off the rest (`active = 0`), never deleting a row. Billing uses only active charges.
Editing the amount affects only future bills, because bills copy it into `bill_line`.

### `rent_change`
`id`, `renterId` (FK RESTRICT, index), `oldRentPaise`, `newRentPaise`, `effectiveMonth` (`YYYYMM`), `decidedOn` (epoch day), `voidedAt` (set by Undo; reads use `voidedAt IS NULL`).
Saying yes stores the row **and** sets `renter.monthlyRentPaise` (and clears `raiseSnoozedUntil`) in one transaction. Undo hides the row and puts the old rent back. `effectiveMonth` is the renter's first month without a bill at that moment, so past bills are never touched.
Next raise month = (latest active `effectiveMonth`, or the agreement start month) + `raiseEveryMonths`. "Not now" sets `renter.raiseSnoozedUntil` to next month.

### `bill`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK autogen | |
| renterId | INTEGER NOT NULL | FK → renter, RESTRICT |
| month | INTEGER NOT NULL | `YYYYMM`. **Unique with `renterId`.** |
| dueOn | INTEGER NOT NULL | epoch day: the renter's due day in the billing month itself (the November bill is due on 5 Nov), copied when the bill is made. Bills made earlier were due in the following month and keep the date they were made with |
| prevReading | INTEGER | Null when electricity isn't billed. Stored explicitly, so no lookback is ever needed. |
| currReading | INTEGER | |
| ratePaise | INTEGER | The rate actually used (custom or global at that time). |
| meterReset | INTEGER NOT NULL DEFAULT 0 | 1 when the meter was replaced and `prevReading` is a new start. |
| meterPhotoPath | TEXT | The **file name** of the proof photo (`<24 hex>.jpg`) in the app's `meter_photos` folder. Never a path: the app joins it to its own folder, and a backup file with anything but a plain `[A-Za-z0-9_-]{1,64}.jpg` name is refused, so a hostile file can't point outside that folder. Null when there is no photo. Has a default in the entity, so older backup files still decode. |
| sentAt | INTEGER | epoch millis when the WhatsApp message was opened |
| createdAt | INTEGER NOT NULL | |

Indexes: unique `(renterId, month)`.
A bill can be deleted (ADR 0009): its lines go with it (cascade), and the month is free to be billed again. Undo inserts the same bill again with the same id, so it fails if a new bill has taken the month.
Units used = `currReading − prevReading`, derived and not stored.

### `bill_line`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK autogen | |
| billId | INTEGER NOT NULL | FK → bill, **CASCADE** (lines belong to the bill). Index. |
| kind | TEXT NOT NULL | `RENT`, `ELECTRICITY`, `FIXED_CHARGE`, `ADJUSTMENT` |
| label | TEXT NOT NULL | "Rent", "Electricity", "Water", "Waived late fee" |
| amountPaise | INTEGER NOT NULL | Negative only for `ADJUSTMENT` (discount or waiver). |
| sortOrder | INTEGER NOT NULL | |

A bill total is `SUM(amountPaise)` of its lines, computed in the query. One-off extra charges and corrections are `ADJUSTMENT` lines.

### `payment`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK autogen | |
| renterId | INTEGER NOT NULL | FK → renter, RESTRICT |
| amountPaise | INTEGER NOT NULL | Greater than 0, enforced in the domain. Can have paise, because a bill can. |
| paidOn | INTEGER NOT NULL | epoch day |
| mode | TEXT NOT NULL | The name of `PaymentMode`: `Upi`, `Cash` or `Bank`. Never rename them. |
| note | TEXT | |
| voidedAt | INTEGER | Set instead of deleting. All queries use `voidedAt IS NULL`. |
| createdAt | INTEGER NOT NULL | |

Indexes: `(renterId, paidOn)`.
Payments aren't linked to a bill. They reduce the renter's running balance, which handles partial payments and overpayment naturally.

### `backup_snapshot` (single row, `id = 1`)
`id`, `json` (the whole database as backup JSON), `takenAt` (epoch millis), `afterRestore` (what the data looked like right after the restore, for the "restoring twice" rule below). Written in the same transaction as a restore and used up by "Undo last restore". It is never part of a backup file.

### `app_settings` (single row, `id = 1`)
`id`, `globalRatePaise` (default 800), `reminderMinuteOfDay` (default 540 = 9:00 AM), `reminderRepeatDays` (`2`, `3`, or `0` = once), `lastBackupAt`.
There's no seeding. The repository maps a missing row to defaults and upserts on the first change.

## Derived queries (never stored)
- **Balance (pending)** per renter = the sum of the renter's bill lines − the sum of their payments where `voidedAt IS NULL`. The domain computes it (`PendingCalculator`) from the bill and payment flows, so Home updates by itself when either changes. A negative balance means the renter paid ahead.
- **Ledger** for the renter screen: bills and payments merged by date. A bill sits on the day it was made (`bill.createdAt`), but never later than its due date, so a bill entered long after the fact doesn't jump ahead of the payments that came before it. On the same day the bill comes first. The running balance is a `runningFold` in the use case.
- **Previous pending** in the WhatsApp message = the balance just before this bill's lines.
- **Status** is computed in the domain from the balance, the latest bill and the injected `Clock`: No bill yet / Paid / Late (money owed and today is after the latest bill's `dueOn`) / Part paid (a payment since the latest bill was made) / Unpaid. "Bill sent" needs `bill.sentAt`, which isn't built yet.
- **Unbilled months** per active renter = every month from `firstBillMonth` up to the current month that has no `bill` row. Bills are created only by the landlord, so this drives the "Create <month> bill" to-do and the monthly notification.
- **Reminder mute:** a renter is muted while `remindersMutedMonth >= the month of their latest bill`. "Stop this month" sets it to the month of the renter's latest bill, not the calendar month. The next bill is newer, so reminders resume with no cleanup job, and muting on 2 Dec about November's bill doesn't silence December's.

## Integrity and persistence
- Foreign keys on, `RESTRICT` everywhere except `bill_line` → `bill` (CASCADE).
- Domain constructors `require` the rules Room can't express: amounts, ranges, `dueDay` 1–31, `startingReading` present when billing electricity.
- `exportSchema = true`, with schema JSON committed under `data/schemas/` for every version.
- Additive changes use auto-migrations. Everything else gets a hand-written migration and a migration test.
- WAL journaling (the Room default with the bundled driver). The database lives in app-private storage.
- Android: disable system auto-backup of the database file, since the JSON backup is the supported route. iOS: the default iCloud device backup stays on.

## Backup file (JSON)
- Top level: `schemaVersion`, `exportedAt`, then one array per table (`address`, `renter`, `fixedCharge`, `rentChange`, `bill`, `billLine`, `payment`) and `settings`. The entities are `@Serializable`, so backup and restore work on entities directly, with no domain mapping and no separate backup model.
- **Restore** runs in one transaction: validate the file, check `schemaVersion` is not newer than the app's, check all foreign keys resolve, clear the tables, insert, commit. If anything fails, nothing changes.
- **Before every restore** the app saves an automatic snapshot of the current data (the same JSON format, keeping the latest one) in the `backup_snapshot` table, so it is written in the same transaction as the restore: a failed restore leaves no snapshot and changes nothing. Restore wipes the tables and Undo can't cover that, so Settings has "Undo last restore", which restores the snapshot and uses it up.
- **Checks on a file before it touches anything** (`BackupCodec`): it parses, `schemaVersion` is not newer than the app's (a newer file is refused before its fields are read), ids are unique, every link points at a row in the file, one bill per renter and month, bill-line kinds and payment modes are names this app knows, and the values that would crash the app later are in range (due day 1-31, months that exist, raise percent, reminder time and repeat). The database's own constraints are the backstop, for example the case-insensitive unique address name, and a failure there rolls everything back.
- Dates must be days between 2000 and 2200 and a phone number must be 11-15 digits, because a value outside that would throw when a screen reads it, or end up inside the `wa.me` link.
- Removed payments and rent changes are in the file: they are history. New rows get ids after the restored ones.
- **The file format is frozen with the schema.** Its property names are the entities' property names, so from the first real install: a new property needs a default value in the entity (so older files still decode), and a rename or removal needs a conversion of the old files. `GoldenBackups` (a frozen copy of a full version 1 file and a real one from the app) must always decode: a failing `GoldenBackupTest` means the change broke old backups.
- **Restoring twice** ("wrong file, try another"): if the data has not been touched since the last restore, the snapshot of what was there before it is kept, so one Undo still reaches the original data. If the data was changed in between, the snapshot is replaced by that newer data. Making a backup in between does not count as a change.
- **"Last backup"** is the last time a file was handed to the share sheet, recorded only after the sheet opened (`MarkBackedUpUseCase`). The app can't know if the person then finished sharing it. If the file could not be written or the sheet could not open, Settings says so and the date does not change.
- **The picker reads at most 25 MB.** A bigger file is reported as not a backup, so picking something else by mistake can't freeze or crash the app.
- The backup file is plain JSON with no password, for now. It contains renters' names and phone numbers, so the Settings screen should say so when sharing.
- Meter photos are files, not rows. The JSON stores their file names. Bundling the photos into the backup is later work, so v1 backups restore data without photos: after a restore on another phone the name is there but the file isn't, and the screen then shows no photo link.

## Schema changes: before and after real data
Room refuses to open a database whose schema differs from the one the file was created with ("cannot verify the data integrity"), and the app crashes at launch.
- **Until the app holds data you care about** (today: emulators and simulators only), the schema stays at version 1 and a change means uninstalling the app so the old file is deleted. The saved schema file in `data/schemas/` is overwritten.
- **From the first install on your own phone, version 1 is frozen.** Every schema change then bumps the version, adds a migration (auto-migration for added columns and tables, a hand-written one for anything else) and a migration test that opens a database created at the old version.
- When that day comes, write the date and the first frozen version here.

## Open assumptions
- Meter readings are whole numbers. If a meter shows decimals, change `prevReading`/`currReading` to tenths-of-a-unit integers.
- An address can have many renters. The `unit` label tells them apart.
- Rate per unit is stored in paise, so ₹8.50 is `850`.
