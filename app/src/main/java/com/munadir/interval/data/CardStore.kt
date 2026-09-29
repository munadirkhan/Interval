package com.munadir.interval.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Cards and the review log, persisted as a single JSON file in private storage.
 *
 * A database would be the textbook answer, but Room pulls in an annotation processor and a
 * migration story that a six-field model does not need. One file, read once at startup,
 * rewritten atomically on change.
 */
object CardStore {

    private const val SCHEMA_VERSION = 1

    private lateinit var file: File

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    private val _events = MutableStateFlow<List<ReviewEvent>>(emptyList())
    val events: StateFlow<List<ReviewEvent>> = _events.asStateFlow()

    fun init(context: Context) {
        if (::file.isInitialized) return
        file = File(context.filesDir, "interval.json")
        load()
    }

    // ---------------------------------------------------------------- persistence

    private fun load() {
        runCatching {
            if (!file.exists()) return
            val root = JSONObject(file.readText())
            val cardArr = root.optJSONArray("cards") ?: JSONArray()
            val eventArr = root.optJSONArray("events") ?: JSONArray()
            _cards.value = (0 until cardArr.length()).map { Card.fromJson(cardArr.getJSONObject(it)) }
            _events.value =
                (0 until eventArr.length()).map { ReviewEvent.fromJson(eventArr.getJSONObject(it)) }
        }.onFailure {
            // A corrupt file must not wipe the user's work silently -- keep a copy to recover from.
            runCatching { file.copyTo(File(file.parentFile, "interval.bak"), overwrite = true) }
        }
    }

    /** Write to a temp file and rename, so a crash mid-write cannot leave half a file behind. */
    private fun persist() {
        runCatching {
            val root = JSONObject().apply {
                put("version", SCHEMA_VERSION)
                put("cards", JSONArray().also { arr -> _cards.value.forEach { arr.put(it.toJson()) } })
                put("events", JSONArray().also { arr -> _events.value.forEach { arr.put(it.toJson()) } })
            }
            val tmp = File(file.parentFile, "interval.json.tmp")
            tmp.writeText(root.toString())
            tmp.renameTo(file)
        }
    }

    // ---------------------------------------------------------------- cards

    fun add(
        front: String,
        back: String,
        deck: String = "",
        type: CardType = CardType.FLIP,
        choices: List<String> = emptyList(),
        correctIndex: Int = 0
    ): Card {
        val card = Card(
            // Ids come from the clock, so a batch added in the same millisecond would collide.
            id = nextId(),
            front = front.trim(),
            back = back.trim(),
            deck = deck.trim(),
            type = type,
            choices = choices,
            correctIndex = correctIndex
        )
        _cards.value = _cards.value + card
        persist()
        return card
    }

    private fun nextId(): Long {
        val now = System.currentTimeMillis()
        val highest = _cards.value.maxOfOrNull { it.id } ?: 0L
        return if (now > highest) now else highest + 1
    }

    fun update(card: Card) {
        _cards.value = _cards.value.map { if (it.id == card.id) card else it }
        persist()
    }

    fun delete(card: Card) {
        _cards.value = _cards.value.filterNot { it.id == card.id }
        persist()
    }

    fun byId(id: Long): Card? = _cards.value.firstOrNull { it.id == id }

    fun dueCards(): List<Card> = _cards.value.filter { it.isDue }.sortedBy { it.dueAt }

    fun decks(): List<String> =
        _cards.value.map { it.deck }.filter { it.isNotBlank() }.distinct().sorted()

    /** When the next card comes due, or null if there are no cards at all. */
    fun nextDueAt(): Long? = _cards.value.minOfOrNull { it.dueAt }

    // ---------------------------------------------------------------- grading

    fun grade(card: Card, gotIt: Boolean) {
        update(if (gotIt) card.promoted() else card.reset())
        _events.value = _events.value + ReviewEvent(card.id, System.currentTimeMillis(), gotIt)
        persist()
    }

    /** Puts a card back exactly as it was and drops its most recent event. Backs the undo snackbar. */
    fun undoGrade(previous: Card) {
        _cards.value = _cards.value.map { if (it.id == previous.id) previous else it }
        val lastForCard = _events.value.indexOfLast { it.cardId == previous.id }
        if (lastForCard >= 0) {
            _events.value = _events.value.toMutableList().apply { removeAt(lastForCard) }
        }
        persist()
    }

    // ---------------------------------------------------------------- stats

    /** Fraction of all reviews that were correct, 0f..1f. Null when nothing has been reviewed. */
    fun retention(): Float? {
        val all = _events.value
        if (all.isEmpty()) return null
        return all.count { it.gotIt }.toFloat() / all.size
    }

    fun reviewsToday(): Int {
        val start = startOfToday()
        return _events.value.count { it.timestamp >= start }
    }

    /** Reviews per day for the last [days] days, oldest first. Drives the heatmap and bars. */
    fun dailyCounts(days: Int): List<Int> {
        val start = startOfToday()
        val day = TimeUnit.DAYS.toMillis(1)
        return (days - 1 downTo 0).map { back ->
            val from = start - back * day
            val to = from + day
            _events.value.count { it.timestamp in from until to }
        }
    }

    /** Consecutive days ending today (or yesterday) with at least one review. */
    fun currentStreak(): Int {
        if (_events.value.isEmpty()) return 0
        val day = TimeUnit.DAYS.toMillis(1)
        val start = startOfToday()
        val reviewed = _events.value.map { dayIndexOf(it.timestamp) }.toHashSet()
        val todayIndex = start / day

        // A streak stays alive until the end of the following day, so reviewing yesterday and
        // not yet today does not read as a broken streak.
        var cursor = if (reviewed.contains(todayIndex)) todayIndex else todayIndex - 1
        if (!reviewed.contains(cursor)) return 0

        var streak = 0
        while (reviewed.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    fun longestStreak(): Int {
        if (_events.value.isEmpty()) return 0
        val days = _events.value.map { dayIndexOf(it.timestamp) }.distinct().sorted()
        var best = 1
        var run = 1
        for (i in 1 until days.size) {
            if (days[i] == days[i - 1] + 1) run++ else run = 1
            if (run > best) best = run
        }
        return best
    }

    private fun dayIndexOf(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / TimeUnit.DAYS.toMillis(1)
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // ---------------------------------------------------------------- seed and debug

    /**
     * The first-run deck. Teaches how the app works while demonstrating all three card types,
     * so the quiz modes are visible before anyone connects an AI key.
     */
    fun seedIfEmpty() {
        if (_cards.value.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val deck = "How memory works"

        _cards.value = listOf(
            Card(
                id = now,
                front = "What is spaced repetition?",
                back = "Reviewing material at widening intervals, timed to land right before you would forget it.",
                deck = deck
            ),
            Card(
                id = now + 1,
                front = "Which interval comes after 3 days in Interval?",
                back = "The ladder is 1, 3, 7, 30, 90 days. Each correct recall moves a card one rung up.",
                deck = deck,
                type = CardType.MULTIPLE_CHOICE,
                choices = listOf("5 days", "7 days", "14 days", "30 days"),
                correctIndex = 1
            ),
            Card.trueFalse(
                id = now + 2,
                statement = "Rereading your notes builds memory as well as testing yourself does.",
                isTrue = false,
                explanation = "It doesn't. Retrieval strengthens memory far more than review — this is the testing effect.",
                deck = deck
            )
        )
        persist()
    }

    /** Debug affordance: bulk cards for screenshots without typing them one at a time. */
    fun seedMany(count: Int) {
        val now = System.currentTimeMillis()
        _cards.value = _cards.value + (1..count).map {
            Card(now + it, "Sample card $it", "Sample answer $it", "Sample deck")
        }
        persist()
    }

    fun wipe() {
        _cards.value = emptyList()
        _events.value = emptyList()
        persist()
    }
}
