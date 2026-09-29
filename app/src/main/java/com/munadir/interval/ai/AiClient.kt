package com.munadir.interval.ai

import com.munadir.interval.data.AiProvider
import com.munadir.interval.data.CardType
import com.munadir.interval.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/** One turn in a conversation. [role] is "system", "user" or "assistant". */
data class AiMessage(val role: String, val content: String)

/** A card the model proposed, before the user has accepted it. */
data class DraftCard(
    val front: String,
    val back: String,
    val type: CardType = CardType.FLIP,
    val choices: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val accepted: Boolean = true
)

/** Where the material came from. Changes the prompt, not the output shape. */
enum class GenerateMode { TOPIC, NOTES }

sealed interface AiResult<out T> {
    data class Ok<T>(val value: T) : AiResult<T>
    data class Failed(val message: String) : AiResult<Nothing>
}

/**
 * The app's one connection to a language model.
 *
 * Two providers are supported. **Gemini is the default**: its free tier needs no credit card,
 * and it can be asked for `application/json` directly, which makes card generation far more
 * reliable than hoping a model returns clean JSON from the prompt alone. OpenRouter stays as a
 * fallback so a rate limit on one provider does not take the feature down.
 *
 * Deliberately built on HttpURLConnection and org.json rather than Retrofit/OkHttp/kotlinx --
 * two endpoints do not justify three dependencies, and the whole app remains a single external
 * library. Every entry point returns a typed failure rather than throwing, because the UI needs
 * something useful to show when the key is wrong or the network is down.
 */
object AiClient {

    private const val TIMEOUT_MS = 60_000

    val isConfigured: Boolean get() = Settings.state.value.hasAiKey

    // ------------------------------------------------------------------ chat

    private const val COACH_SYSTEM =
        "You are the study coach inside Interval, a spaced-repetition app. Help the user " +
            "understand and remember things. Be concise -- a few short paragraphs at most. " +
            "Plain text, no markdown headers."

    /** Free-form chat. [history] is the whole conversation so far, oldest first. */
    suspend fun chat(history: List<AiMessage>): AiResult<String> =
        complete(COACH_SYSTEM, history, jsonMode = false)

    /** Explains the answer to a card the user just got wrong, or is stuck on. */
    suspend fun explain(front: String, back: String): AiResult<String> = complete(
        system = "You explain flashcard answers to a student who just failed to recall one. " +
            "Give the intuition in 2-3 short sentences, then one concrete example or memory " +
            "hook. Plain text, under 90 words, no preamble.",
        turns = listOf(
            AiMessage(
                "user",
                "Question: $front\nAnswer: $back\n\nWhy is this the answer, and how do I remember it?"
            )
        ),
        jsonMode = false
    )

    // ------------------------------------------------------------------ generation

    /**
     * Builds a study set, either from a topic the user named or from material they pasted.
     *
     * Even in JSON mode the response is dug out of the text rather than trusted wholesale --
     * models still occasionally wrap output in prose or fences -- and every element is
     * validated before it becomes a draft, so a malformed multiple-choice question is dropped
     * rather than shown with three options and no answer.
     */
    suspend fun generateCards(
        input: String,
        count: Int,
        types: Set<CardType>,
        mode: GenerateMode
    ): AiResult<List<DraftCard>> {
        val allowed = types.ifEmpty { setOf(CardType.FLIP) }

        val shapes = buildList {
            if (CardType.FLIP in allowed) {
                add("""{"type":"flip","front":"a question","back":"the answer"}""")
            }
            if (CardType.MULTIPLE_CHOICE in allowed) {
                add("""{"type":"mcq","front":"a question","choices":["a","b","c","d"],"answer":0,"back":"why that option is right"}""")
            }
            if (CardType.TRUE_FALSE in allowed) {
                add("""{"type":"tf","front":"a statement that is true or false","answer":true,"back":"why"}""")
            }
        }

        val task = when (mode) {
            GenerateMode.TOPIC ->
                "Build a study set that teaches the topic below from scratch. Cover the core " +
                    "ideas someone new to it must know. Topic:\n\n$input"

            GenerateMode.NOTES ->
                "Turn the notes below into a study set. Only use facts present in the notes -- " +
                    "do not add outside material. Notes:\n\n$input"
        }

        val result = complete(
            system = "You write study material for a spaced-repetition app. " +
                "Return ONLY a JSON array, no prose and no code fences. " +
                "Mix these element shapes: ${shapes.joinToString(" or ")}. " +
                "Rules: one idea per card; fronts are a single clear question or statement; " +
                "backs are under 200 characters; for mcq give exactly four distinct plausible " +
                "options and \"answer\" is the 0-based index of the correct one; for tf " +
                "\"answer\" is a boolean. Never reveal the answer inside the front.",
            turns = listOf(AiMessage("user", "Make at most $count items. $task")),
            jsonMode = true
        )

        return when (result) {
            is AiResult.Failed -> result
            is AiResult.Ok -> {
                val cards = parseCards(result.value, allowed)
                if (cards.isEmpty()) {
                    AiResult.Failed("The model didn't return anything usable. Try rewording it.")
                } else {
                    AiResult.Ok(cards)
                }
            }
        }
    }

    // ------------------------------------------------------------------ discovery

    /**
     * Asks the provider which models this key can actually call.
     *
     * Model ids drift, differ by region, and differ by account. Hard-coding a name and hoping
     * is how you end up with an app that 404s for one user and works for another -- so the
     * settings screen offers this list instead of asking anyone to guess.
     */
    suspend fun listModels(): AiResult<List<String>> = withContext(Dispatchers.IO) {
        val prefs = Settings.state.value
        if (!prefs.hasAiKey) return@withContext AiResult.Failed("No API key set.")

        val provider = AiProvider.from(prefs.aiProvider)
        val (url, headers) = when (provider) {
            AiProvider.GEMINI ->
                "https://generativelanguage.googleapis.com/v1beta/models?pageSize=200" to
                    mapOf("x-goog-api-key" to prefs.activeKey)

            AiProvider.OPENAI ->
                "https://api.openai.com/v1/models" to
                    mapOf("Authorization" to "Bearer ${prefs.activeKey}")

            AiProvider.OPENROUTER ->
                "https://openrouter.ai/api/v1/models" to
                    mapOf("Authorization" to "Bearer ${prefs.activeKey}")
        }

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) {
                return@withContext AiResult.Failed(readableError(code, text, "model list"))
            }

            val json = JSONObject(text)
            val names = when (provider) {
                AiProvider.GEMINI -> {
                    val arr = json.optJSONArray("models") ?: JSONArray()
                    (0 until arr.length()).mapNotNull { i ->
                        val o = arr.getJSONObject(i)
                        // Only models that can actually answer a prompt.
                        val methods = o.optJSONArray("supportedGenerationMethods")
                        val supported = methods != null && (0 until methods.length())
                            .any { methods.getString(it) == "generateContent" }
                        if (supported) o.optString("name").removePrefix("models/") else null
                    }
                }

                // OpenAI and OpenRouter share the same {"data":[{"id":…}]} listing shape.
                AiProvider.OPENAI, AiProvider.OPENROUTER -> {
                    val arr = json.optJSONArray("data") ?: JSONArray()
                    (0 until arr.length()).map { arr.getJSONObject(it).optString("id") }
                }
            }

            val usable = names.filter { it.isNotBlank() }.distinct().sorted()
            if (usable.isEmpty()) AiResult.Failed("That key returned no usable models.")
            else AiResult.Ok(usable)
        } catch (e: Exception) {
            AiResult.Failed(e.message ?: "Couldn't reach the provider.")
        } finally {
            connection?.disconnect()
        }
    }

    // ------------------------------------------------------------------ transport

    private suspend fun complete(
        system: String,
        turns: List<AiMessage>,
        jsonMode: Boolean
    ): AiResult<String> = withContext(Dispatchers.IO) {
        val prefs = Settings.state.value
        if (!prefs.hasAiKey) return@withContext AiResult.Failed("No API key set.")

        val provider = AiProvider.from(prefs.aiProvider)
        // Google's docs write ids both ways ("gemini-2.5-flash" and "models/gemini-2.5-flash");
        // the path already contains "models/", so a pasted prefix would double up.
        val model = prefs.aiModel.ifBlank { provider.defaultModel }.removePrefix("models/")

        val (url, body, headers) = when (provider) {
            AiProvider.GEMINI -> geminiRequest(prefs.activeKey, model, system, turns, jsonMode)
            AiProvider.OPENAI -> chatCompletionsRequest(
                url = "https://api.openai.com/v1/chat/completions",
                key = prefs.activeKey,
                model = model,
                system = system,
                turns = turns,
                extraHeaders = emptyMap()
            )

            AiProvider.OPENROUTER -> openRouterRequest(prefs.activeKey, model, system, turns)
        }

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }

            connection.outputStream.use { it.write(body.toByteArray()) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            if (code !in 200..299) {
                return@withContext AiResult.Failed(readableError(code, text, model))
            }

            val content = when (provider) {
                AiProvider.GEMINI -> parseGemini(text)
                AiProvider.OPENAI, AiProvider.OPENROUTER -> parseChatCompletions(text)
            }

            if (content.isNullOrBlank()) AiResult.Failed("Empty response from the model.")
            else AiResult.Ok(content)
        } catch (e: Exception) {
            AiResult.Failed(e.message ?: "Couldn't reach the model.")
        } finally {
            connection?.disconnect()
        }
    }

    // ---- Gemini ----------------------------------------------------------

    private fun geminiRequest(
        key: String,
        model: String,
        system: String,
        turns: List<AiMessage>,
        jsonMode: Boolean
    ): Triple<String, String, Map<String, String>> {
        val body = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system)))
            )
            put("contents", JSONArray().apply {
                turns.forEach { turn ->
                    put(
                        JSONObject()
                            // Gemini calls the assistant "model"; everything else is "user".
                            .put("role", if (turn.role == "assistant") "model" else "user")
                            .put("parts", JSONArray().put(JSONObject().put("text", turn.content)))
                    )
                }
            })
            if (jsonMode) {
                // Asking for JSON directly is far more reliable than asking in the prompt.
                put(
                    "generationConfig",
                    JSONObject().put("responseMimeType", "application/json")
                )
            }
        }.toString()

        // The key goes in a header, not the query string, so it stays out of any URL logging.
        return Triple(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
            body,
            mapOf("x-goog-api-key" to key)
        )
    }

    private fun parseGemini(text: String): String? = JSONObject(text)
        .optJSONArray("candidates")
        ?.optJSONObject(0)
        ?.optJSONObject("content")
        ?.optJSONArray("parts")
        ?.optJSONObject(0)
        ?.optString("text")
        ?.trim()

    // ---- OpenRouter ------------------------------------------------------

    /**
     * The OpenAI chat-completions shape, which OpenRouter also implements. One builder serves
     * both; only the base URL and a couple of attribution headers differ.
     */
    private fun chatCompletionsRequest(
        url: String,
        key: String,
        model: String,
        system: String,
        turns: List<AiMessage>,
        extraHeaders: Map<String, String>
    ): Triple<String, String, Map<String, String>> {
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                turns.forEach {
                    put(JSONObject().put("role", it.role).put("content", it.content))
                }
            })
        }.toString()

        return Triple(url, body, mapOf("Authorization" to "Bearer $key") + extraHeaders)
    }

    private fun openRouterRequest(
        key: String,
        model: String,
        system: String,
        turns: List<AiMessage>
    ): Triple<String, String, Map<String, String>> = chatCompletionsRequest(
        url = "https://openrouter.ai/api/v1/chat/completions",
        key = key,
        model = model,
        system = system,
        turns = turns,
        // OpenRouter uses these for attribution on their dashboard.
        extraHeaders = mapOf(
            "HTTP-Referer" to "https://github.com/munadirkhan/interval",
            "X-Title" to "Interval"
        )
    )

    private fun parseChatCompletions(text: String): String? = JSONObject(text)
        .optJSONArray("choices")
        ?.optJSONObject(0)
        ?.optJSONObject("message")
        ?.optString("content")
        ?.trim()

    // ---- errors ----------------------------------------------------------

    private fun readableError(code: Int, body: String, model: String): String {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }

        return when (code) {
            400 -> detail ?: "The request was rejected. Check the model name in Settings."
            401, 403 -> "That API key was rejected. Check it in Settings."
            402 -> "This account is out of credit."
            404 -> "No model called \"$model\" on this provider. Tap Find models in Settings."
            429 -> "Rate limited — the free tier allows a few requests a minute. Wait and retry."
            else -> detail ?: "Request failed ($code)."
        }
    }

    // ------------------------------------------------------------------ parsing

    /** Pulls the first JSON array out of a response that may be wrapped in prose or fences. */
    private fun parseCards(raw: String, allowed: Set<CardType>): List<DraftCard> {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()

        return runCatching {
            val arr = JSONArray(raw.substring(start, end + 1))
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { toDraft(it, allowed) }
            }
        }.getOrDefault(emptyList())
    }

    private fun toDraft(o: JSONObject, allowed: Set<CardType>): DraftCard? {
        val front = o.optString("front").trim()
        val back = o.optString("back").trim()
        if (front.isBlank()) return null

        return when (o.optString("type").lowercase()) {
            "mcq", "multiple_choice", "multiple-choice" -> {
                if (CardType.MULTIPLE_CHOICE !in allowed) return null
                val arr = o.optJSONArray("choices") ?: return null
                val choices = (0 until arr.length()).map { arr.getString(it).trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                val answer = o.optInt("answer", -1)
                // Drop anything structurally wrong rather than showing a broken card.
                if (choices.size < 2 || answer !in choices.indices) return null
                DraftCard(
                    front = front,
                    back = back.ifBlank { choices[answer] },
                    type = CardType.MULTIPLE_CHOICE,
                    choices = choices,
                    correctIndex = answer
                )
            }

            "tf", "true_false", "true-false", "truefalse" -> {
                if (CardType.TRUE_FALSE !in allowed) return null
                if (!o.has("answer")) return null
                val isTrue = o.optBoolean("answer", true)
                DraftCard(
                    front = front,
                    back = back,
                    type = CardType.TRUE_FALSE,
                    choices = listOf("True", "False"),
                    correctIndex = if (isTrue) 0 else 1
                )
            }

            else -> {
                if (CardType.FLIP !in allowed || back.isBlank()) return null
                DraftCard(front = front, back = back, type = CardType.FLIP)
            }
        }
    }
}
