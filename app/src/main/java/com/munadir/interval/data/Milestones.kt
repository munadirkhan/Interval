package com.munadir.interval.data

/**
 * An XP milestone.
 *
 * The ladder exists because XP with nothing behind it is just a number going up. Most of these
 * are titles, which cost nothing and are the honest kind of reward. The last one is a joke, but
 * a joke the developer can actually honour — at ten to twenty XP per correct answer, a million
 * XP is somewhere north of fifty thousand cards. Nobody is getting lunch.
 */
data class Milestone(
    val xp: Int,
    val title: String,
    val blurb: String,
    /** Marks the punchline tier, which the UI presents differently. */
    val isTheBit: Boolean = false
)

object Milestones {

    val ALL = listOf(
        Milestone(
            xp = 50,
            title = "Showed Up",
            blurb = "You reviewed something. That is already more than most people manage."
        ),
        Milestone(
            xp = 250,
            title = "Regular",
            blurb = "The app knows your face now. It has no feelings about this. Probably."
        ),
        Milestone(
            xp = 1_000,
            title = "Committed",
            blurb = "A thousand points. Your brain is measurably different than it was."
        ),
        Milestone(
            xp = 5_000,
            title = "Scholar",
            blurb = "You could have learned a language in this time. You may in fact be doing that."
        ),
        Milestone(
            xp = 25_000,
            title = "Unreasonable",
            blurb = "That is roughly two thousand correct answers. We did the arithmetic so you don't have to."
        ),
        Milestone(
            xp = 100_000,
            title = "Concerning",
            blurb = "Please go outside. You may bring the flashcards."
        ),
        Milestone(
            xp = 1_000_000,
            title = "Lunch Is On Us",
            blurb = "Hit a million and the developer will genuinely buy you a five dollar lunch. " +
                "Anywhere you like. Just email him. Nobody has ever emailed him.",
            isTheBit = true
        )
    )

    fun unlocked(totalXp: Int): List<Milestone> = ALL.filter { totalXp >= it.xp }

    fun next(totalXp: Int): Milestone? = ALL.firstOrNull { totalXp < it.xp }

    /** 0f..1f progress from the previous milestone to the next one. */
    fun progressToNext(totalXp: Int): Float {
        val next = next(totalXp) ?: return 1f
        val previous = ALL.lastOrNull { totalXp >= it.xp }?.xp ?: 0
        val span = (next.xp - previous).coerceAtLeast(1)
        return ((totalXp - previous).toFloat() / span).coerceIn(0f, 1f)
    }

    /** Any milestone crossed by earning [earned] XP, so a session can celebrate it. */
    fun crossedBy(totalXp: Int, earned: Int): Milestone? {
        val before = totalXp - earned
        return ALL.firstOrNull { it.xp in (before + 1)..totalXp }
    }
}
