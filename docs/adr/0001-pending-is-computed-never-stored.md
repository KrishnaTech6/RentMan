# Pending is computed, never stored

Pending is always calculated from all bill lines minus all non-voided payments, and no pending, bill total or units-used column exists. A stored balance would drift from its sources after any edit, restore or partial failure, and with money that silently goes wrong. The cost is a SUM query on every read, which is trivial at this scale.
