# 0089. A confirmed name shows nothing extra; only "not confirmed yet" is said (wording only)

- Status: Accepted
- Date: 2026-11-16
- Supersedes: nothing. It changes words that ADR 0071 chose; that ADR is not edited.

## Context

ADR 0071 put "(identity confirmed)" or "(identity not confirmed yet)" after every person's name. Once most members are confirmed, the first phrase repeats on every line and hides the one case that needs attention.

## Decision

1. A person's name (`personName`) is followed by "(identity not confirmed yet)" only when nobody has confirmed it. A confirmed name has nothing after it.
2. Sentences about confirmation that are not name decorations stay: "Identity confirmed by Alice Smith." on the person page, "You confirmed this member's identity.", "Nobody has confirmed your identity yet."
3. Wording only. The confirmation rules, the steward rules and every identifier keep their behaviour; the constant `NAME_CONFIRMED` is removed.

## Consequences

- Lists of members, stewards and loans are shorter; an unconfirmed name stands out.
- A person who sees no note cannot tell "confirmed" from "no data"; the person page still says who confirmed.
- View tests and the browser script check that no "(identity confirmed)" appears.
