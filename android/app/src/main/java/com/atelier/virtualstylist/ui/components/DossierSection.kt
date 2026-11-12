package com.atelier.virtualstylist.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.atelier.virtualstylist.data.model.GarmentAnalysis
import com.atelier.virtualstylist.data.model.StyleBrief
import com.atelier.virtualstylist.ui.theme.GalleryBorder
import com.atelier.virtualstylist.ui.theme.GalleryBrass
import com.atelier.virtualstylist.ui.theme.GalleryCard
import com.atelier.virtualstylist.ui.theme.GalleryErrorBg
import com.atelier.virtualstylist.ui.theme.GalleryErrorInk
import com.atelier.virtualstylist.ui.theme.GalleryInk
import com.atelier.virtualstylist.ui.theme.GalleryMuted
import com.atelier.virtualstylist.ui.theme.GalleryPaper

@Composable
fun DossierSection(
    analysis: GarmentAnalysis,
    brief: StyleBrief,
    isLoadingInsight: Boolean,
    insightError: String?,
    onRefreshInsight: () -> Unit,
    isLoadingQuickTips: Boolean,
    quickTipsError: String?,
    onRefreshQuickTips: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copiedHex by remember { mutableStateOf<String?>(null) }
    var showQuickTips by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header + Quick Tips Trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GARMENT DOSSIER · ${analysis.category.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = GalleryBrass
            )
            OutlinedButton(
                onClick = {
                    showQuickTips = true
                    if (analysis.quickTips.isEmpty() && !isLoadingQuickTips) {
                        onRefreshQuickTips()
                    }
                },
                shape = RectangleShape,
                border = BorderStroke(1.dp, GalleryInk),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = GalleryCard,
                    contentColor = GalleryInk
                )
            ) {
                Icon(AtelierIcons.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Quick Tips", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text(
            text = analysis.garmentTitle,
            style = MaterialTheme.typography.displaySmall,
            color = GalleryInk
        )

        Text(
            text = "${brief.season} · ${brief.silhouette} · ${brief.budgetTier}",
            style = MaterialTheme.typography.bodyMedium,
            color = GalleryMuted
        )

        Text(
            text = analysis.styleSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = GalleryInk
        )

        Surface(
            color = GalleryCard,
            border = BorderStroke(1.dp, GalleryBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "MATERIAL & PATTERN READ",
                    style = MaterialTheme.typography.labelMedium,
                    color = GalleryMuted
                )
                Text(
                    text = analysis.fabricNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GalleryInk
                )
            }
        }

        // Visual Color Palette Grid
        Surface(
            color = GalleryCard,
            border = BorderStroke(1.dp, GalleryBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("COLOR PALETTE", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                    Text(
                        "${analysis.palette.size} Swatches",
                        style = MaterialTheme.typography.labelMedium,
                        color = GalleryMuted
                    )
                }

                // Tonal Proportion Bar
                if (analysis.palette.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .border(0.5.dp, GalleryBorder)
                    ) {
                        analysis.palette.forEachIndexed { idx, sw ->
                            val weight = when {
                                sw.role.contains("primary", true) || idx == 0 -> 3f
                                sw.role.contains("secondary", true) || sw.role.contains("bridge", true) -> 2f
                                else -> 1f
                            }
                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .height(10.dp)
                                    .background(parseHexColor(sw.hex))
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    analysis.palette.chunked(2).forEach { rowSwatches ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowSwatches.forEach { swatch ->
                                val hexUpper = swatch.hex.uppercase()
                                Surface(
                                    color = GalleryPaper,
                                    border = BorderStroke(1.dp, GalleryBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                                .background(parseHexColor(swatch.hex))
                                        )
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = swatch.role.uppercase(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = GalleryBrass
                                            )
                                            Text(
                                                text = swatch.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = GalleryInk
                                            )
                                            HorizontalDivider(color = GalleryBorder)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        clipboard.setText(AnnotatedString(hexUpper))
                                                        copiedHex = hexUpper
                                                    }
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (copiedHex == hexUpper) "COPIED" else hexUpper,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (copiedHex == hexUpper) GalleryBrass else GalleryMuted
                                                )
                                                Icon(
                                                    AtelierIcons.Copy,
                                                    contentDescription = "Copy $hexUpper",
                                                    tint = GalleryMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (rowSwatches.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Style Insight Card (Material & Cut Guidance)
        Surface(
            color = GalleryCard,
            border = BorderStroke(1.dp, GalleryBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("STYLE INSIGHT · MATERIAL & CUT", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                    IconButton(onClick = onRefreshInsight, enabled = !isLoadingInsight) {
                        if (isLoadingInsight) {
                            CircularProgressIndicator(strokeWidth = 1.5.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(AtelierIcons.Refresh, contentDescription = "Refresh Style Insight", tint = GalleryInk)
                        }
                    }
                }
                HorizontalDivider(color = GalleryBorder)

                if (!insightError.isNullOrBlank()) {
                    Text(
                        text = insightError,
                        color = GalleryErrorInk,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GalleryErrorBg)
                            .padding(12.dp)
                    )
                }

                val insight = analysis.styleInsight
                if (insight != null) {
                    val rows = listOf(
                        "01. PROPORTION & CUT" to insight.fitAndProportion,
                        "02. MATERIAL & DRAPE" to insight.materialAndDrape,
                        "03. HOW TO WEAR IT" to insight.howToWear,
                        "04. WHAT TO AVOID" to insight.whatToAvoid
                    )
                    rows.forEachIndexed { idx, (label, text) ->
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                            Text(text, style = MaterialTheme.typography.bodyMedium, color = GalleryInk)
                        }
                        if (idx < rows.lastIndex) HorizontalDivider(color = GalleryBorder)
                    }
                } else if (!isLoadingInsight) {
                    Text(
                        text = "Tap Refresh above to generate proportion and drape guidance for this piece.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GalleryMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Quick Tips Modal Overlay
    if (showQuickTips) {
        Dialog(onDismissRequest = { showQuickTips = false }) {
            Surface(
                color = GalleryPaper,
                border = BorderStroke(1.dp, GalleryInk),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GalleryCard)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("QUICK TIPS · TEXTURE & CUT", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(analysis.garmentTitle, style = MaterialTheme.typography.headlineSmall, color = GalleryInk)
                        }
                        IconButton(onClick = { showQuickTips = false }) {
                            Icon(AtelierIcons.Close, contentDescription = "Close Quick Tips", tint = GalleryInk)
                        }
                    }
                    HorizontalDivider(color = GalleryBorder)

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!quickTipsError.isNullOrBlank()) {
                            Text(
                                text = quickTipsError,
                                color = GalleryErrorInk,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GalleryErrorBg)
                                    .padding(10.dp)
                            )
                        }

                        if (isLoadingQuickTips) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp, color = GalleryInk)
                            }
                        } else if (analysis.quickTips.isNotEmpty()) {
                            Surface(color = GalleryCard, border = BorderStroke(1.dp, GalleryBorder)) {
                                Column {
                                    analysis.quickTips.forEachIndexed { idx, tip ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = String.format("%02d", idx + 1),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = GalleryBrass
                                            )
                                            Text(
                                                text = tip,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = GalleryInk
                                            )
                                        }
                                        if (idx < analysis.quickTips.lastIndex) {
                                            HorizontalDivider(color = GalleryBorder)
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = onRefreshQuickTips,
                                enabled = !isLoadingQuickTips,
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, GalleryBorder)
                            ) {
                                Icon(AtelierIcons.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refresh Tips")
                            }

                            OutlinedButton(
                                onClick = { showQuickTips = false },
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, GalleryInk),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = GalleryInk,
                                    contentColor = GalleryPaper
                                )
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> GalleryInk
        }
    } catch (_: Exception) {
        GalleryInk
    }
}
