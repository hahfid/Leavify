package com.hafd.leafivy3.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hafd.leafivy3.R
import com.hafd.leafivy3.ml.Prediction
import com.hafd.leafivy3.ui.components.ConfidenceBar
import com.hafd.leafivy3.ui.components.ConfidenceBarChart
import com.hafd.leafivy3.ui.components.leafivyBackground

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ResultScreen(
    uiState: LeafivyUiState,
    onBack: () -> Unit,
    onOpenCareGuide: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                actions = {
                    if (uiState.predictions.isNotEmpty() && !uiState.isLoading) {
                        IconButton(onClick = { shareResult(context, uiState.predictions.first()) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.btn_share))
                        }
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
                // ── Leaf image ────────────────────────────────────────────────
                AnimatedVisibility(visible = uiState.image != null, enter = fadeIn() + scaleIn(initialScale = 0.95f)) {
                    uiState.image?.let { bitmap ->
                        Card(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            elevation = CardDefaults.cardElevation(0.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.cd_leaf_image),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // ── Loading ───────────────────────────────────────────────────
                AnimatedVisibility(visible = uiState.isLoading, enter = fadeIn(), exit = fadeOut()) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text(stringResource(R.string.analyzing_leaf), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ── Error ─────────────────────────────────────────────────────
                AnimatedVisibility(visible = uiState.error != null && !uiState.isLoading, enter = fadeIn() + slideInVertically(), exit = fadeOut()) {
                    uiState.error?.let { errorMessage ->
                        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.height(4.dp))
                                Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                // ── Results ───────────────────────────────────────────────────
                AnimatedVisibility(visible = uiState.predictions.isNotEmpty() && !uiState.isLoading, enter = fadeIn() + slideInVertically(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val topPrediction = uiState.predictions.firstOrNull() ?: return@AnimatedVisibility
                        val displayLabel = normalizeLabel(topPrediction.label)
                        val isHealthy = displayLabel.equals("Healthy", ignoreCase = true)
                        val statusColor = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                        // Status card
                        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(displayLabel, style = MaterialTheme.typography.headlineSmall, color = statusColor)
                                }
                                Text(
                                    stringResource(R.string.confidence_format, topPrediction.confidence * 100),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(if (isHealthy) stringResource(R.string.disease_healthy) else stringResource(R.string.recommended_action), style = MaterialTheme.typography.labelSmall) }
                                    )
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("${(topPrediction.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        // Disease details
                        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    stringResource(R.string.disease_details),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val descRes = descriptionResForDisease(displayLabel)
                                Text(
                                    text = if (descRes != null) context.getString(descRes) else stringResource(R.string.no_details_available),
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Text(
                                    stringResource(R.string.recommended_action),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val treatRes = treatmentResForDisease(displayLabel)
                                Text(
                                    text = if (treatRes != null) context.getString(treatRes) else stringResource(R.string.no_details_available),
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Chart & all predictions
                        if (uiState.predictions.size > 1) {
                            Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(stringResource(R.string.result_chart_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    ConfidenceBarChart(predictions = uiState.predictions, modifier = Modifier.fillMaxWidth())
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(stringResource(R.string.all_predictions), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    uiState.predictions.forEach { pred ->
                                        ConfidenceBar(label = normalizeLabel(pred.label), confidence = pred.confidence, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                            }
                        }

                        // Explore section
                        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.result_explore_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                FilledTonalButton(onClick = onOpenCareGuide, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                                    Text(stringResource(R.string.recommended_action))
                                }
                                FilledTonalButton(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                                    Text(stringResource(R.string.diagnostics_title))
                                }
                            }
                        }
                    }
                }

                // ── Analyze another ───────────────────────────────────────────
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.btn_analyze_another))
                }
                Text(
                    text = stringResource(R.string.copyright_line),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun shareResult(context: Context, prediction: Prediction) {
    val displayLabel = normalizeLabel(prediction.label)
    val descRes = descriptionResForDisease(displayLabel)
    val shareText = context.getString(R.string.share_text, displayLabel, prediction.confidence * 100,
        if (descRes != null) context.getString(descRes) else context.getString(R.string.no_details_available))
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_title))
        putExtra(Intent.EXTRA_TEXT, shareText)
    }, context.getString(R.string.share_title)))
}

private fun descriptionResForDisease(disease: String): Int? = when (disease.trim().lowercase()) {
    "frog eye leaf spot" -> R.string.desc_frogeye
    "powdery mildew"     -> R.string.desc_powdery
    "rust"               -> R.string.desc_rust
    "scab"               -> R.string.desc_scab
    "healthy"            -> R.string.desc_healthy
    else                 -> null
}

private fun treatmentResForDisease(disease: String): Int? = when (disease.trim().lowercase()) {
    "frog eye leaf spot" -> R.string.treatment_frogeye
    "powdery mildew"     -> R.string.treatment_powdery
    "rust"               -> R.string.treatment_rust
    "scab"               -> R.string.treatment_scab
    "healthy"            -> R.string.treatment_healthy
    else                 -> null
}

