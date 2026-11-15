package circles.event

import kotlin.jvm.JvmInline

/** Category tags (ADR 0086, called categories in the UI by ADR 0088): a circle's list of words, kept by steward vote and picked by owners. */
@JvmInline
value class TagId(val v: String)

const val MAX_TAG_NAME_CHARS = 24
const val MAX_LIVE_TAGS = 30
const val MAX_TAGS_PER_ITEM = 3

enum class TagOp { ADD, RENAME, RETIRE }

/** `tag.change`: one steward's vote for a change to the list. [name] is set for ADD and RENAME and absent for RETIRE. */
data class TagChange(val tag: TagId, val op: TagOp, val name: String?)

/** A tag as the fold holds it. A retired tag is not offered any more but stays on the items that already have it. */
data class TagState(val tag: TagId, val name: String, val retired: Boolean, val addedDay: Int)

/** A tag change with some stewards' votes but not yet enough. */
data class PendingTag(val change: TagChange, val voters: Set<circles.policy.MemberId>, val firstDay: Int)

const val TAG_TOO_MANY_PER_ITEM = "An item can have at most $MAX_TAGS_PER_ITEM categories."
const val TAG_NAME_TAKEN = "A category with that name already exists."
const val TAG_LIMIT_REACHED = "The circle already has $MAX_LIVE_TAGS categories. Retire one first."

/** The rules for a tag's name (ADR 0086): 1 to 24 characters, the display-name text rules, no leading, trailing or doubled spaces. */
object TagNames {
    private const val SPACE = ' '

    /** Why [name] is not acceptable as stored, or null. Code-point based, so the JVM and the browser agree. */
    fun problem(name: String): String? {
        val points = DisplayNames.codePoints(name) ?: return "A category's name must be valid text."
        return when {
            points.isEmpty() -> "A category's name cannot be empty."
            points.size > MAX_TAG_NAME_CHARS -> "A category's name can have at most $MAX_TAG_NAME_CHARS characters."
            points.any { DisplayNames.isControl(it) || it in DisplayNames.FORBIDDEN } -> "A category's name cannot hold control characters, invisible marks or unusual spaces."
            points.first() == SPACE.code || points.last() == SPACE.code -> "A category's name cannot start or end with a space."
            name.contains("$SPACE$SPACE") -> "A category's name cannot hold two spaces in a row."
            else -> null
        }
    }
}

// ---- member suggestions (ADR 0087) ----

const val MAX_SUGGESTION_NOTE_CHARS = 140
const val MAX_DECLINE_REASON_CHARS = 140

/** A member may send this many suggestions in any [SUGGESTION_WINDOW_DAYS] days. */
const val MAX_SUGGESTIONS_PER_WINDOW = 3
const val SUGGESTION_WINDOW_DAYS = 30

/** A suggestion nobody answers lapses this many days after it was sent. */
const val SUGGESTION_LAPSE_DAYS = 30

/** `tag.suggest`: a member asks for a new category. [tag] is a fresh id; a steward's `tag.change` ADD with the same id approves it. */
data class TagSuggest(val tag: TagId, val name: String, val note: String)

/** `tag.decline`: one steward closes a suggestion with a reason. */
data class TagDecline(val tag: TagId, val reason: String)

enum class SuggestionStatus { WAITING, ADDED, DECLINED, LAPSED }

/** A suggestion as the fold holds it. Its status depends on the list and on the day, so it is worked out by [MarketView.suggestionStatus]. */
data class TagSuggestion(
    val tag: TagId,
    val suggester: circles.policy.MemberId,
    val name: String,
    val note: String,
    val sentDay: Int,
    val declinedBy: circles.policy.MemberId? = null,
    val declinedDay: Int? = null,
    val declineReason: String? = null,
) {
    /** Open for an answer on [day]: not declined and not lapsed (the list is checked separately). */
    fun openOn(day: Int): Boolean = declinedDay == null && day - sentDay < SUGGESTION_LAPSE_DAYS
}
