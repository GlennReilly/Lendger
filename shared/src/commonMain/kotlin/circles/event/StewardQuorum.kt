package circles.event

import circles.policy.MemberId

/** Limits and defaults of the steward settings (ADR 0027). The minimums need a new ADR to change. */
const val MIN_STEWARD_QUORUM = 1
const val MAX_STEWARD_QUORUM = 9
const val DEFAULT_STEWARD_QUORUM = 2
const val MIN_STEWARD_INACTIVE_DAYS = 7
const val MAX_STEWARD_INACTIVE_DAYS = 365
const val DEFAULT_STEWARD_INACTIVE_DAYS = 60
const val MIN_STEWARD_FALLBACK_DELAY_DAYS = 7
const val MAX_STEWARD_FALLBACK_DELAY_DAYS = 90
const val DEFAULT_STEWARD_FALLBACK_DELAY_DAYS = 14

/** Recovery shares (ADR 0048): 0 means none; otherwise this many stewards must come together to rebuild the key. */
const val NO_RECOVERY_SHARES = 0
const val MIN_STEWARD_RECOVERY_THRESHOLD = 2
const val MAX_STEWARD_RECOVERY_THRESHOLD = 9

/**
 * Fixed policy, not settings: how many days after a proposal's first vote later votes still count (ADR 0095),
 * and how long a new steward waits before voting.
 */
const val DEFAULT_STEWARD_PROPOSAL_DAYS = 14
const val DEFAULT_STEWARD_COOL_OFF_DAYS = 7

/** The `addedDay` of a steward present at `circle.create`: far enough in the past that cooling-off never applies. */
const val FOUNDING_STEWARD_DAY = -1_000_000

/** The governance settings every circle has, set at creation and changed by `steward.policy` (ADR 0027, 0048). */
data class StewardSettings(
    val quorum: Int = DEFAULT_STEWARD_QUORUM,
    val inactiveDays: Int = DEFAULT_STEWARD_INACTIVE_DAYS,
    val fallbackDelayDays: Int = DEFAULT_STEWARD_FALLBACK_DELAY_DAYS,
    val recoveryThreshold: Int = NO_RECOVERY_SHARES,
) {
    /** Why these values are not allowed, or null if they are. */
    fun problem(): String? = when {
        quorum !in MIN_STEWARD_QUORUM..MAX_STEWARD_QUORUM -> "The number of stewards who must agree can be from $MIN_STEWARD_QUORUM to $MAX_STEWARD_QUORUM."
        inactiveDays !in MIN_STEWARD_INACTIVE_DAYS..MAX_STEWARD_INACTIVE_DAYS ->
            "A steward must be quiet for $MIN_STEWARD_INACTIVE_DAYS to $MAX_STEWARD_INACTIVE_DAYS days before the others may act without them."
        fallbackDelayDays !in MIN_STEWARD_FALLBACK_DELAY_DAYS..MAX_STEWARD_FALLBACK_DELAY_DAYS ->
            "The others must wait $MIN_STEWARD_FALLBACK_DELAY_DAYS to $MAX_STEWARD_FALLBACK_DELAY_DAYS days."
        recoveryThreshold != NO_RECOVERY_SHARES && recoveryThreshold !in MIN_STEWARD_RECOVERY_THRESHOLD..MAX_STEWARD_RECOVERY_THRESHOLD ->
            "The number of stewards needed to rebuild the key must be $NO_RECOVERY_SHARES or from $MIN_STEWARD_RECOVERY_THRESHOLD to $MAX_STEWARD_RECOVERY_THRESHOLD."
        else -> null
    }

    /** Names this exact set of values, so votes for different proposals are never mixed. The fourth part is left out when it is 0. */
    fun subject(): String = "$quorum:$inactiveDays:$fallbackDelayDays" + if (recoveryThreshold == NO_RECOVERY_SHARES) "" else ":$recoveryThreshold"

    companion object {
        /** The settings a [subject] string names, or null if it is not one. */
        fun fromSubject(subject: String): StewardSettings? {
            val parts = subject.split(':').map { it.toIntOrNull() ?: return null }
            return when (parts.size) {
                3 -> StewardSettings(parts[0], parts[1], parts[2])
                4 -> StewardSettings(parts[0], parts[1], parts[2], parts[3])
                else -> null
            }
        }
    }
}

/** The numbers behind ADR 0027. Both the server and the client fold take one, so they cannot drift apart. */
data class StewardPolicy(
    val quorum: Int = DEFAULT_STEWARD_QUORUM,
    val proposalDays: Int = DEFAULT_STEWARD_PROPOSAL_DAYS,
    val coolOffDays: Int = DEFAULT_STEWARD_COOL_OFF_DAYS,
    val inactiveDays: Int = DEFAULT_STEWARD_INACTIVE_DAYS,
    val fallbackDelayDays: Int = DEFAULT_STEWARD_FALLBACK_DELAY_DAYS,
    val recoveryThreshold: Int = NO_RECOVERY_SHARES,
) {
    init {
        require(settings.problem() == null) { settings.problem().orEmpty() }
        require(proposalDays >= 0 && coolOffDays >= 0) { "days must not be negative" }
    }

    val settings: StewardSettings get() = StewardSettings(quorum, inactiveDays, fallbackDelayDays, recoveryThreshold)

    /** This policy with a circle's own settings. */
    fun with(settings: StewardSettings): StewardPolicy =
        copy(quorum = settings.quorum, inactiveDays = settings.inactiveDays, fallbackDelayDays = settings.fallbackDelayDays, recoveryThreshold = settings.recoveryThreshold)
}

enum class StewardChange { ADD, REMOVE, POLICY }

/**
 * The one window rule for every steward vote (ADR 0095): `steward.add`, `steward.remove`, `steward.policy`,
 * `config.update` and `tag.change`. A proposal opens with its first vote; a later vote joins it while the later vote's
 * day is no more than [DEFAULT_STEWARD_PROPOSAL_DAYS] after that first day (the last day is included). A vote after
 * that starts a new proposal, with itself as the first vote, and the earlier votes no longer count.
 */
object ProposalWindow {
    /** True while a vote on [day] still joins the proposal whose first vote was on [firstDay]. */
    fun isOpen(firstDay: Int, day: Int, windowDays: Int = DEFAULT_STEWARD_PROPOSAL_DAYS): Boolean = day <= firstDay + windowDays

    /** The last day on which a vote still joins the proposal whose first vote was on [firstDay]. */
    fun lastDay(firstDay: Int, windowDays: Int = DEFAULT_STEWARD_PROPOSAL_DAYS): Int = firstDay + windowDays
}

/**
 * One steward's `steward.add`, `steward.remove` or `steward.policy` for a subject, on its signed [day].
 * [position] orders events in the log (`serverSeq` on the server, index in a client's log).
 */
data class StewardVote(val voter: MemberId, val day: Int, val position: Long = 0)

/** What the log says about a steward's activity; the inactivity fallback is judged from it (ADR 0027). */
interface StewardActivity {
    /** The day of the latest event [member] authored before [position], or null if there is none. */
    fun lastEventDayBefore(member: MemberId, position: Long): Int?

    /** True if [member] authored any event after [position]. */
    fun hasEventAfter(member: MemberId, position: Long): Boolean
}

/** What one steward event does (ADR 0027). */
sealed interface StewardOutcome {
    /** Enough distinct stewards agree, or the fallback applies: apply the change now. */
    data object Effective : StewardOutcome

    /** Valid, recorded, and waiting for more stewards. */
    data object Pending : StewardOutcome

    data class Refused(val reason: String) : StewardOutcome
}

/** The rules of ADR 0027 as pure functions, shared by the server and every client. */
object StewardRules {
    /**
     * Judges [voter]'s `steward.add` or `steward.remove` for [target] on [day].
     * [stewards] maps each current steward to the day they became one ([FOUNDING_STEWARD_DAY] for founders);
     * [priorVotes] are earlier events for the same change and target. Votes are counted by distinct member.
     * [activity] enables the inactivity fallback; without it a change needs its quorum.
     */
    fun decide(
        policy: StewardPolicy,
        change: StewardChange,
        target: MemberId,
        voter: MemberId,
        day: Int,
        stewards: Map<MemberId, Int>,
        priorVotes: List<StewardVote>,
        activity: StewardActivity? = null,
    ): StewardOutcome {
        require(change != StewardChange.POLICY) { "use decidePolicy for steward.policy" }
        val leaving = change == StewardChange.REMOVE && voter == target
        refusalForVoter(policy, voter, day, stewards, leaving)?.let { return it }
        val required = when (change) {
            StewardChange.ADD -> {
                if (target in stewards) return StewardOutcome.Refused("That member is already a steward.")
                minOf(policy.quorum, stewards.size)
            }
            else -> {
                if (target !in stewards) return StewardOutcome.Refused("That member is not a steward.")
                if (stewards.size == 1) return StewardOutcome.Refused("A circle must keep at least one steward.")
                if (leaving) return StewardOutcome.Effective // a steward may always leave while another remains
                policy.quorum
            }
        }
        return count(policy, voter, day, stewards, priorVotes, required, excluded = target.takeIf { change == StewardChange.REMOVE }, activity = activity)
    }

    /** Judges [voter]'s `steward.policy` on [day]. The inactivity fallback never applies to it. */
    fun decidePolicy(
        policy: StewardPolicy,
        proposed: StewardSettings,
        voter: MemberId,
        day: Int,
        stewards: Map<MemberId, Int>,
        priorVotes: List<StewardVote>,
    ): StewardOutcome {
        proposed.problem()?.let { return StewardOutcome.Refused(it) }
        refusalForVoter(policy, voter, day, stewards, leaving = false)?.let { return it }
        if (proposed == policy.settings) return StewardOutcome.Refused("Those are already the circle's steward settings.")
        return count(policy, voter, day, stewards, priorVotes, minOf(policy.quorum, stewards.size), excluded = null, activity = null)
    }

    private fun refusalForVoter(policy: StewardPolicy, voter: MemberId, day: Int, stewards: Map<MemberId, Int>, leaving: Boolean): StewardOutcome? {
        val since = stewards[voter] ?: return StewardOutcome.Refused("Only a steward can change who the stewards are.")
        // Leaving is a right, not a vote, and a quorum of one has no one to wait for, so cool-off never delays either.
        val coolOff = if (policy.quorum == 1 || leaving) 0 else policy.coolOffDays
        return if (day < since + coolOff) StewardOutcome.Refused("A new steward cannot vote until ${circles.app.ReasonDates.token(since + coolOff)}.") else null
    }

    private fun count(
        policy: StewardPolicy,
        voter: MemberId,
        day: Int,
        stewards: Map<MemberId, Int>,
        priorVotes: List<StewardVote>,
        required: Int,
        excluded: MemberId?,
        activity: StewardActivity?,
    ): StewardOutcome {
        val proposal = currentProposal(priorVotes + StewardVote(voter, day, Long.MAX_VALUE), policy.proposalDays)
        val counted = proposal.filter { it.voter in stewards && it.voter != excluded }.map { it.voter }.toSet()
        if (counted.size >= required) return StewardOutcome.Effective
        if (activity != null && required >= 2 && fallbackHolds(policy, voter, day, stewards, priorVotes, activity)) return StewardOutcome.Effective
        return StewardOutcome.Pending
    }

    /**
     * The votes of the proposal that is open after [votes], taken in log order ([StewardVote.position]); ADR 0095.
     * A vote outside the open proposal's window starts a new proposal and the earlier votes are dropped, exactly as
     * `config.update` and `tag.change` votes are handled in `MarketFold`. Empty when [votes] is empty.
     */
    fun currentProposal(votes: List<StewardVote>, windowDays: Int = DEFAULT_STEWARD_PROPOSAL_DAYS): List<StewardVote> {
        var proposal = emptyList<StewardVote>()
        for (vote in votes.sortedBy { it.position }) {
            val first = proposal.firstOrNull()
            proposal = if (first != null && ProposalWindow.isOpen(first.day, vote.day, windowDays)) proposal + vote else listOf(vote)
        }
        return proposal
    }

    /**
     * The inactivity fallback (ADR 0027 point 6): the voter's earliest matching event is old enough, every other steward
     * was silent for long enough before it, and none of them has sent anything since. The earliest event is taken from
     * every vote recorded since the subject was last decided, not only the open proposal (ADR 0095): the fallback is
     * not a vote count, and its delay may be longer than the proposal window.
     */
    private fun fallbackHolds(
        policy: StewardPolicy,
        voter: MemberId,
        day: Int,
        stewards: Map<MemberId, Int>,
        priorVotes: List<StewardVote>,
        activity: StewardActivity,
    ): Boolean {
        val earliest = priorVotes.filter { it.voter == voter }.minByOrNull { it.position } ?: return false
        if (day - earliest.day < policy.fallbackDelayDays) return false
        val others = stewards.keys - voter
        if (others.isEmpty()) return false
        return others.all { other ->
            val quietSince = activity.lastEventDayBefore(other, earliest.position) ?: stewards.getValue(other)
            earliest.day - quietSince >= policy.inactiveDays && !activity.hasEventAfter(other, earliest.position)
        }
    }
}
