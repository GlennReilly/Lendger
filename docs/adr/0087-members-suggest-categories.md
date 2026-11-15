# 0087. Members suggest new categories; stewards approve or decline

- Status: Accepted
- Date: 2026-11-15
- Supersedes: nothing. Builds on ADR 0086 (option 5 there, "not now"). Wording follows ADR 0088 ("category").

## Context

ADR 0086 gave the circle a list of categories (tags such as Garden or Camping) that only stewards change, by the same vote as the circle rules (ADR 0051). The people who notice a missing category are usually owners filling in the item form, and they could only tell a steward outside the app. ADR 0086 said member proposals could be added on top without a format change. This ADR decides how.

Constraints: the list stays steward-controlled (a member's suggestion never changes it by itself); members must not be able to flood stewards or the log; messages follow ADR 0077 (full sentences that name their subject); one topic per screen; no clean break.

Every sealed event can be read by every member's device. "Who sees a suggestion" below is about what the pages show, as for pauses (ADR 0050), not about secrecy.

## Options considered

1. **No change in the app.** Nothing to build; suggestions get lost and the suggester never hears back.
2. **Members send `tag.change ADD` too.** One event type, but a suggestion and a vote look the same in the log, declining still needs a new event, and the 14-day vote window would become the suggestion's life.
3. **A new sealed `tag.suggest` event; approving is the ADR 0086 vote.** A suggestion carries a fresh tag id; a steward approves by sending the ordinary `tag.change ADD` with that same id, so the fold links the two by id and `tag.change` is unchanged. Declining is a second small event, `tag.decline`. **Chosen (B1).**
4. **Owners tag items with a new word at once, pending approval.** Items would carry tags that may never exist. Rejected.

## Decision

The owner chose A1, B1, C1, D1 and E1 on 2026-11-15.

1. **Who may suggest (A1).** Any current member whose access has been approved, Level 1 included.
2. **Suggesting (B1).** New sealed event `tag.suggest {tag, name, note}`: `tag` is a new random `TagId` chosen by the suggester, `name` follows the tag-name rules (`TagNames.problem`, 1 to 24 characters), `note` is 0 to 140 characters. The fold refuses it, with a full sentence, if the author is not a current member, if a live category already has that name ignoring case (`NameKey`), if an open suggestion already has that name, if the id is already used, or if the author is over the limit (point 5). It never changes the list.
3. **Approving.** On the Stewards page each waiting suggestion has "Agree to add this category", which sends `tag.change {tag = the suggestion's id, op = ADD, name}`: exactly the ADR 0086 vote, same quorum, 14-day window and cool-off. A steward may correct the spelling first ("Add with a different name"); the id stays the same, so the link holds. When the ADD takes effect the suggestion is "Added".
4. **Declining (C1).** New sealed event `tag.decline {tag, reason}`, sent by one steward, with a required reason of 1 to 140 characters. It does not need the quorum, because it changes nothing in the list. It is refused if the suggestion is unknown, already added, already declined or lapsed. Any ADD votes already sent for that id stay an ordinary steward proposal; if one completes anyway, the suggestion shows as Added (added wins).
5. **Limit (D1).** At most 3 suggestions per member in any 30 days (the same number as contribution notes, ADR 0053); the refusal names the day the next one can be sent (`ReasonDates.token`, ADR 0082). A suggestion nobody answers lapses 30 days after it was sent and no longer blocks the same name.
6. **Status, worked out by the fold for each suggestion on a given day:** Added (the id is in the list, retired or not), else Declined (with the reason), else Lapsed, else Waiting. Suggestions from someone who has left are not shown.
7. **Who sees it (E1).** The pages show a suggestion only to the suggester and to stewards. A decline stays a quiet answer.
8. **Pages.**
   - Items page and the item form (under the category tick boxes): a link "Suggest a new category" to the sub page `#/items/suggest-category` (one topic: name, optional note, submit, and "Your suggestions" with each status in a full sentence, including the decline reason). The link is hidden for a member at the limit; the page says when they can suggest again.
   - Stewards page, "Categories" section: "Suggested by members" above the pending changes; each waiting suggestion shows the suggester (`personName`), the note, the date (`DateText`), and the actions Agree, "Add with a different name" and Decline (reason, in a disclosure).
   - Home: a line for stewards only, "N categories suggested by members are waiting for an answer", linking to the Stewards page.
9. **Server.** Accepts `tag.suggest` and `tag.decline` opaquely (`SUPPORTED_TYPES`). Both `EventType` entries are added at the end of the enum.

## Consequences

- No clean break: `tag.change` and `item.upsert` are unchanged, and logs written before this ADR fold as before (they have no suggestions).
- Two new sealed event types with strict payload readers and `EventAuthor` builders; fold state `TagSuggestion` with `MarketView.suggestionStatus`; app calls `suggestTag` and `declineSuggestion` (approving reuses `proposeTag`); one sub page, one Stewards block and a Home line.
- The log stays bounded by member count (at most 3 suggestions and the matching declines per member per 30 days).
- A suggester cannot tick the suggested category on an item until it is added; they edit the item afterwards.
- A declined id is never reused. The same name can be suggested again after a decline or lapse (within the limit).
- The pre-check for a suggestion or a decline is the fold itself (enqueue, fold, drop if refused), like pauses.
- Not decided here: members supporting ("+1") a suggestion, suggesting renames or retirements, notifications.
