# 0084. The "hold" after an unreported incident is called a waiting period, and the pages explain it

- Status: Accepted
- Date: 2026-11-14
- Supersedes: nothing. No earlier ADR fixed the word; ADR 0042 and the spec (C7) call the rule an "incident cooldown", which stays as the identifier name only.

## Context

After an unreported incident (damage or loss found later), higher-value items are closed to a member for a while (`incidentCooldownDays`, default 90). People read this as "on hold", "the hold" or a "cooldown", which sounds like a pause (ADR 0050), a penalty or a ban. It is none of these: it is automatic, it ends by itself, it touches only higher-value items, and it does not change the member's level. The owner chose the term **waiting period** (not "hold", not "wait time").

## Decision

1. **One term.** Everything people read says "waiting period": the Circle rules list and the proposal line, the form field label ("Waiting period for higher-value items after an unreported incident (days): "), the refusal when a rules change is out of range ("The waiting period for higher-value items after an unreported incident can be 0 to 365 days."), the Items "Not yet" reasons ("<item> is in a waiting period for N more days after an unreported incident.") with the hint "The waiting period ends by itself in N days. It is not a ban, and it is not a pause that members voted on.", and the design document.
2. **A block on My level while one is active.** Heading "Waiting period", then: "A recent unreported incident means a waiting period of N more days before you can borrow higher-value items. Everyday and essential items are not affected, and your level does not change." and one plain line: "A waiting period starts by itself after an unreported incident and ends by itself. A pause is different: members vote on it." The same sentence is left out of the "Why" list on that page so it is said once. The sentence comes from one function, `waitingPeriodSentence`, in `CircleAccess.kt`; `StandingSummary.waitingDays` carries the days left.
3. **The not-a-pause line also sits on the Circle rules explanation** (Stewards page), because that is where stewards set the length.
4. **A scan.** `StandaloneWordsSourceScanTest` fails if a string literal in shared or web code says "on hold", "the hold", "hold ends", "cooldown" or "cool-down". Identifiers, event names, config keys (`incidentCooldownDays`, `heldDuringCooldown`), sealed values and accepted ADRs keep their names.
5. **Singular days.** "1 more day" rather than "1 more days".

## Consequences

- No data or format change: the config key and the event stay as they are, so old logs fold as before.
- A reason that carries the waiting-period sentence now appears in item refusals too (it is the member's own standing reason), next to the item's own sentence.
- Any new text about this rule must say "waiting period"; the scan enforces it.
