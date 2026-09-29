package com.munadir.interval.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.munadir.interval.BuildConfig
import com.munadir.interval.Config
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType

@Composable
fun PaywallScreen(
    offering: Offering?,
    loading: Boolean,
    purchasing: Boolean,
    error: String?,
    onPurchase: (Package) -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit
) {
    var selected by remember { mutableStateOf<Package?>(null) }

    // Default to annual — the plan worth nudging people toward.
    LaunchedEffect(offering) {
        if (selected == null) {
            selected = offering?.availablePackages?.firstOrNull { it.packageType == PackageType.ANNUAL }
                ?: offering?.availablePackages?.firstOrNull()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) {
                Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))

        Text("Interval Pro", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Five cards is a note. Pro is a memory system.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        Feature("Unlimited cards", "Free stops at ${Config.FREE_CARD_LIMIT}.")
        Feature("Decks", "Keep chemistry apart from Spanish.")
        Feature("Stats", "Streaks, retention and a 30-day heatmap.")
        Feature("Every accent", "Amber, ink blue, sage, magenta.")

        Spacer(Modifier.height(28.dp))

        when {
            loading -> Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            offering == null || offering.availablePackages.isEmpty() -> Text(
                "Couldn't load plans. Check your connection and try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )

            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                offering.availablePackages.forEach { pkg ->
                    PlanRow(
                        pkg = pkg,
                        selected = selected?.identifier == pkg.identifier,
                        onClick = { selected = pkg }
                    )
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { selected?.let(onPurchase) },
            enabled = selected != null && !purchasing,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                if (purchasing) "Working…" else "Continue",
                style = MaterialTheme.typography.labelLarge
            )
        }

        TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
            Text("Restore purchases", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Text(
                    "Sandbox build — this uses RevenueCat's Test Store. " +
                        "Tap Continue, then TEST VALID PURCHASE. No money moves.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Feature(title: String, detail: String) {
    Row(Modifier.padding(vertical = 7.dp)) {
        Text(
            "✓",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.padding(horizontal = 7.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanRow(pkg: Package, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                planName(pkg),
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            val sub = when (pkg.packageType) {
                PackageType.LIFETIME -> "Pay once, keep forever"
                PackageType.ANNUAL -> "Best value"
                else -> null
            }
            if (sub != null) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            pkg.product.price.formatted,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun planName(pkg: Package): String = when (pkg.packageType) {
    PackageType.MONTHLY -> "Monthly"
    PackageType.ANNUAL -> "Yearly"
    PackageType.LIFETIME -> "Lifetime"
    PackageType.WEEKLY -> "Weekly"
    PackageType.SIX_MONTH -> "6 months"
    PackageType.THREE_MONTH -> "3 months"
    PackageType.TWO_MONTH -> "2 months"
    else -> pkg.identifier.replaceFirstChar { it.uppercase() }
}
