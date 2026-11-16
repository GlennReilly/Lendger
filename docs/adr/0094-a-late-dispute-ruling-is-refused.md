# 0094. A dispute ruling after the 30 days is refused

- Status: Accepted
- Date: 2026-11-16
- Supersedes: nothing. It clarifies ADR 0043 B1 (an unresolved dispute counts as dismissed after 30 days) by making the fold refuse a ruling that comes after that.

## Context

ADR 0043 B1 says a dispute that nobody resolves counts as `DISMISSED`, worked out when standing is computed: from the dispute's day plus `DAMAGE_CLAIM_DAYS` (30) (`StandingRules.eventsFrom`). The app already stops offering the ruling from that day (`CircleApp.canResolve`: today minus the dispute day must be under 30). But `MarketFold.resolve` accepted a `loan.resolve` at any time, so a ruling sent late, by a modified client or one that was offline for weeks, replaced "counts as dismissed" after the fact: a borrower whose credit had been released could later get the unreported-incident entry and the waiting period. The owner decided on 2026-11-16 that a late ruling is refused.

## Decision

1. The fold refuses a `loan.resolve` whose event day is on or after the day the dispute counts as dismissed, that is the dispute's day plus `DAMAGE_CLAIM_DAYS`. The one function `disputeClosesDay(disputeDay)` (in `StandingRules.kt`) gives that day, and the fold, the standing calculation and the app all use it, so they cannot drift apart.
2. Boundary: the dispute's own day is the first of its 30 days. A ruling dated the dispute's day plus 29 (the 30th day) is accepted; one dated the dispute's day plus 30 (the 31st day, the first day it counts as dismissed) is refused. This keeps ADR 0043's existing evaluation unchanged ("counts as `DISMISSED` from its 30th day" counted from the dispute's day) and the app's existing offer window.
3. The refusal is a full sentence with the last ruling day as a date placeholder (ADR 0082): "The time to rule on this dispute ended on {date:N}, so it counts as dismissed."
4. The server stays opaque: `loan.resolve` is sealed, so only the client folds can check the day (as for every loan rule).

## Consequences

- Once a dispute counts as dismissed, nothing can turn it into an upheld one; the borrower's released credit is final.
- A steward who rules on the last day while offline and syncs later still has the ruling counted, because the fold judges the event's own day, not the day it arrives. (The server refuses events dated more than a day in its future, but not ones dated in the past.)
- Tests: `MarketContract` (the last day accepted, the next day refused with the sentence, the late ruling changes nothing in standing; JVM and JS) and a real-server test (the app offers the ruling on the 30th day, not the 31st, and a hand-built late ruling reaches the server but is refused by every member's fold, with the dispute still shown as dismissed).
