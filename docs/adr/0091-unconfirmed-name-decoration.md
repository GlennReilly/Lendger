# 0091. The note after an unconfirmed name reads "(unconfirmed)" (wording only)

- Status: Accepted
- Date: 2026-11-16
- Supersedes: nothing except the phrase "(identity not confirmed yet)" in ADR 0089. ADR 0089 and ADR 0071 are not edited.

## Context

ADR 0089 shows a note after a person's name only when nobody has confirmed that person's identity, and the note read "(identity not confirmed yet)". On every unconfirmed line in a members list this is long. The owner asked for the short word "(unconfirmed)".

## Decision

1. The note after a name (`personName`, constant `NAME_NOT_CONFIRMED`) reads "(unconfirmed)". A confirmed name still has nothing after it.
2. Sentences are unchanged: "Nobody has confirmed your identity yet.", "Identity confirmed by Alice Smith.", "You confirmed this member's identity.", and the tick box and button wording of ADR 0071.
3. The note stays plain text in a `span` after the name link, so a screen reader reads "Bea, link, (unconfirmed)". It is not hidden and has no ARIA override.
4. Wording only. Identifiers and behaviour do not change.

## Consequences

- Member, steward and loan lines are shorter.
- "Unconfirmed" on its own is less explicit than the old phrase; the Check identity pages and the sentence on the person's own Home screen still say what is missing.
- View tests and the browser script check the new word.
