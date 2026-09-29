package com.munadir.interval.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.munadir.interval.BuildConfig
import com.munadir.interval.data.AiProvider
import com.munadir.interval.data.Prefs
import com.munadir.interval.ui.theme.Accent

@Composable
fun SettingsScreen(
    prefs: Prefs,
    isPro: Boolean,
    onAccent: (Accent) -> Unit,
    onDarkTheme: (Boolean) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onDailyReminder: (Boolean) -> Unit,
    onReminderTime: (Int, Int) -> Unit,
    onAiKey: (String) -> Unit,
    onAiModel: (String) -> Unit,
    onAiProvider: (AiProvider) -> Unit,
    availableModels: List<String>,
    loadingModels: Boolean,
    modelsError: String?,
    onFindModels: () -> Unit,
    onUpgrade: () -> Unit,
    onRestore: () -> Unit,
    onTestNotification: () -> Unit,
    onDebugPro: (Boolean) -> Unit,
    onSeedCards: () -> Unit,
    onBack: () -> Unit
) {
    var keyDraft by remember(prefs.aiProvider, prefs.activeKey) { mutableStateOf(prefs.activeKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var modelDraft by remember(prefs.aiModel) { mutableStateOf(prefs.aiModel) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBack) { Text("Done") }
        }

        // ------------------------------------------------------------ appearance

        SectionLabel("Appearance")

        SettingsCard {
            RowItem(
                title = "Dark theme",
                subtitle = "Paper black with one warm accent",
                trailing = { Switch(checked = prefs.darkTheme, onCheckedChange = onDarkTheme) }
            )
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column(Modifier.padding(16.dp)) {
                Text("Accent", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (isPro) "Pick any colour" else "Amber is free. Pro unlocks the rest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Accent.entries.forEach { accent ->
                        val locked = !isPro && accent != Accent.AMBER
                        val selected = prefs.accent == accent.name
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accent.swatch.copy(alpha = if (locked) 0.3f else 1f))
                                .clickable { if (locked) onUpgrade() else onAccent(accent) },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                locked -> Text(
                                    "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )

                                selected -> Text(
                                    "✓",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------ ai

        SectionLabel("AI coach")

        SettingsCard {
            Column(Modifier.padding(16.dp)) {
                Text("Provider", style = MaterialTheme.typography.bodyLarge)
                Text(
                    when (AiProvider.from(prefs.aiProvider)) {
                        AiProvider.GEMINI -> "Free tier, no credit card needed."
                        AiProvider.OPENAI -> "Uses your OpenAI account credit."
                        AiProvider.OPENROUTER -> "One key, hundreds of models."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    AiProvider.entries.forEach { option ->
                        val selected = prefs.aiProvider == option.name
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onAiProvider(option) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                option.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text("API key", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Powers card generation, explanations and the Coach tab. " +
                        "Stored on this device only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = keyDraft,
                    onValueChange = { keyDraft = it },
                    placeholder = { Text(AiProvider.from(prefs.aiProvider).keyHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (keyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { keyVisible = !keyVisible }) {
                        Text(
                            if (keyVisible) "Hide" else "Show",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onAiKey(keyDraft) }) {
                        Text(if (prefs.activeKey.isNotBlank()) "Update key" else "Save key")
                    }
                }
                Text(
                    when (AiProvider.from(prefs.aiProvider)) {
                        AiProvider.GEMINI ->
                            "Free key at aistudio.google.com → Get API key. No card needed."
                        AiProvider.OPENAI ->
                            "Key at platform.openai.com → API keys. Uses your account balance."
                        AiProvider.OPENROUTER -> "Get one at openrouter.ai → Keys."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                Text("Model", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Model id for the selected provider. The default is on the free tier.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = modelDraft,
                    onValueChange = { modelDraft = it },
                    placeholder = { Text(AiProvider.from(prefs.aiProvider).defaultModel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onFindModels, enabled = !loadingModels) {
                        Text(if (loadingModels) "Checking…" else "Find models")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onAiModel(modelDraft) }) { Text("Save model") }
                }

                if (modelsError != null) {
                    Text(
                        modelsError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (availableModels.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${availableModels.size} MODELS THIS KEY CAN USE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    // No nested verticalScroll here: this card already sits inside the
                    // screen's scroll container, and stacking two in the same direction is
                    // both a measurement hazard and horrible to actually use.
                    Column(Modifier.fillMaxWidth()) {
                        availableModels.forEach { id ->
                            val selected = id == prefs.aiModel
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { modelDraft = id; onAiModel(id) }
                                    .padding(horizontal = 14.dp, vertical = 11.dp)
                            ) {
                                Text(
                                    id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------ notifications

        SectionLabel("Notifications")

        SettingsCard {
            RowItem(
                title = "Notifications",
                subtitle = "Reminders when cards come due",
                trailing = {
                    Switch(checked = prefs.notificationsEnabled, onCheckedChange = onNotifications)
                }
            )
            Divider()
            RowItem(
                title = "Daily reminder",
                subtitle = "A nudge at the same time each day",
                trailing = {
                    Switch(
                        checked = prefs.dailyReminderEnabled,
                        onCheckedChange = onDailyReminder,
                        enabled = prefs.notificationsEnabled
                    )
                }
            )
            Divider()
            TimePickerRow(
                hour = prefs.dailyReminderHour,
                minute = prefs.dailyReminderMinute,
                enabled = prefs.notificationsEnabled && prefs.dailyReminderEnabled,
                onChange = onReminderTime
            )
            Divider()
            RowItem(
                title = "Send a test reminder",
                subtitle = "Fires in 15 seconds — background the app to see it",
                onClick = onTestNotification
            )
        }

        // ------------------------------------------------------------ pro

        SectionLabel("Interval Pro")

        SettingsCard {
            RowItem(
                title = if (isPro) "Pro is active" else "Upgrade to Pro",
                subtitle = "Unlimited cards, decks, stats and every accent",
                onClick = if (isPro) null else onUpgrade
            )
            Divider()
            RowItem(
                title = "Restore purchases",
                subtitle = "Already bought Pro? Bring it back.",
                onClick = onRestore
            )
        }

        // ------------------------------------------------------------ debug

        if (BuildConfig.DEBUG) {
            SectionLabel("Debug")
            SettingsCard {
                RowItem(
                    title = "Grant Pro",
                    subtitle = "Local override for screenshots. Does not touch RevenueCat.",
                    trailing = { Switch(checked = prefs.debugPro, onCheckedChange = onDebugPro) }
                )
                Divider()
                RowItem(
                    title = "Seed 20 cards",
                    subtitle = "Bulk data for testing the list and stats",
                    onClick = onSeedCards
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Interval — built solo for the RevenueCat Shipaton 2026.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(26.dp))
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface),
        content = content
    )
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

@Composable
private fun RowItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * A stepper rather than a full time-picker dialog. Two taps to move an hour is fine for a
 * setting people change once, and it keeps the screen self-contained.
 */
@Composable
private fun TimePickerRow(
    hour: Int,
    minute: Int,
    enabled: Boolean,
    onChange: (Int, Int) -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Reminder time",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            Text(
                formatTime(hour, minute),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            )
        }
        Stepper("−", enabled) { onChange((hour + 23) % 24, minute) }
        Spacer(Modifier.size(8.dp))
        Stepper("+", enabled) { onChange((hour + 1) % 24, minute) }
    }
}

@Composable
private fun Stepper(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(h, minute, suffix)
}
