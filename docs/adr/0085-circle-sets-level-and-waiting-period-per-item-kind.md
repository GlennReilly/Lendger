# 0085. Stewards set each kind of item's level and waiting-period membership

- Status: Accepted
- Date: 2026-11-14
- Supersedes: only point B1 of ADR 0051 (the "short list" of circle rules that may be changed). ADR 0051 is not edited; everything else in it (steward quorum, 14-day window, cool-off, decisions judged by the rules of their own day) is unchanged and used here.

## Context

`ItemClass` fixed, in code, the lowest level that may ask for each kind of item and whether the kind is in the waiting period after an unreported incident (ADR 0084): Essential Level 1, Everyday Level 1, Valuable Level 2 and in the waiting period, High-value Level 3 and in the waiting period. A circle with different needs (a small group of close friends, or a very cautious one) could not change this without a release. The owner asked that stewards set these on the Circle rules screen, with the defaults unchanged.

## Decision

1. **Six new numbers in the circle rules.** For Everyday, Valuable and High-value: the level (1, 2 or 3, shown as "Level N" through `TierText`) and whether the kind is in the waiting period. They are part of `ConfigSettings` (`everydayLevel`, `valuableLevel`, `highValueLevel`, `everydayWaiting`, `valuableWaiting`, `highValueWaiting`) and of `CircleConfig.classRules` (`ItemClassRule(minTier, waiting)`, `CircleConfig.ruleOf`). The defaults are exactly today's: 1, 2, 3 and no / yes / yes.
2. **Essential is not a setting.** It is always Level 1 and never in the waiting period, so there is nothing to validate away: the library already treats an Essential item as always open to members (even during a pause or waiting period), and `CircleConfig.validate()` now also refuses a config that says otherwise.
3. **Level 4 is not allowed.** Level 4 is the steward-only points tier; an item can need Level 1, 2 or 3 ("A kind of item can need Level 1, 2 or 3.").
4. **Order.** Levels cannot fall from Everyday to Valuable to High-value ("A more valuable kind of item cannot need a lower level than a less valuable one."). Whether a kind is in the waiting period is free.
5. **Same vote as the other rules.** Changes travel in the existing sealed `config.update` event (ADR 0051): the same steward quorum, 14-day window and cool-off, and a decision is judged by the rules in force on its own day (`configOn(day)`), so old loan requests are not re-judged. The server still accepts the event opaquely.
6. **Wire format.** The six numbers are six optional keys of the `config.update` payload, written as whole numbers (the flags as 0 or 1). A payload with none of them (every event written before this ADR) reads as the defaults; a payload with some but not all, a flag other than 0 or 1, or an unknown key is refused. Votes compare the whole settings, so an old-format vote and a new-format vote of the same values count together.
7. **Where the rules apply.** `AccessPolicy.canBorrow` takes the level and the waiting-period membership from the circle's rules for the item's kind. `StandingRules`/`MarketFold` already pass the config of the request's day, so the fold, `ItemSummary.access` and the Items "Not yet" lines use the effective rules. The owner's own "Who may ask" bar still works and cannot go below the kind's level (the larger of the two applies; the app drops a bar at or below the kind's level when saving). The sentence on My level and in refusals that names which items the waiting period does not touch now lists them from the rules ("Everyday and essential items" by default), through `unaffectedKindsText`.
8. **UI.** Circle rules lists each kind ("Valuable items: Level 2, in the waiting period"; "Essential items: Level 1, always open, never in the waiting period"). The steward form gets three selects (the level of each kind) and three tick boxes (in the waiting period), with the Essential rule and the order rule stated in a sentence; the "Agree to these rules" button and the proposal line carry the new values. Item-form "Who may ask" options stay "Level 2 or above" and "Level 3 or above".

## Consequences

- No clean break: old logs fold unchanged; a circle that never changes these keeps today's behaviour exactly.
- `ConfigSettings` and `CircleConfig` gain fields with defaults, so existing constructors and tests compile.
- The owner's "Who may ask" bar below a lowered class default is simply ignored, as it already was below the old default.
- A kind can now be put in the waiting period or taken out of it by vote; the explanation shown to members reads the circle's own rules, not a fixed list.
- The reader of `item.upsert` still refuses an owner bar below the built-in class default; that refusal is not relaxed when a circle lowers a default, because an owner bar equal to Level 1 is meaningless and Level 2 or 3 are never below the built-in default.
