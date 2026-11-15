# 0088. Item classes are "value class", steward-managed tags are "category" (wording only)

- Status: Accepted
- Date: 2026-11-15
- Supersedes: nothing. It changes words that ADR 0025, 0085 and 0086 chose; those ADRs are not edited.

## Context

"Kind of item" meant two different things. ADR 0085 used it for the four classes (Essential, Everyday, Valuable, High-value) that decide who may ask and whether the waiting period applies; ADR 0086 used it for the steward-managed tags (Garden, Camping, Kitchen). A member reading "Kind of item: Garden" next to "Kind: Valuable" could not tell which was which. Marketplaces call Garden and Kitchen **categories**, and the four classes describe how much is at stake. The owner chose one word for each.

## Decision

1. The four classes are the **value class** of an item. The item form asks "Value class: "; Circle rules says "What each value class needs" and "Level needed for the everyday value class: " (and likewise for valuable and high-value); the order rule reads "A value class worth more cannot need a lower level than one worth less."
2. The steward-managed tags are **categories**. The Stewards section is "Categories" ("New category: ", "Propose adding this category", "Retired categories: ..."); the item form has a "Categories" group of tick boxes; the Items filter is "Category: " with "All categories"; under an item the line reads "Category: Garden" or "Categories: Garden, Camping"; refusals say "A category's name cannot be empty." and so on.
3. Wording only. Identifiers (`ItemClass`, `TagId`, `tag.change`), event types, payload values, config keys, CSS ids, form field names and localStorage keys keep their names. There is no clean break.
4. A source scan (`StandaloneWordsSourceScanTest`) fails if a string a person reads says "kind" or "kinds" again. Refusal reasons stay full sentences (ADR 0077).

## Consequences

- ADR 0085's "kind of item" is now "value class" and ADR 0086's "Kind of item" tags are now "categories" in everything people read. The rest of both ADRs stands.
- Tests and the browser script check the new words.
- Any new feature about tags (ADR 0087) uses "category".
