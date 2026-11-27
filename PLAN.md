# Rent Manager — plan (imported from claude.ai chat, 2026-11-22)

App for a landlord to track renters, monthly bills (rent + electricity + fixed charges), partial payments, and send bills on WhatsApp.

Engineering: `ARCHITECTURE.md`. Schema: `DATABASE.md`. Vocabulary: `GLOSSARY.md`. Decisions: `docs/adr/`. Visual spec: `design/project/`.

## Decisions
- Platform: Android + iOS, Kotlin Multiplatform. Compose Multiplatform UI, Room KMP, Koin, kotlinx-datetime.
- Data: on the phone only. Backup = export/import a JSON file via the share sheet. The file is plain, with no password, for now.
- Money: `Long` in paise, never Double.
- Pending = sum(bills) − sum(payments), computed live, never stored.
- Partial payments are normal.
- "Extra charge" = fixed monthly charge per renter (e.g. Water ₹200), added to every bill.
- Rent is a fixed negotiated amount per renter (₹4,500 … ₹24,000). No shared rent table.
- Renters are grouped by Address (see below). Home shows pending per address, with a filter.
- **The landlord creates every bill. The app prompts, it never creates one on its own.**

## Data model
The full schema is in `DATABASE.md` (v1). Summary:
- Address (replaces Property + Flat), Renter, FixedCharge, RentChange.
- Bill with BillLine rows (rent, electricity, fixed charges, adjustments). Meter readings and the proof photo live on the bill, with no separate MeterReading table. A bill is a snapshot.
- Payment (UPI / cash / bank), voidable so Undo works.
- AppSettings: a single row in the same database (global rate, reminder time, repeat interval, last backup).

## Rules
- Electricity rate used = `customRate ?: globalRate`, saved on each bill. Changing the global rate affects only future bills of renters with no custom rate.
- If `billsElectricity` is false, skip the reading step entirely.
- Rent increase: 11 months after agreementStart (or after the last increase), Home shows a "Raise rent 10%?" card.
  - Yes → new rent applies from the next bill, round to nearest ₹10, write a RentChange row (₹7,000 → ₹7,700).
  - Not now → the card returns next month.
- Reading lower than the previous one → warn and offer "meter replaced" (enter a new starting reading).
- Reading far above the renter's usual usage → warn.
- Renter leaves → mark inactive, keep history.
- First bill uses the starting reading from move-in.

## Creating bills
- Each renter has a **first bill month**, set when they're added (default: the current month). From then on, every month without a bill is an **unbilled month**.
- Home shows a "Create <month> bill" to-do for each renter with an unbilled month, oldest first.
- Renter who pays electricity: enter the reading → bill preview → send. Renter who doesn't: straight to the bill preview with rent and fixed charges filled in → send.
- Skipped months stay on the to-do list until you create them. Pending only counts bills that exist.
- Each renter gets a "Create <month> bill: <name>" notification (repeating per the Settings interval) from the day that bill opens until it is made. A bill opens on the 1st of its month, or 3 days before the renter's due day if that is earlier (due on the 1st: the 28th before), so Home's "Create bill" card and the notification start together.

## Flow and screens
Monthly path: create bill (reading if needed) → bill preview (math shown: 1325 − 1240 = 85 × ₹8) → send on WhatsApp → record payment.

Built: Home with each renter's pending and status and the total still to collect, Renter detail (balance and history, remove a payment with Undo) and Record payment (amount or full amount, UPI / Cash / Bank, date stepper, note). The date is picked a day at a time, which is fine for a payment made recently. Payments can have paise. A bill's "Previous pending" now has payments taken off, and a renter who paid ahead sees a credit line.

Built: Settings, opened from Home. It has the rate per unit (future bills only), the reminder time (steps of 30 minutes) and the repeat interval (every 2 days, every 3 days, once). Nothing sends a reminder yet, so those two are only stored. Addresses and backup are not on it yet.

Built: the rent increase. From the raise month, Home shows "Raise rent for Ravi by 10%?" with the old and new rent and the bill it starts from. "Raise rent" applies it at once with an Undo bar for 5 seconds. "Not now" brings the card back next month. The next raise is counted from the last accepted one.

Built: reminders.
- **What is planned** (`ReminderPlanner`): "Create <month> bill: <name>" for each renter, from the day that bill opens (see Creating bills) and repeating until it is made; and "Rent pending: <name>" from the day after the due date of the oldest bill that is still owed and overdue (the latest bill's due date while nothing is overdue yet), repeating until it is paid. Both use the reminder time and the repeat interval from Settings, and the cadence is counted from the first day, so it doesn't drift when the app is opened late.
- **How it works:** a notification can't check "is it paid yet?", so the app plans up to 5 slots ahead per reminder (60 in all, under iOS's limit of 64) and plans again whenever a bill, payment, mute or setting changes, and every time the app opens. Android uses WorkManager (the time is the earliest, so it can arrive a little late). iOS uses local notifications.
- **Stop this month:** a button on the rent reminder and on the renter screen (with Resume). It lasts until the renter's next bill. The manual "Remind" button is not built.
- **Permission:** Settings shows "Allow notifications" until it is granted, and only once the phone has said it is not (on iPhone the answer comes a moment late, and before it did the row flashed up and vanished). The app asks once, the first time it is opened: on iPhone only if it was never asked, on Android with a flag kept on the phone, because Android can't say "never asked". A refusal is not asked again at launch. The Settings row asks again, and when the phone won't ask any more it opens the phone's own settings for the app.
- **Reminder time and repeat interval** are saved the moment they are changed, not with the Save button (leaving the screen without Save used to throw the change away). The reminders are planned again from the saved time. The rate still needs Save, and a rate being typed is kept when the time is saved.
- Not checked on a real phone: the iOS notification actually appearing, and the iOS button.

Built: backup and restore, in Settings.
- **Back up now** makes one JSON file with everything and opens the phone's share sheet (Android: a chooser. iOS: the share sheet). **Restore from a file** opens the file picker, checks the file, and replaces all data in one transaction. A file that is not a backup, is damaged, or comes from a newer version is refused and nothing changes.
- **Undo last restore** brings back what you had before the restore (one level, then it is used up). Restoring a second file without using the app in between keeps the first snapshot, so "wrong file, try another" can't lose your data.
- The file has no password and holds names and phone numbers, and Settings says so. Meter photos are not in it yet.
- Not checked on a real phone: the iOS share sheet and file picker.

Built: renter edit and "mark left".
- **Edit** is the Add renter form filled in with the saved renter, opened from the renter screen. It has the same checks. Changing the rent or the due day affects future bills only: a bill already made keeps its own snapshot. Fixed charges can be removed or added (a change of amount is a remove and an add): a removed charge is switched off, never deleted. When someone else lives at the address, the unit must be different.
- **Mark as left** is only possible once the renter owes nothing (a payment is recorded first), so what someone owed can't be forgotten when they drop out of "still to collect". They keep their bills and payments, stop getting reminders and bill to-dos, and show under "Left renters" on Home, where they can be opened or moved back in. Moving back fails if someone else now has the same unit. A renter who left can still be edited, but payments can't be added to or removed from their history.

Built: the meter photo.
- On the reading step, **Scan the meter with the camera** opens the phone's camera. The photo is turned the right way up, shrunk (at most 1,600 pixels on the long side, about 50-300 KB) and kept in the app's own storage. The text on it is read on the phone, with no internet: ML Kit on Android, Apple Vision on iOS.
- The reading is **only suggested**. The reader picks the number a meter display is likely to show (five or six digits, not the serial number, the voltage, "imp/kWh" or the year, and not below the last reading or far above it), puts it in the reading box, and says "Read from the photo. Check it against the meter." The landlord checks it, can type over it, and still sees the full bill math before saving. If no number is found, the photo is kept and the screen says to type it.
- The photo is saved with the bill as proof. In the renter's history a bill with a photo has a **Meter photo** link that opens it in the phone's viewer (Photos on Android, Quick Look on iOS). Backups don't include photos yet, so after a restore on another phone the link is simply not shown.
- Retaking or removing a photo, or leaving without saving the bill, leaves the file behind in the photo folder. Nothing cleans those up yet.
- On the iOS simulator, which has no camera, the button picks a photo from the library instead. Not checked on a real iPhone: the camera itself and the Quick Look viewer.

Built: **Months** (Home card, then Months list, then one Month). Tap the month card on Home ("All months ›") for every month newest first, from the first bill to this month, or to the newest bill if that is later (a bill made early for next month has its own row). Each row shows received out of billed, a thin bar, and what is left. Tap a month for the same card, "Cash received in <month>" (payments dated in it, a different number from Received), and every renter in that month, most urgent first. Tap a renter for their own screen.
- One source of numbers: `RenterMonth` (one renter in one month) is what `MonthSummary`, the list and the month screen all add up, so they can't disagree. The urgency order is one shared function (`RenterStatus.urgency`) used by Home and the month screen.
- Who has a row in a month: anyone with a bill that month (also someone who has left), and an active renter with no bill whose first bill month has come (a "No bill" row). Someone who left with no bill that month has no row.
- **Status here is per bill, Home's is per renter.** A payment that cleared October can leave Ravi "Part paid" on Home (a payment came after his newest bill) and "Unpaid" for November on the month screen (nothing of November's bill is settled). Same words, deliberately different question.
- Not built: a renters-by-months grid, a chart of received per month, a financial-year statement. No schema change.

Built: **the month card** (Home), which replaces the "Still to collect" card below. Checked by hand on a real backup: November ₹69,784 received of ₹76,996, ₹7,212 left.
- Why: the old card said what is owed but not out of how much. A landlord also wants "how is this month going".
- What: one blue card for the current calendar month. Big number: **received**, then "of ₹X expected". One bar splits the month into received, not due yet, late, and the rent of renters with no bill yet, with a short legend under it. A line "+ ₹X still owed from earlier months" appears only when something older is open.
- All figures belong to **one month**, so they add up: billed = received + late + not due yet, and expected = billed + rent of the not-billed renters. Received is **not** counted by payment date: a renter's payments settle their **oldest bill first** (the order the renter's running balance uses), so an unpaid October bill is settled before November's, and a December bill made early (at the end of November) and paid never counts as November. Late or not due is judged by the month's bill due day. A renter paying ahead is capped at the bill.
- Dropped on purpose: "N owing" (the status chips show it) and tapping the legend to filter the list. Late here is per bill and the chips are per renter by the latest bill, so a tap would show a list that misses some of the late amount. A renter is now Late when any past-due money is owed, so this could be added later.
- No schema change. The old `StillToCollect` is gone.

Superseded: **Still to collect** in detail (Home). Checked on the emulator with a real backup: ₹5,796 not due yet, 1 owing, 4 not billed yet (₹53,000 rent). Late is covered by tests only, no late renter in that data.
- Why: one number hides what to do. ₹9,000 late means call today. ₹9,000 not due yet means wait. And a renter with no bill yet is not in the number at all, so on a new setup the number looks small while most of the rent is still unbilled. (Seen in a real backup: 6 renters, 2 billed, 4 not.)
- The big number stays what it is: what is owed now, from renters who owe, credits ignored (someone who paid ahead doesn't reduce what the others owe).
- Under it, two lines: **late** and **not due yet** (the two add up to the big number; a part that is zero is left out), then **N owing** (renters who owe), then **N not billed yet (₹X rent)** (renters who have a "Create bill" to-do, and one month's rent each, rent only, because the electricity isn't known before the reading).
- **Late** here is counted **per bill**, not per renter: the part of what someone owes that comes from bills past their due day, with payments settling the oldest bill first (so it matches the running balance on the renter screen). A renter whose October bill is unpaid and overdue still has that amount in Late after the November bill is made, even though the newest bill isn't due. **Not due yet** is the rest. (Later change: the renter's status chip and the "Rent pending" reminders now count this too. A renter is Late when any past-due money is owed, and days late count from the oldest bill still owed.)
- Left out on purpose: a "collected of billed" bar (the original design had one). The total includes older balances, so "collected this month of billed this month" would not add up to it. Tap-to-filter the renter list is a later, optional step.
- Built from what Home already has (each renter's pending and status, and the bill to-dos). No schema change.

Built: meter photo, round two (found by using it on a real phone). Checked on the Android emulator (gallery, crop, preview on both steps, cancel leaves no file) and, for the iOS file steps, on the simulator. Not checked on the iPhone: the crop screen's touch dragging, and the camera.
1. **Preview.** After a photo is attached, the reading step and the review step show it as a picture (tap to open it big in the phone's viewer), so the landlord can compare the number the app suggested with the meter itself. This is also the best protection against a wrong suggestion reaching a bill.
2. **Adjust what part to keep.** After taking or choosing a photo, a crop screen shows it with a box over it. The landlord drags the box (and its corners) around the meter's number and taps **Use this part**. **Use whole photo** skips the cropping. The box is on the whole meter, so what is kept as proof shows the meter body and its serial number (a real photo of that meter), while the number is read only in the meter's display inside the box, where the serial number can't get in the way. Both phones use our own crop screen (the box can be any shape, which suits a wide display). The iPhone first used Apple's square "Move and Scale" crop, because our screen once vanished a moment after it opened there, but that square kept serial numbers in view and the wrong number was filled in, so it is off again; the picker now finishes closing before our screen opens. After the scan, up to three numbers read from the kept part are shown as buttons under the reading ("Numbers in the photo. Tap the reading:"), likeliest first and already filled in, so a serial number or a decimal digit taken as a whole one is fixed with one tap. The last reading decides: numbers not below it and at most 2,000 units above it come first, closest first, because serial and standard numbers (e.g. "Sr No 706047", "IS 13779") are anywhere, while a meter moves a few hundred units a month. If no number fits, nothing is filled in ("type it in") and the numbers are only offered as buttons. A counter display is read in pieces, one window at a time and with a 0 often taken for a 1 ("16", "566", "4"), so short digit pieces on the same row are joined left to right by their position on the photo, and every run of 3 to 7 of those digits is a candidate ("165664" gives 6566). A small crop is enlarged (up to 5x, to about 2,000 px) before its text is read, only for the reading. Checked on 10 real photos of an AMTL meter (06566[4], last reading 6504) with Apple's reader on the Mac: all 10 give 6566 (MeterTextTest). The crop box is placed for the landlord: the whole photo is read first ("Finding the meter…"), each meter display is found from its row of digit pieces (or one 4–7 digit number that fits the last reading; a printed number like a serial is never cut into runs), and the box starts around the display that fits this renter's last reading, otherwise the left-most one. "Next meter" moves it to the next display, and it can still be dragged. A display that is not read at all (too dark) gets no box. Checked on a real photo of three meters (9913 left, 6566 middle): the box lands on the right meter for each renter and its crop reads the right number.
3. **Choose from the gallery.** Next to "Scan the meter with the camera" there is "Choose from gallery" (Android: the system photo picker, which needs no permission. iOS: the photo library picker). A chosen photo goes through the same shrink, turn-upright, crop, read and save steps as a camera photo.
- No change to the bill, the database or the ViewModel: the crop screen and the picking are UI-only phone hooks (like the camera), and the ViewModel still gets one "photo + lines of text" when it is finished. The crop box maths is pure and tested.
- A photo that is cancelled at the crop screen is deleted at once. A photo that is retaken or removed later still stays in the folder (known shortcut, see above).

Built: **remove a wrong bill** (decision: ADR 0009). Checked on the Android emulator: a bill with a typo reading (₹1,03,580) was removed, Home showed "Create Nov 2026 bill" again, the right reading made an ₹8,180 bill, and Remove then Undo brought the same bill back. Not checked on the iPhone.
- Why: a bill made with a wrong meter reading could not be fixed at all in the app (a bill is a snapshot, never edited). Fixing the amount with an adjustment line does not work for a reading: the wrong reading stays on the bill and the next bill starts from it.
- What: in the renter's history, the **newest** bill of an active renter has **Remove bill**. It is gone at once and an **Undo** bar shows for a few seconds (the same as removing a payment). The month then shows on Home as **Create <month> bill** again, and the landlord makes the bill again with the right reading. The meter photo file is not deleted, so Undo still has it.
- Only the newest bill: removing an older one would break the chain of readings (each bill starts from the one before). To fix an older bill, remove the newer ones first.
- Payments are left alone. What the renter owes is worked out again, so someone who had paid the removed bill is shown as paid ahead until the new bill is made. The renter may already have the wrong bill on WhatsApp, so the new one is sent again by the landlord.
- Not a "soft" hide like payments: bills are unique per renter and month, so a hidden bill would block making the bill again. The bill is deleted, and Undo puts it back with the same id. No schema change, so no migration, and the data on real phones is not touched.
- A bill made again uses the **rent of its own month**, not what the renter pays now (`RentHistory.rentFor`): a raise accepted later, for a later month, does not leak into it. The rate per unit and the fixed charges are still today's, so a bill made again after changing those uses the new ones.
- Not built: editing a bill's reading in place, and adjustment lines (the type exists, there is no screen).

Screens: Home (total to collect + to-do cards: create bill, remind late renter, approve rent increase), Renter detail (bills, payments, running balance), Enter reading → bill preview, Record payment, Add/edit renter, Address picker, Settings (global rate, reminder time, repeat interval, addresses, backup/restore).

UX: undo instead of confirm dialogs (do it now, show Undo for a few seconds), big tap targets, one primary button per screen. Amounts use Indian grouping (₹1,00,000). Text scales with the system font size.

## WhatsApp
`https://wa.me/<number>?text=<encoded>` opens the renter's chat with the bill typed in (built: opened with the platform's URL handler, so no platform code). The "Previous pending" line is left out when nothing is owed. The number is stored with country code. One expect/actual launcher. Text only. An image or PDF needs the share sheet, where the user picks the contact, so that's later.

Recording a payment can post it to the landlord's group: "Save & post on WhatsApp" opens `https://wa.me/?text=<encoded>` (no number, so WhatsApp asks which chat) with `Paid: <name> (<unit · address>) - ₹<amount>`. The landlord picks the group and presses send: WhatsApp lets no app post into a group by itself. "Save only" posts nothing. A post stays in the group even if the payment is later removed.

The month screen's **Share** posts the month review the same way: the month, `Total` (this month's bills), `Received` and, if anything is left, `Pending` with one `Name - ₹amount` line per renter who still owes. Renters with no bill and earlier months' dues are not in it.

```
Hi Ravi, your bill for Nov 2026:
Rent: ₹7,500
Electricity: 1240 → 1325 = 85 units × ₹8 = ₹680
Water: ₹200
Previous pending: ₹1,880
Total due: ₹10,260
Please pay by 5 Nov. Thanks!
```

## Notifications
Two kinds, both derived from the data and resynced whenever it changes (see `ARCHITECTURE.md` §7):
- **Create a bill** for each renter, from the day it opens (the 1st, or 3 days before the due day if earlier) until it is made.
- **Rent pending** for each renter with a pending amount after the due day, repeating (Settings: every 2 days / every 3 days / once) until it's paid.
- **Stop for the month:** the notification has a "Stop reminders this month" action, and the Renter screen has "Stop this month". It stops reminders for that renter until a newer bill exists, so the pending amount carries into next month's bill as usual. The manual "Remind" button still works while muted.
- Android: WorkManager and the notification permission. iOS: `UNUserNotificationCenter`.

## Addresses (replaces Property + Flat)
Each renter has one address picked from a dropdown list (the old Property field), e.g. 338 A, 338 B, 338 C, AIMS G-703, AIMS I-301, Supertech B9-1906. The picker has an "add new address" field that adds it to the list and selects it. Address: id, name. An address can have several renters (3 at one, 1 at each of the others), so each renter also has a flat/floor label ("Ground floor", "1st floor back") shown as "label · address". The Home filter chips are by address, so make them horizontally scrollable.

## Settings screen
Rate per unit (global ₹8, future bills only) · reminder time · repeat interval · manage addresses · back up now / restore with last-backup time. Restore saves an automatic snapshot of the current data first.

## Build order
1. Domain rules and the Address slice (end to end), then renters, bills, payments, rent increase, custom rate
2. Notifications
3. Backup/restore
4. WhatsApp bill image/PDF (**not built yet**; the bill goes as text only)
5. Meter photo OCR (ML Kit / Apple Vision), user always confirms the number (built)

## Ideas (not building now)
Looked at together with a real backup (6 renters, 4 addresses, about ₹66,000 rent a month, 4 renters billed electricity). Most of these need a few months of data to be worth looking at, so they are ranked by what helps from day one.
1. **Money page** (partly built as Months, see above): for each month, billed, collected and still pending, a last-12-months view, and a split by address. "Collected" counts payments by the day they were paid and skips voided ones. "Billed" is the sum of saved bills. No schema change. Open question: which is the headline (we lean to "collected").
2. **Financial-year statement** (April to March, assumed, not confirmed): rent received per renter and per address, shared as CSV or PDF, for the income-tax return or a renter's HRA claim. Builds on the Money page queries.
3. **Bill as image or PDF** on WhatsApp (build order 4): needs the share sheet (already used for backups) and a render on each platform.
4. **Security deposit**: not tracked at all today. Record it per renter and, when marking someone as left, show deposit, owed and refund. Needs a schema change (fine before release).
5. **Electricity trend**: units per month per renter with a warning when a month is far above their average. Needs about 3 months of readings.
- Not worth doing yet: **vacancy tracking** (a Unit is only a text field on the renter, not a record) and **how late each renter pays** (payments aren't matched to specific bills, so it would be guesses).

## Notes
- The Claude Design mockup "Rent Manager – Key Screens" is saved in `design/project/` (8 artboards, 390px wide: Home, Reading, Bill, Payment, Renter, AddRenter, Address picker, Settings, plus `canvas.json`). Source: https://claude.ai/artifact/9xnyDrNnxBED1AvJXFZ4rb. It needs the claude.ai editor to render, but the markup reads fine as a visual spec.
- Light theme only for now. The design has no dark theme, so the app ignores the phone's dark mode (status-bar icons are forced dark on Android). Dark mode is a later decision.
- Look: Inter font, background #F4F5F7, primary blue #2747C9, hero card #1C2E8A, WhatsApp green #0B7A3B. Tabular numbers, 44px+ tap targets, 56px primary button pinned at the bottom.
- Mockup vs this plan, to settle when building:
  - Payment modes in the mockup are UPI / Cash / Bank. The schema has all three.
  - The mockup's WhatsApp text drops the "Previous pending" line and the meter readings. The plan keeps both, which is better for partial payers.
  - The Home filter chips and the sample names on the other screens still show the old placeholder properties ("Home building", "Model Town"). Cosmetic only.
  - "Send receipt on WhatsApp" on the payment screen is new and not in the original plan.
  - The Home to-do card says "Create <month> bill" for every renter, and the bill goes straight to the review when there's no electricity to bill. Built.
  - The mockup's "Enter meter reading" and "Bill preview" are one screen with two steps (reading, then review). The "usual usage" warning (more than 3x the average and at least 100 units) and "Save & send on WhatsApp" are built, and so is the meter photo.
