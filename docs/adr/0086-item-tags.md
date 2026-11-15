# 0086. Kind-of-item tags: a steward-managed list, picked by owners

- Status: Accepted
- Date: 2026-11-14
- Supersedes: nothing.

## Context

Items have a class (Essential, Everyday, Valuable, High-value) that drives who may ask (ADR 0025, 0085), but nothing says what an item is for: garden, kitchen, camping. Members scanning a long Items list want to narrow it. The owner asked for tags that stewards manage and owners pick, designed minimally and written down before any code.

## Options considered

1. **Free-text tags typed by owners.** Simplest, but every circle ends up with "garden", "Garden", "gardening" and "garden tools"; nobody owns the vocabulary. Rejected: the point is a shared list.
2. **A fixed list in code.** No governance, but a release for every new word and the wrong words for most circles. Rejected.
3. **A circle list changed by any steward alone.** Simple, but with a steward quorum above 1 one person could silently rewrite everyone's categories, which the circle rules and steward changes already avoid. Rejected.
4. **A circle list changed through the same steward vote as the circle rules (ADR 0051).** One sealed event type, each change sent identically by enough stewards (quorum, 14-day window, cool-off), in force from the day it is agreed. Chosen.
5. **Members propose, stewards approve.** Nicer for members but a second kind of vote and a pending-proposals inbox for members; can be added later on top of 4 without a format change. Not now.

## Decision

1. **The list.** A circle has tags. A tag has a random 128-bit hex id (`TagId`), a name of 1 to 24 characters, and is live or retired. At most 30 tags are live. Two live tags cannot share a name once case is ignored, using the existing `NameKey` folding (ADR 0069); a retired tag's name is free again. Names follow the display-name text rules (no control or invisible characters, no leading, trailing or doubled spaces), with the 24-character limit.
2. **Changes are votes.** One new sealed event type, `tag.change`, carries `{tag, op, name?}` with `op` one of `ADD`, `RENAME`, `RETIRE` (`name` is required for the first two and absent for the last). Only stewards may send it. As with `config.update`, a change takes effect when `min(quorum, stewards)` current stewards have sent the identical payload within 14 days of the first; a steward in the cool-off cannot vote when the quorum is above 1. For `ADD` the proposer chooses the id, so the others' "Agree" resends the identical payload. The change is checked against the list as it stands on the day it would take effect (name taken, 30 live tags, unknown or already retired tag) and refused with a full sentence if it does not fit. Judged by log order, like everything else.
3. **Items.** `item.upsert` gets an optional `tags` array of tag ids (0 to 3, distinct, sealed with the rest of the payload). A payload without it reads as no tags; the reader stays strict (unknown keys, wrong types, more than 3, duplicates and malformed ids are refused). The fold accepts a tag id only if the circle has that tag; an id that is retired is accepted only if the item already carried it, so owners cannot add a retired tag but editing other fields keeps it.
4. **Retired tags.** A retired tag disappears from the picker and the filter and cannot be added to an item. Items that already have it keep it and show it as plain text. Retiring never touches item events.
5. **Renaming.** Items refer to the tag by id, so a rename changes the name everywhere at once.
6. **Server.** It accepts `tag.change` opaquely like the other sealed events (`SUPPORTED_TYPES`, and the unsupported-type test now uses another example).
7. **UI.** Stewards page: a "Tags" section listing live tags, with a form to propose adding a tag, and for each live tag a rename form and a retire button; pending proposals are listed with "Agree" and how many stewards agree, like the other proposals. Item form: a "Kinds of item" group of tick boxes (live tags, at most 3; a fourth is refused with a sentence; a retired tag the item already has is listed as plain text). Items page: a "Kind of item" select that narrows the list ("All kinds" by default), the tags shown under each item name, and the existing Items search (a local text filter) also matches tag names.
8. **Wording and style.** Self-explanatory sentences (ADR 0077), "Create" not "Make", member names through `personName`, dates through `DateText`, all labels ending in ": ".
9. **Limits to the log.** A circle can only ever hold 30 live tags, and each change needs a steward vote, so the number of tag events is bounded by steward effort, not by members.

## Consequences

- No clean break: items and logs written before this ADR fold unchanged; they have no tags.
- A tag rename or retirement by vote is permanent history; a retired tag id is never reused.
- One steward alone cannot rewrite categories unless the circle's quorum is 1.
- The item event grows by about 40 bytes per tag (a 32-character id), at most 3.
- Not decided here, and cheap to add later: member proposals, tag colours or icons, tag-based access rules.
