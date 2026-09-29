package com.munadir.interval

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.munadir.interval.ai.AiClient
import com.munadir.interval.ai.AiMessage
import com.munadir.interval.ai.AiResult
import com.munadir.interval.ai.Attachment
import com.munadir.interval.ai.AttachmentKind
import com.munadir.interval.ai.DraftCard
import com.munadir.interval.ai.GenerateMode
import com.munadir.interval.billing.Billing
import com.munadir.interval.data.Card
import com.munadir.interval.data.CardStore
import com.munadir.interval.data.CardType
import com.munadir.interval.data.Settings
import com.munadir.interval.notifications.Notifier
import com.munadir.interval.notifications.Scheduler
import com.munadir.interval.ui.AVATARS
import com.munadir.interval.ui.AddCardScreen
import com.munadir.interval.ui.BottomBar
import com.munadir.interval.ui.CoachScreen
import com.munadir.interval.ui.GenerateCardsScreen
import com.munadir.interval.ui.HomeScreen
import com.munadir.interval.ui.OnboardingScreen
import com.munadir.interval.ui.PaywallScreen
import com.munadir.interval.ui.ProfileScreen
import com.munadir.interval.ui.ReviewScreen
import com.munadir.interval.ui.SessionCompleteScreen
import com.munadir.interval.ui.SettingsScreen
import com.munadir.interval.ui.StatsScreen
import com.munadir.interval.ui.Tab
import com.munadir.interval.ui.WelcomeScreen
import com.munadir.interval.ui.dueLabel
import com.munadir.interval.ui.theme.Accent
import com.munadir.interval.ui.theme.IntervalTheme
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import kotlinx.coroutines.launch

/**
 * Routes. Tabbed destinations live under [Route.Main]; everything else is a full-screen push
 * that covers the bottom bar.
 */
private sealed interface Route {
    data object Welcome : Route
    data object Onboarding : Route
    data class Main(val tab: Tab = Tab.TODAY) : Route
    data class Edit(val card: Card?) : Route
    data object EditProfile : Route
    data object Generate : Route
    data object Review : Route
    data class Complete(val got: Int, val missed: Int, val xp: Int) : Route
    data object Stats : Route
    data object Settings : Route
    data object Paywall : Route
}

class MainActivity : ComponentActivity() {

    private var openReviewFromNotification by mutableStateOf(false)

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Settings.init(applicationContext)
        CardStore.init(applicationContext)
        openReviewFromNotification =
            intent?.getBooleanExtra(Notifier.EXTRA_OPEN_REVIEW, false) == true

        setContent {
            val prefs by Settings.state.collectAsState()
            IntervalTheme(darkTheme = prefs.darkTheme, accent = Accent.from(prefs.accent)) {
                Surface(Modifier.fillMaxSize()) { App() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(Notifier.EXTRA_OPEN_REVIEW, false)) {
            openReviewFromNotification = true
        }
    }

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.canPost(this)) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    /**
     * Reads a picked document.
     *
     * Plain text becomes source material. PDFs and images are accepted and named, but not
     * parsed -- extraction and OCR are each a dependency and a failure mode the app does not
     * carry, and [Attachment] says so plainly rather than pretending otherwise.
     */
    private fun readAttachment(uri: Uri): Attachment {
        val name = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        } ?: uri.lastPathSegment ?: "Attachment"

        val mime = contentResolver.getType(uri).orEmpty()
        val looksTextual = mime.startsWith("text/") ||
            name.endsWith(".txt", true) || name.endsWith(".md", true) ||
            name.endsWith(".csv", true) || name.endsWith(".json", true)

        if (!looksTextual) {
            return Attachment(name, AttachmentKind.DOCUMENT)
        }

        val text = runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()

        // Keep the prompt a sane size; models charge by the token and 20k characters is plenty.
        return Attachment(name, AttachmentKind.TEXT, text?.take(20_000))
    }

    @Composable
    private fun App() {
        val scope = rememberCoroutineScope()
        val cards by CardStore.cards.collectAsState()
        val events by CardStore.events.collectAsState()
        val prefs by Settings.state.collectAsState()
        val purchasedPro by Billing.isPro.collectAsState()

        // One flag gates everything. The debug override is local-only and never touches RevenueCat.
        val isPro = purchasedPro || (BuildConfig.DEBUG && prefs.debugPro)

        var route: Route by remember {
            mutableStateOf(
                when {
                    !prefs.hasProfile -> Route.Welcome
                    !prefs.onboarded -> Route.Onboarding
                    else -> Route.Main()
                }
            )
        }
        var reviewQueue by remember { mutableStateOf(emptyList<Card>()) }

        // ---- billing
        var offering by remember { mutableStateOf<Offering?>(null) }
        var offeringLoading by remember { mutableStateOf(false) }
        var purchasing by remember { mutableStateOf(false) }
        var purchaseError by remember { mutableStateOf<String?>(null) }

        // ---- coach
        val chat = remember { mutableStateListOf<AiMessage>() }
        var chatThinking by remember { mutableStateOf(false) }
        var chatError by remember { mutableStateOf<String?>(null) }

        // ---- generation
        var genMode by remember { mutableStateOf(GenerateMode.TOPIC) }
        var genInput by remember { mutableStateOf("") }
        var genTypes by remember { mutableStateOf(setOf(CardType.FLIP, CardType.MULTIPLE_CHOICE)) }
        var attachment by remember { mutableStateOf<Attachment?>(null) }
        val drafts = remember { mutableStateListOf<DraftCard>() }
        var generating by remember { mutableStateOf(false) }
        var generateError by remember { mutableStateOf<String?>(null) }

        // ---- model discovery
        val availableModels = remember { mutableStateListOf<String>() }
        var loadingModels by remember { mutableStateOf(false) }
        var modelsError by remember { mutableStateOf<String?>(null) }

        // ---- explain
        var explaining by remember { mutableStateOf(false) }
        var explanation by remember { mutableStateOf<String?>(null) }
        var explainError by remember { mutableStateOf<String?>(null) }

        val pickDocument = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri -> if (uri != null) attachment = readAttachment(uri) }

        val takePhoto = rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            if (bitmap != null) attachment = Attachment("Camera photo", AttachmentKind.PHOTO)
        }

        LaunchedEffect(Unit) {
            Billing.refresh()
            Scheduler.scheduleNext(this@MainActivity)
            Scheduler.syncDailyReminder(this@MainActivity)
        }

        // Tie purchases to the profile rather than the install, so a reinstall keeps Pro.
        LaunchedEffect(prefs.profileId) {
            if (prefs.profileId.isNotBlank()) Billing.identify(prefs.profileId)
        }

        LaunchedEffect(openReviewFromNotification) {
            if (openReviewFromNotification) {
                val due = CardStore.dueCards()
                if (due.isNotEmpty()) {
                    reviewQueue = due
                    route = Route.Review
                }
                openReviewFromNotification = false
            }
        }

        fun openPaywall() {
            route = Route.Paywall
            purchaseError = null
            if (offering == null) {
                offeringLoading = true
                scope.launch {
                    offering = Billing.currentOffering()
                    offeringLoading = false
                }
            }
        }

        fun sendToCoach(text: String) {
            chat.add(AiMessage("user", text))
            chatError = null
            chatThinking = true
            scope.launch {
                when (val result = AiClient.chat(chat.toList())) {
                    is AiResult.Ok -> chat.add(AiMessage("assistant", result.value))
                    is AiResult.Failed -> chatError = result.message
                }
                chatThinking = false
            }
        }

        fun generate() {
            // An unreadable attachment still helps: its name becomes a topic hint.
            val source = buildString {
                append(genInput.trim())
                attachment?.let { file ->
                    if (file.isReadable) {
                        appendLine()
                        appendLine()
                        append(file.extractedText)
                    } else if (genInput.isBlank()) {
                        append("the subject of a file named \"${file.name}\"")
                    } else {
                        append(" (from a file named \"${file.name}\")")
                    }
                }
            }
            if (source.isBlank()) return

            generating = true
            generateError = null
            drafts.clear()
            scope.launch {
                val effectiveMode =
                    if (attachment?.isReadable == true) GenerateMode.NOTES else genMode
                when (val result = AiClient.generateCards(source, 10, genTypes, effectiveMode)) {
                    is AiResult.Ok -> drafts.addAll(result.value)
                    is AiResult.Failed -> generateError = result.message
                }
                generating = false
            }
        }

        BackHandler(enabled = route !is Route.Main && route !is Route.Welcome) {
            route = Route.Main()
        }

        when (val current = route) {

            Route.Welcome -> WelcomeScreen(
                initialAvatar = AVATARS.first(),
                onContinue = { name, avatar ->
                    Settings.createProfile(name, avatar)
                    route = Route.Onboarding
                }
            )

            Route.EditProfile -> WelcomeScreen(
                initialName = prefs.profileName,
                initialAvatar = prefs.profileAvatar,
                isEditing = true,
                onContinue = { name, avatar ->
                    Settings.updateProfile(name, avatar)
                    route = Route.Main(Tab.YOU)
                },
                onCancel = { route = Route.Main(Tab.YOU) }
            )

            Route.Onboarding -> OnboardingScreen(
                onFinish = {
                    Settings.setOnboarded(true)
                    Scheduler.syncDailyReminder(this@MainActivity)
                    route = Route.Main()
                },
                onRequestNotifications = { askForNotifications() }
            )

            is Route.Main -> Column(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = current.tab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "tab",
                    modifier = Modifier
                        .weight(1f)
                        .statusBarsPadding()
                ) { tab ->
                    when (tab) {
                        Tab.TODAY -> HomeScreen(
                            cards = cards,
                            isPro = isPro,
                            streak = CardStore.currentStreak(),
                            totalXp = prefs.totalXp,
                            profileName = prefs.profileName,
                            profileAvatar = prefs.profileAvatar,
                            onAdd = {
                                if (!isPro && cards.size >= Config.FREE_CARD_LIMIT) openPaywall()
                                else route = Route.Edit(null)
                            },
                            onGenerate = { route = Route.Generate },
                            onReview = {
                                reviewQueue = CardStore.dueCards()
                                if (reviewQueue.isNotEmpty()) route = Route.Review
                            },
                            onEdit = { route = Route.Edit(it) },
                            onDelete = { card ->
                                CardStore.delete(card)
                                Scheduler.scheduleNext(this@MainActivity)
                            },
                            onUpgrade = { openPaywall() },
                            onTestReminder = {
                                Scheduler.scheduleIn(this@MainActivity, 15_000)
                                toast("Reminder in 15s — background the app to see it")
                            }
                        )

                        Tab.COACH -> CoachScreen(
                            messages = chat,
                            thinking = chatThinking,
                            error = chatError,
                            configured = AiClient.isConfigured,
                            avatar = prefs.profileAvatar,
                            onSend = ::sendToCoach,
                            onClear = { chat.clear(); chatError = null },
                            onConnectKey = { route = Route.Settings }
                        )

                        Tab.STATS -> if (isPro) {
                            StatsScreen(
                                streak = CardStore.currentStreak(),
                                longestStreak = CardStore.longestStreak(),
                                retention = CardStore.retention(),
                                totalReviews = events.size,
                                reviewsToday = CardStore.reviewsToday(),
                                totalXp = prefs.totalXp,
                                dailyCounts = CardStore.dailyCounts(30),
                                onBack = { route = Route.Main(Tab.TODAY) }
                            )
                        } else {
                            LaunchedEffect(Unit) { openPaywall() }
                        }

                        Tab.YOU -> ProfileScreen(
                            prefs = prefs,
                            isPro = isPro,
                            totalCards = cards.size,
                            totalReviews = events.size,
                            streak = CardStore.currentStreak(),
                            retention = CardStore.retention(),
                            onEditProfile = { route = Route.EditProfile },
                            onSettings = { route = Route.Settings },
                            onStats = { if (isPro) route = Route.Stats else openPaywall() },
                            onUpgrade = { openPaywall() }
                        )
                    }
                }

                BottomBar(
                    selected = current.tab,
                    dueCount = cards.count { it.isDue },
                    onSelect = { route = Route.Main(it) },
                    modifier = Modifier.navigationBarsPadding()
                )
            }

            is Route.Edit -> AddCardScreen(
                editing = current.card,
                decks = CardStore.decks(),
                isPro = isPro,
                onSave = { front, back, deck ->
                    val existing = current.card
                    if (existing == null) CardStore.add(front, back, deck)
                    else CardStore.update(
                        existing.copy(front = front.trim(), back = back.trim(), deck = deck.trim())
                    )
                    Scheduler.scheduleNext(this@MainActivity)
                    route = Route.Main()
                },
                onUpgrade = { openPaywall() },
                onCancel = { route = Route.Main() }
            )

            Route.Generate -> GenerateCardsScreen(
                configured = AiClient.isConfigured,
                mode = genMode,
                input = genInput,
                types = genTypes,
                attachment = attachment,
                drafts = drafts,
                working = generating,
                error = generateError,
                remainingFreeSlots = if (isPro) null
                else (Config.FREE_CARD_LIMIT - cards.size).coerceAtLeast(0),
                onModeChange = { genMode = it },
                onInputChange = { genInput = it },
                onToggleType = { type ->
                    // Never let the user switch every type off; there would be nothing to generate.
                    genTypes = if (type in genTypes) {
                        (genTypes - type).ifEmpty { setOf(type) }
                    } else {
                        genTypes + type
                    }
                },
                onAttachFile = { pickDocument.launch(arrayOf("*/*")) },
                onAttachPhoto = { takePhoto.launch(null) },
                onClearAttachment = { attachment = null },
                onGenerate = ::generate,
                onToggleDraft = { i -> drafts[i] = drafts[i].copy(accepted = !drafts[i].accepted) },
                onSaveAccepted = {
                    val accepted = drafts.filter { it.accepted }
                    val room = if (isPro) accepted.size
                    else (Config.FREE_CARD_LIMIT - cards.size).coerceAtLeast(0)

                    accepted.take(room).forEach {
                        CardStore.add(
                            front = it.front,
                            back = it.back,
                            type = it.type,
                            choices = it.choices,
                            correctIndex = it.correctIndex
                        )
                    }
                    Scheduler.scheduleNext(this@MainActivity)

                    if (accepted.size > room) {
                        // Save what fits, then ask for the rest. Better than refusing the batch.
                        drafts.clear()
                        genInput = ""
                        attachment = null
                        openPaywall()
                    } else {
                        drafts.clear()
                        genInput = ""
                        attachment = null
                        toast(if (accepted.size == 1) "1 card added" else "${accepted.size} cards added")
                        route = Route.Main()
                    }
                },
                onConnectKey = { route = Route.Settings },
                onBack = { route = Route.Main() }
            )

            Route.Review -> ReviewScreen(
                queue = reviewQueue,
                aiConfigured = AiClient.isConfigured,
                explaining = explaining,
                explanation = explanation,
                explainError = explainError,
                onExplain = { card ->
                    explaining = true
                    explanation = null
                    explainError = null
                    scope.launch {
                        when (val result = AiClient.explain(card.front, card.back)) {
                            is AiResult.Ok -> explanation = result.value
                            is AiResult.Failed -> explainError = result.message
                        }
                        explaining = false
                    }
                },
                onDismissExplanation = {
                    explaining = false
                    explanation = null
                    explainError = null
                },
                onGrade = { card, gotIt ->
                    CardStore.grade(card, gotIt)
                    Scheduler.scheduleNext(this@MainActivity)
                },
                onUndo = { previous ->
                    CardStore.undoGrade(previous)
                    Scheduler.scheduleNext(this@MainActivity)
                },
                onAward = { Settings.addXp(it) },
                onFinish = { got, missed, xp -> route = Route.Complete(got, missed, xp) },
                onExit = { route = Route.Main() }
            )

            is Route.Complete -> SessionCompleteScreen(
                got = current.got,
                missed = current.missed,
                xpEarned = current.xp,
                streak = CardStore.currentStreak(),
                nextDueLabel = cards.minByOrNull { it.dueAt }
                    ?.let { dueLabel(it).removePrefix("Due ").lowercase() },
                onDone = { route = Route.Main() },
                onStats = { if (isPro) route = Route.Stats else openPaywall() }
            )

            Route.Stats -> StatsScreen(
                streak = CardStore.currentStreak(),
                longestStreak = CardStore.longestStreak(),
                retention = CardStore.retention(),
                totalReviews = events.size,
                reviewsToday = CardStore.reviewsToday(),
                totalXp = prefs.totalXp,
                dailyCounts = CardStore.dailyCounts(30),
                onBack = { route = Route.Main(Tab.YOU) }
            )

            Route.Settings -> SettingsScreen(
                prefs = prefs,
                isPro = isPro,
                onAccent = { Settings.setAccent(it.name) },
                onDarkTheme = { Settings.setDarkTheme(it) },
                onNotifications = {
                    Settings.setNotifications(it)
                    if (it) askForNotifications()
                    Scheduler.scheduleNext(this@MainActivity)
                    Scheduler.syncDailyReminder(this@MainActivity)
                },
                onDailyReminder = {
                    Settings.setDailyReminder(it)
                    Scheduler.syncDailyReminder(this@MainActivity)
                },
                onReminderTime = { h, m ->
                    Settings.setReminderTime(h, m)
                    Scheduler.syncDailyReminder(this@MainActivity)
                },
                onAiKey = {
                    Settings.setAiKey(it)
                    toast(if (it.isBlank()) "Key cleared" else "Key saved")
                },
                onAiModel = {
                    val fallback = com.munadir.interval.data.AiProvider
                        .from(prefs.aiProvider).defaultModel
                    val model = it.ifBlank { fallback }
                    Settings.setAiModel(model)
                    toast("Model set to $model")
                },
                onAiProvider = {
                    Settings.setAiProvider(it)
                    availableModels.clear()
                    modelsError = null
                    toast("Using ${it.label}")
                },
                availableModels = availableModels,
                loadingModels = loadingModels,
                modelsError = modelsError,
                onFindModels = {
                    loadingModels = true
                    modelsError = null
                    availableModels.clear()
                    scope.launch {
                        when (val result = AiClient.listModels()) {
                            is AiResult.Ok -> {
                                availableModels.addAll(result.value)
                                toast("Found ${result.value.size} models — scroll down to pick one")
                            }

                            is AiResult.Failed -> {
                                modelsError = result.message
                                toast(result.message)
                            }
                        }
                        loadingModels = false
                    }
                },
                onUpgrade = { openPaywall() },
                onRestore = {
                    scope.launch { toast(Billing.restore() ?: "Purchases restored") }
                },
                onTestNotification = {
                    Scheduler.scheduleIn(this@MainActivity, 15_000)
                    toast("Reminder in 15s — background the app to see it")
                },
                onDebugPro = { Settings.setDebugPro(it) },
                onSeedCards = {
                    CardStore.seedMany(20)
                    toast("Seeded 20 cards")
                },
                onBack = { route = Route.Main(Tab.YOU) }
            )

            Route.Paywall -> PaywallScreen(
                offering = offering,
                loading = offeringLoading,
                purchasing = purchasing,
                error = purchaseError,
                onPurchase = { pkg: Package ->
                    purchasing = true
                    purchaseError = null
                    scope.launch {
                        val failure = Billing.purchase(this@MainActivity, pkg)
                        purchasing = false
                        purchaseError = failure
                        if (failure == null && Billing.isPro.value) route = Route.Main()
                    }
                },
                onRestore = {
                    scope.launch {
                        purchaseError = Billing.restore()
                        if (Billing.isPro.value) route = Route.Main()
                    }
                },
                onClose = { route = Route.Main() }
            )
        }
    }
}
