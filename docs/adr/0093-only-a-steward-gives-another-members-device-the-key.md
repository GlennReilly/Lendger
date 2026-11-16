# 0093. Only a steward can give another member's device the circle key

- Status: Accepted
- Date: 2026-11-16
- Supersedes: nothing. It turns a convention of the app (ADR 0021, 0071, 0081: a steward approves access) into a rule the server and every client fold enforce.

## Context

A newcomer who joins with an invite (ADR 0040) can read nothing until someone wraps the circle key to their device with a `key.wrap`. The app offers that act, "Approve access", only to stewards (`CircleApp.admit`), after an in-person check (ADR 0071). But once a circle had its first key, the server (`EventRules.keyWrap`) and the client fold (`CircleFold.wrap`) accepted a `key.wrap` from any live device of any member. A member with a modified client could therefore give the circle key to anyone holding an invite, skipping the steward's check. The owner decided on 2026-11-16 that approving access is a steward's act and must be enforced.

Every place that sends a `key.wrap` today was checked:

- The founder's first wrap to their own device (`startCircle`): sender and target are the same member, and the first key already needs a steward.
- Approve access (`admit`): a steward's wrap to another member's device.
- Adding a second device (`addDevice`, ADR 0046) and restoring from a recovery code (`confirmRecoveryCode`, `restoreFromCode`, ADR 0047): a wrap of every held epoch from one of the member's devices (the recovery device included) to another device of the same member.
- Rebuilding a key from recovery shares (`rebuildKey`, ADR 0048): a wrap to the steward's own device.
- The wraps that follow a `key.rotate` (`EventAuthor.rotateKeys`, used by `removeMember`, `rotateKeyNow`, `removeDevice`, `confirmRecoveryCode` and `restoreFromCode`): wraps to every remaining device, including other members' devices. Only a steward may send `key.rotate` today (server `EventRules.keyRotate` and `CircleFold.rotate`), and every app path that rotates checks that the member is a steward first, so these wraps always come from a steward.

## Decision

1. A `key.wrap` whose target device belongs to a different member than the sender's member is accepted only if the sender is a steward at that point in the log (the steward set as it stands when the event is judged, after every earlier event).
2. A wrap to another device of the sender's own member stays open to every member: a second device, a restore and a rebuild keep working for members who are not stewards.
3. The wraps that accompany a rotation need no rule of their own. Because only a steward may rotate, the simple rule of point 1 already accepts them; a "same batch as the sender's own `key.rotate`" exception was considered and not needed. If a later decision lets non-stewards rotate, that decision must revisit this rule.
4. Both the server and the client fold enforce the same rule, with the shared constant `ANOTHER_MEMBERS_WRAP_REASON` ("only a steward may give another member's device the circle key"). The server refuses with the existing `RejectionCode.NOT_STEWARD` (no new wire code, so no API change); the fold lists the event as rejected, and the target device does not learn the key from it.
5. `recovery.share` (already steward-to-steward only, ADR 0048) and item key wraps inside `item.upsert` and `loan.respond` (ADR 0025, 0049) are different events and are unchanged.
6. ADR 0090's order of first members already counts only a steward's first wrap to a member's device, so it is unaffected.

## Consequences

- A modified client of a member who is not a steward can no longer approve anyone's access, on the server or in any other member's fold.
- A steward who steps down cannot approve access with an event they had queued but not yet sent: it is judged at its place in the log and refused. The app checks the steward role before it queues an approval, so this only happens in a race with the stepping down.
- Logs written before this rule that contain a non-steward's wrap to another member now fold with that wrap rejected, and the target may lose the key it gave. No live circle exists yet, and the app never produced such a wrap.
- Tests: `CircleFoldContract` (a member's wrap to another member is rejected, the steward's is accepted, own-device wraps are accepted, the rule follows stepping down, a steward's rotation wraps reach every member; JVM and JS), `SealedFlowTest` on the server (refused with `not_steward`, own device accepted, rotation accepted), and a real-server test in which a hand-built wrap from a member who is not a steward to a waiting newcomer is refused while the steward's approval still works. The existing real-server tests for a second device, restore from a recovery code, removal with rotation and rebuilding from recovery shares still pass.
