# 0092. The "(unconfirmed)" note is a link to where the identity is confirmed

- Status: Accepted
- Date: 2026-11-16
- Supersedes: only point 3 of ADR 0091 (the note is a plain `span` with no ARIA override). ADR 0091 is not edited; its wording "(unconfirmed)" stays.

## Context

ADR 0091 made the note after an unconfirmed name a plain word. The owner wants the word to be the way to fix what it points out: tapping "(unconfirmed)" should take the person to the page where the identity is confirmed.

## Decision

1. The note keeps the visible text "(unconfirmed)" but is now a link (`a.name-status.unconfirmed`, built in `personName`). A confirmed name still has no note and no link.
2. For another member it opens the Check identity in person page (`Screen.CheckIdentity`, `#/members/check`) for that member, the same page the person page's "Check identity in person" button opens. It is shown whether or not the viewer may confirm; that page offers Confirm identity only when it is usable (ADR 0078). The link carries `data-action="check-person"` and `data-member`, so the page's click handler sets the check state first.
3. For the viewer's own name it opens Members, where "Show my QR code" and the characters to read out live (the check page says "That is you" for yourself, so it is no use there).
4. Accessible names are set with `aria-label`, and each starts with the visible word so the visible text is part of the name: "Unconfirmed: check Bea's identity" and "Unconfirmed: show others who you are". Two links side by side therefore have different names. The name in the label is plain text (`plain-name:` comment), because an attribute cannot hold a link.
5. The link has the same 44 px tap area as `a.person-name`.

## Consequences

- One tap from an unconfirmed name to the place to confirm it.
- Without JavaScript the link lands on the check page, which says the person is not found; the app needs JavaScript anyway.
- View tests check both link targets and that a confirmed name has none; the browser script clicks one and checks the check page opens; axe and the keyboard walk cover the new links.
