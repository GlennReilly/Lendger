# 0096. Open proposals show their closing day

- Status: Accepted
- Date: 2026-11-16
- Supersedes: only point 6 of ADR 0095, where it says the Stewards page shows no "days left" line. The rest of ADR 0095 stands; it is not edited.

## Context

ADR 0095 made every steward vote (`steward.add`, `steward.remove`, `steward.policy`, `config.update`, `tag.change`) count within 14 days of the first vote, and its point 6 recorded that the Stewards page showed no "days left" line, so no wording changed. The owner then asked for each open proposal to say when it closes, and commit 7ab1930 added that line. A review of the rules reference document found ADR 0095 point 6 no longer matched the code.

## Decision

1. Every open proposal on the Stewards page (steward changes, circle rules, categories) shows its closing day under its "N of M stewards agree." line: "Open until <date>. If not enough stewards agree by then, the proposal lapses.", and on the last day "Last day for stewards to agree (<date>)."
2. The date is `ProposalWindow.lastDay` (the first vote's day plus 14), carried as `lastDay` on `ProposalSummary`, `RulesProposal` and `TagProposalSummary`; the sentence comes from one helper, `proposalClosesLine(lastDay, today)` in `ShellView.kt`, and dates go through `DateText` (ADR 0080).
3. Members' category suggestions (ADR 0087) have no vote window and show no closing line; they lapse after 30 days.

## Consequences

- Stewards can see how long a proposal stays open without counting days.
- A UI-only addition: no event, server or fold change.
- Tests: `ShellTest` (each proposal kind and the last-day form), `CircleAppEndToEndTest` (each `lastDay` equals the first vote's day plus 14), the browser script (a category proposal shows the line; axe clean light and dark).
