# 0090. The circle's first members may confirm the founder's identity

- Status: Accepted
- Date: 2026-11-16
- Supersedes: ADR 0068's confirmer rule, only as it applies to the founder. ADR 0068 and ADR 0079 are not edited; everyone else keeps the ADR 0068 rule.

## Context

ADR 0068 lets only a steward or a member at Level 2 or above confirm someone's identity. A founder who is the only steward cannot confirm themselves, and new members start at Level 1, so the founder stays "identity not confirmed yet" for weeks (ADR 0079 exempts the founder from the steward rule for the same reason). The owner chose: the first members confirm the founder.

## Decision

1. The first 3 members, counted in log order of their access approval and not counting the founder, may confirm the founder's current name at any level. The number is the constant `FIRST_MEMBERS_WHO_MAY_CONFIRM_FOUNDER`.
2. "Access approval" is the first `key.wrap` that a steward sends to one of a member's devices (in the app: Approve access after checking the person in person). A later device, a rotation or a restore does not change the order. A member who later leaves keeps their place in the order, so a removed member cannot free a place.
3. The exception covers only the target "the founder" (the author of `circle.create`). It does not change who may confirm anyone else, nor the other checks of ADR 0068 (not yourself, current name only, once per confirmer).
4. It is a client-fold rule: names are sealed, so the server cannot check it. `CircleFold` computes the founder and the first members from the log and passes them to `MarketFold`; the app's `canConfirmName` uses the same two values. A refusal reads "Only a member at Level 2 or above, a steward, or one of the first 3 members whose access was approved can confirm the founder's identity."

## Consequences

- A new circle's founder can be confirmed as soon as the first member has been approved and has met them in person.
- A founder whose first three members never confirm them stays unconfirmed until a Level 2 member or another steward does.
- Three people can vouch for the founder's identity at Level 1 only for the founder; the risk is small because the founder already controls the circle.
- Tests: `MarketContract` (third member allowed, fourth refused, a first member still cannot confirm others), a real-server test, a view test.
