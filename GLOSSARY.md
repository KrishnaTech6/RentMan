# Rent Manager

A landlord's tool for tracking renters across several addresses: what each one owes each month, what they have paid, and what is still pending.

## Language

### Places and people

**Address**:
A building or flat location the landlord owns, picked from a list, e.g. "12 A" or "Park View G-7". One address can have several renters.
_Avoid_: Property, building, location

**Unit**:
The label that tells renters apart at one address, e.g. "Ground floor" or "1st floor back".
_Avoid_: Flat, room, floor

**Renter**:
A person who rents a unit and receives a monthly bill. A renter who has left stays on record as inactive.
_Avoid_: Tenant, customer, client

### Rent and charges

**Left renter**:
A renter who was marked as left on a date. They can only leave once every month up to now has a bill and they owe nothing. They keep their bills and payments, get no more reminders or bill to-dos, and can be moved back in.
_Avoid_: Inactive, former renter, deleted

**Rent**:
The fixed monthly amount negotiated with a renter. It changes only through a rent increase.
_Avoid_: Base amount, base rent

**Rent increase**:
A percentage raise of a renter's rent, offered after the agreed number of months (10% every 11 months by default) and applied only when the landlord says yes.
_Avoid_: Hike, escalation

**Agreement start**:
The date a renter's rental agreement began. Rent increases are counted from it.

**Fixed charge**:
A recurring flat amount added to a renter's bill every month, e.g. Water ₹200.
_Avoid_: Extra charge, dedicated charge

**Adjustment**:
A one-off amount added to or taken off a single bill, e.g. a waiver or a repair cost.
_Avoid_: Discount, correction

### Electricity

**Meter reading**:
The number shown on a renter's electricity meter at billing time.

**Rate**:
The price of one unit of electricity. The global rate applies to everyone, and a custom rate overrides it for one renter.
_Avoid_: Tariff, price per unit

**Units**:
The electricity used in a month: this month's meter reading minus last month's.
_Avoid_: Consumption, usage

**Meter reset**:
A new starting meter reading entered because the meter was replaced and the new reading is lower than the old one.
_Avoid_: Meter replacement

### Billing and payment

**Billing month**:
The calendar month a bill covers. Each renter has at most one bill per billing month.

**Bill**:
The monthly statement for one renter, made of bill lines, saved exactly as it was calculated. Later changes to rent, rate or charges never alter it.
_Avoid_: Invoice

**Unbilled month**:
A billing month, from a renter's first bill month onward, that has no bill yet. The landlord creates the bill; the app only reminds.
_Avoid_: Missing bill, pending bill

**Bill line**:
One item on a bill: rent, electricity, a fixed charge, or an adjustment.

**Payment**:
Money a renter has handed over, by UPI, cash or bank. A payment is not tied to a bill and can cover only part of what is owed.
_Avoid_: Receipt, installment

**Pending**:
What a renter currently owes: all of their bill lines minus all of their payments. It is always calculated and never stored.
_Avoid_: Due, outstanding, balance owed

**Paid ahead**:
A renter whose payments are more than their bills, so their pending is below zero. The extra is a credit that lowers their next bill.
_Avoid_: Advance, overpaid, negative pending

**Part paid**:
A renter who owes money and has made a payment since their latest bill was made.

**Unpaid**:
A renter who owes money, has paid nothing since their latest bill was made, and is not yet late.
_Avoid_: Due

**Due day**:
The day of the month by which a renter should pay. A bill for one month is due on this day of that same month (the landlord makes it a few days before).

**Received**:
What Home's month card shows as the big number: how much of this month's bills the renters' payments have settled. Payments settle a renter's oldest bill first, so an unpaid older bill is settled before this month's. It is not counted by the day the payment was made.
_Avoid_: Income, revenue

**Month**:
A calendar month of billing, shown on the Months screen with what was received of what was billed. Open one to see every renter in it. Figures are as of today: an October bill still unpaid now counts as late in October.
_Avoid_: Period, cycle

**Cash received**:
Every payment dated in a month, whichever bill it went to. It answers "what reached my bank in October", which is a different number from **Received** (how much of October's bills is settled).
_Avoid_: Earnings, income

**Expected**:
This month's bills added up, plus one month's rent for each renter who has no bill for it (**Not billed**). It counts renters who have left for the bills they were given.
_Avoid_: Target, total due

**Not billed**:
An active renter who has no bill for a month, whether it is this month or one that was skipped. Shown on the month card with the number of renters and one month's rent each, with no electricity because that isn't known before the reading. A renter who has left is never not billed.

**Not due yet**:
The part of this month's bills, left after payments, that is not yet past the due day. The **late amount** is the part that is. What a renter still owes on bills of earlier months is shown apart, as "still owed from earlier months".
_Avoid_: Upcoming, current

**Late**:
A renter who owes money on any bill that is past its due day, even when their newest bill is not due yet. Days late count from the due day of the oldest bill that is still owed.
_Avoid_: Overdue, defaulted

**Reminder mute**:
The landlord's choice to stop reminders for one renter for the current billing month. Reminders resume with the next bill.
_Avoid_: Snooze, dismiss
