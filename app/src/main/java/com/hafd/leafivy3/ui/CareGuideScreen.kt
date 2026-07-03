package com.hafd.leafivy3.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hafd.leafivy3.R
import com.hafd.leafivy3.ui.components.leafivyBackground

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CareGuideScreen(
    uiState: LeafivyUiState,
    onBack: () -> Unit
) {
    val displayLabel = uiState.predictions.firstOrNull()?.let { normalizeLabel(it.label) } ?: ""

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.care_guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        leafivyBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (displayLabel.isBlank()) {
                    Text(stringResource(R.string.no_prediction), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    // ── Disease header ────────────────────────────────────────
                    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(displayLabel, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(stringResource(R.string.disease_details), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    // ── Care guide ────────────────────────────────────────────
                    CareGuideSection(displayLabel = displayLabel, modifier = Modifier.fillMaxWidth())

                    // ── Recommended action ────────────────────────────────────
                    val treatmentRes = treatmentResForLabel(displayLabel)
                    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.recommended_action), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(
                                text = if (treatmentRes != null) stringResource(treatmentRes) else stringResource(R.string.no_details_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Text(stringResource(R.string.copyright_line), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CareGuideSection(displayLabel: String, modifier: Modifier = Modifier) {
    val guide = careGuideForDisease(displayLabel)
    val expanded = remember { mutableStateOf(true) }

    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.care_guide_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { expanded.value = !expanded.value }) {
                    Text(
                        if (expanded.value) stringResource(R.string.care_guide_hide)
                        else stringResource(R.string.care_guide_show),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Text(stringResource(guide.summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (expanded.value) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.care_guide_immediate), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                guide.immediateSteps.forEach { stepRes ->
                    Text("• " + stringResource(stepRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.care_guide_prevention), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                guide.preventionSteps.forEach { stepRes ->
                    Text("• " + stringResource(stepRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.care_guide_followup), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                guide.followUp.forEach { stepRes ->
                    Text("• " + stringResource(stepRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

internal fun treatmentResForLabel(disease: String): Int? = when (disease.trim().lowercase()) {
    "frog eye leaf spot" -> R.string.treatment_frogeye
    "powdery mildew"     -> R.string.treatment_powdery
    "rust"               -> R.string.treatment_rust
    "scab"               -> R.string.treatment_scab
    "healthy"            -> R.string.treatment_healthy
    else                 -> null
}
