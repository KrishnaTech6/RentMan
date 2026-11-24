# Bills are immutable snapshots made of lines

When a bill is created, its rent, rate, readings and each charge are copied into the bill and its bill lines. Changing a renter's rent, a fixed charge or the global rate afterwards only affects future bills, so old statements and the balance history stay correct. Corrections are made with an adjustment line, not by recalculating (a bill made with a wrong reading is the exception: it is removed and made again, see ADR 0009). The alternative, recomputing old bills from current settings, would rewrite history whenever a rate changes.
