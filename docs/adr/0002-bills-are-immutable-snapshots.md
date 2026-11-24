# Bills are immutable snapshots made of lines

When a bill is created, its rent, rate, readings and each charge are copied into the bill and its bill lines. Changing a renter's rent, a fixed charge or the global rate afterwards only affects future bills, so old statements and the balance history stay correct. Corrections are made with an adjustment line, not by recalculating. The alternative, recomputing old bills from current settings, would rewrite history whenever a rate changes.
