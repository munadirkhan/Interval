package com.munadir.interval.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Which model service the AI features talk to.
 *
 * Gemini is the default because its free tier needs no credit card and it can be asked for
 * JSON directly. OpenRouter stays available as a fallback.
 */
enum class AiProvider(val label: String, val defaultModel: String, val keyHint: String) {
    GEMINI("Gemini", "gemini-2.5-flash", "AIza…"),
    OPENAI("OpenAI", "gpt-4o-mini", "sk-…"),
    OPENROUTER("OpenRouter", "openai/gpt-4o-mini", "sk-or-v1-…");

    companion object {
        fun from(name: String?): AiProvider = entries.firstOrNull { it.name == name } ?: GEMINI
    }
}

/** Everything the user can change, held in SharedPreferences and exposed as one state object. */
data class Prefs(
    // ---- profile
    /** Stable id for this profile. Also becomes the RevenueCat app user id. */
    val profileId: String = "",
    val profileName: String = "",
    val profileAvatar: String = "🦉",
    val profileCreatedAt: Long = 0L,

    // ---- appearance
    val accent: String = "AMBER",
    val darkTheme: Boolean = true,

    // ---- notifications
    val notificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderHour: Int = 19,
    val dailyReminderMinute: Int = 0,

    // ---- ai
    val aiProvider: String = "GEMINI",
    /**
     * One key per provider, stored on device only and never committed.
     *
     * Kept separate rather than sharing one field: switching provider to try the other one
     * would otherwise overwrite the key you already had working.
     */
    val geminiKey: String = "",
    val openAiKey: String = "",
    val openRouterKey: String = "",
    val aiModel: String = DEFAULT_AI_MODEL,

    // ---- progress
    /** Lifetime XP. Earned on correct answers, with a bonus for streaks inside a session. */
    val totalXp: Int = 0,

    // ---- lifecycle
    val onboarded: Boolean = false,
    /** Debug-only override so screenshots and demos do not need a live purchase. */
    val debugPro: Boolean = false
) {
    val hasProfile: Boolean get() = profileName.isNotBlank()

    fun keyFor(provider: AiProvider): String = when (provider) {
        AiProvider.GEMINI -> geminiKey
        AiProvider.OPENAI -> openAiKey
        AiProvider.OPENROUTER -> openRouterKey
    }

    /** The key for whichever provider is currently selected. */
    val activeKey: String get() = keyFor(AiProvider.from(aiProvider))
    val hasAiKey: Boolean get() = activeKey.isNotBlank()

    companion object {
        const val DEFAULT_AI_MODEL = "gemini-2.5-flash"
    }
}

object Settings {

    private lateinit var prefs: SharedPreferences

    private val _state = MutableStateFlow(Prefs())
    val state: StateFlow<Prefs> = _state.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("interval_prefs", Context.MODE_PRIVATE)
        _state.value = Prefs(
            profileId = prefs.getString("profileId", "") ?: "",
            profileName = prefs.getString("profileName", "") ?: "",
            profileAvatar = prefs.getString("profileAvatar", "🦉") ?: "🦉",
            profileCreatedAt = prefs.getLong("profileCreatedAt", 0L),
            accent = prefs.getString("accent", "AMBER") ?: "AMBER",
            darkTheme = prefs.getBoolean("darkTheme", true),
            notificationsEnabled = prefs.getBoolean("notificationsEnabled", true),
            dailyReminderEnabled = prefs.getBoolean("dailyReminderEnabled", true),
            dailyReminderHour = prefs.getInt("dailyReminderHour", 19),
            dailyReminderMinute = prefs.getInt("dailyReminderMinute", 0),
            aiProvider = prefs.getString("aiProvider", "GEMINI") ?: "GEMINI",
            geminiKey = prefs.getString("geminiKey", null)
                // Installs before per-provider keys stored one shared "aiKey".
                ?: (prefs.getString("aiKey", "") ?: "").takeIf {
                    (prefs.getString("aiProvider", "GEMINI") ?: "GEMINI") == "GEMINI"
                }.orEmpty(),
            openAiKey = prefs.getString("openAiKey", "") ?: "",
            openRouterKey = prefs.getString("openRouterKey", null)
                ?: (prefs.getString("aiKey", "") ?: "").takeIf {
                    (prefs.getString("aiProvider", "GEMINI") ?: "GEMINI") == "OPENROUTER"
                }.orEmpty(),
            aiModel = migrateModel(
                provider = prefs.getString("aiProvider", "GEMINI") ?: "GEMINI",
                stored = prefs.getString("aiModel", Prefs.DEFAULT_AI_MODEL) ?: Prefs.DEFAULT_AI_MODEL
            ),
            totalXp = prefs.getInt("totalXp", 0),
            onboarded = prefs.getBoolean("onboarded", false),
            debugPro = prefs.getBoolean("debugPro", false)
        )
    }

    /**
     * Repairs a model id left over from a different provider.
     *
     * Installs that predate the provider setting stored an OpenRouter id like
     * "openai/gpt-4o-mini". Defaulting the provider to Gemini without touching that would send
     * an OpenAI model name to Google and 404 -- which reads to the user as a broken app.
     * OpenRouter ids are namespaced with a slash; Gemini's are not.
     */
    private fun migrateModel(provider: String, stored: String): String {
        val target = AiProvider.from(provider)
        if (stored.isBlank()) return target.defaultModel
        // Only reset when the stored id is recognisably another provider's default. A model
        // the user typed themselves is left alone.
        val belongsElsewhere = AiProvider.entries.any { it != target && it.defaultModel == stored }
        return if (belongsElsewhere) target.defaultModel else stored
    }

    private fun update(block: (Prefs) -> Prefs) {
        val next = block(_state.value)
        _state.value = next
        prefs.edit()
            .putString("profileId", next.profileId)
            .putString("profileName", next.profileName)
            .putString("profileAvatar", next.profileAvatar)
            .putLong("profileCreatedAt", next.profileCreatedAt)
            .putString("accent", next.accent)
            .putBoolean("darkTheme", next.darkTheme)
            .putBoolean("notificationsEnabled", next.notificationsEnabled)
            .putBoolean("dailyReminderEnabled", next.dailyReminderEnabled)
            .putInt("dailyReminderHour", next.dailyReminderHour)
            .putInt("dailyReminderMinute", next.dailyReminderMinute)
            .putString("aiProvider", next.aiProvider)
            .putString("geminiKey", next.geminiKey)
            .putString("openAiKey", next.openAiKey)
            .putString("openRouterKey", next.openRouterKey)
            .putString("aiModel", next.aiModel)
            .putInt("totalXp", next.totalXp)
            .putBoolean("onboarded", next.onboarded)
            .putBoolean("debugPro", next.debugPro)
            .apply()
    }

    /** Creates the profile on first run. The id is what RevenueCat is told to identify. */
    fun createProfile(name: String, avatar: String) = update {
        it.copy(
            profileId = it.profileId.ifBlank { UUID.randomUUID().toString() },
            profileName = name.trim(),
            profileAvatar = avatar,
            profileCreatedAt = if (it.profileCreatedAt == 0L) System.currentTimeMillis() else it.profileCreatedAt
        )
    }

    fun updateProfile(name: String, avatar: String) = update {
        it.copy(profileName = name.trim(), profileAvatar = avatar)
    }

    fun setAccent(name: String) = update { it.copy(accent = name) }
    fun setDarkTheme(on: Boolean) = update { it.copy(darkTheme = on) }
    fun setNotifications(on: Boolean) = update { it.copy(notificationsEnabled = on) }
    fun setDailyReminder(on: Boolean) = update { it.copy(dailyReminderEnabled = on) }
    fun setReminderTime(hour: Int, minute: Int) =
        update { it.copy(dailyReminderHour = hour, dailyReminderMinute = minute) }

    /** Saves the key against whichever provider is selected right now. */
    fun setAiKey(key: String) = update {
        when (AiProvider.from(it.aiProvider)) {
            AiProvider.GEMINI -> it.copy(geminiKey = key.trim())
            AiProvider.OPENAI -> it.copy(openAiKey = key.trim())
            AiProvider.OPENROUTER -> it.copy(openRouterKey = key.trim())
        }
    }

    /**
     * Switching provider resets the model to that provider's default, unless the user has
     * typed a custom one. Carrying "gemini-2.5-flash" over to OpenRouter would just 404.
     */
    fun setAiProvider(provider: AiProvider) = update {
        val wasDefault = AiProvider.entries.any { p -> p.defaultModel == it.aiModel }
        it.copy(
            aiProvider = provider.name,
            aiModel = if (wasDefault || it.aiModel.isBlank()) provider.defaultModel else it.aiModel
        )
    }
    fun setAiModel(model: String) = update { it.copy(aiModel = model.trim()) }

    fun addXp(amount: Int) = update { it.copy(totalXp = it.totalXp + amount) }

    fun setOnboarded(done: Boolean) = update { it.copy(onboarded = done) }
    fun setDebugPro(on: Boolean) = update { it.copy(debugPro = on) }
}
