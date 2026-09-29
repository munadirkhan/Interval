package com.munadir.interval.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** How a card is asked. The scheduler treats all three identically — only the UI differs. */
enum class CardType {
    /** Front, tap to reveal, grade yourself. */
    FLIP,

    /** Four options, one correct. Graded automatically. */
    MULTIPLE_CHOICE,

    /** A statement that is either true or false. */
    TRUE_FALSE;

    val label: String
        get() = when (this) {
            FLIP -> "Flashcard"
            MULTIPLE_CHOICE -> "Multiple choice"
            TRUE_FALSE -> "True / false"
        }

    companion object {
        fun from(name: String?): CardType =
            entries.firstOrNull { it.name == name } ?: FLIP
    }
}

/**
 * One thing you are trying to remember.
 *
 * [stage] indexes into [INTERVALS]. Recalling correctly moves the card one rung up the ladder;
 * forgetting drops it back to the bottom. That widening gap is the whole idea behind spaced
 * repetition -- you see the material right before you would have forgotten it.
 *
 * All three [CardType]s share this one model rather than each getting a subclass. A quiz card
 * is still just a prompt, an answer, and a position on the ladder; [choices] is the only extra
 * state, and keeping it flat means the store, the scheduler and the stats never branch on type.
 */
data class Card(
    val id: Long,
    val front: String,
    val back: String,
    /** Flat, optional. Empty string means the card is unfiled. */
    val deck: String = "",
    val stage: Int = 0,
    val dueAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val type: CardType = CardType.FLIP,
    /** Options for MULTIPLE_CHOICE and TRUE_FALSE. Empty for FLIP. */
    val choices: List<String> = emptyList(),
    /** Index into [choices] of the correct option. */
    val correctIndex: Int = 0
) {
    val isDue: Boolean get() = dueAt <= System.currentTimeMillis()

    /** True when the app can mark this itself, rather than asking the user to self-grade. */
    val isAutoGraded: Boolean get() = type != CardType.FLIP && choices.isNotEmpty()

    val correctAnswer: String? get() = choices.getOrNull(correctIndex)

    /** Correct recall: up one rung, capped at the top. */
    fun promoted(): Card {
        val next = (stage + 1).coerceAtMost(INTERVALS.lastIndex)
        return copy(stage = next, dueAt = System.currentTimeMillis() + delayFor(next))
    }

    /** Forgotten: back to the bottom, so it returns tomorrow. */
    fun reset(): Card = copy(stage = 0, dueAt = System.currentTimeMillis() + delayFor(0))

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("front", front)
        put("back", back)
        put("deck", deck)
        put("stage", stage)
        put("dueAt", dueAt)
        put("createdAt", createdAt)
        put("type", type.name)
        put("choices", JSONArray().also { arr -> choices.forEach { arr.put(it) } })
        put("correctIndex", correctIndex)
    }

    companion object {
        /** Days between reviews at each rung. */
        val INTERVALS = intArrayOf(1, 3, 7, 30, 90)

        fun delayFor(stage: Int): Long =
            TimeUnit.DAYS.toMillis(INTERVALS[stage.coerceIn(0, INTERVALS.lastIndex)].toLong())

        /** Human label for a rung. Goes on the grade buttons. */
        fun labelFor(stage: Int): String =
            when (val d = INTERVALS[stage.coerceIn(0, INTERVALS.lastIndex)]) {
                1 -> "tomorrow"
                else -> "$d days"
            }

        /** What "Got it" would do to a card sitting at [stage] right now. */
        fun nextLabelAfterPass(stage: Int): String =
            labelFor((stage + 1).coerceAtMost(INTERVALS.lastIndex))

        fun fromJson(o: JSONObject): Card {
            val choicesArray = o.optJSONArray("choices")
            val choices = if (choicesArray == null) emptyList()
            else (0 until choicesArray.length()).map { choicesArray.getString(it) }

            return Card(
                id = o.getLong("id"),
                front = o.getString("front"),
                back = o.getString("back"),
                deck = o.optString("deck", ""),
                stage = o.optInt("stage", 0),
                dueAt = o.optLong("dueAt", System.currentTimeMillis()),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                // Cards written before quiz types existed have no "type" key and are flashcards.
                type = CardType.from(o.optString("type", CardType.FLIP.name)),
                choices = choices,
                correctIndex = o.optInt("correctIndex", 0)
            )
        }

        fun trueFalse(
            id: Long,
            statement: String,
            isTrue: Boolean,
            explanation: String,
            deck: String = ""
        ) = Card(
            id = id,
            front = statement,
            back = explanation,
            deck = deck,
            type = CardType.TRUE_FALSE,
            choices = listOf("True", "False"),
            correctIndex = if (isTrue) 0 else 1
        )
    }
}

/** One grade, appended forever. Every statistic in the app is derived from this log. */
data class ReviewEvent(
    val cardId: Long,
    val timestamp: Long,
    val gotIt: Boolean
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("cardId", cardId)
        put("timestamp", timestamp)
        put("gotIt", gotIt)
    }

    companion object {
        fun fromJson(o: JSONObject) = ReviewEvent(
            cardId = o.getLong("cardId"),
            timestamp = o.getLong("timestamp"),
            gotIt = o.getBoolean("gotIt")
        )
    }
}
