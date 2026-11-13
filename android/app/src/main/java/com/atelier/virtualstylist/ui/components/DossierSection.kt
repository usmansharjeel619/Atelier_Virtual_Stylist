package com.atelier.virtualstylist.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.atelier.virtualstylist.data.model.GarmentAnalysis
import com.atelier.virtualstylist.ui.theme.AtelierSurfaceCard
import com.atelier.virtualstylist.ui.theme.CardShape
import com.atelier.virtualstylist.ui.theme.GalleryBrass
import com.atelier.virtualstylist.ui.theme.GalleryCard
import com.atelier.virtualstylist.ui.theme.GalleryErrorBg
import com.atelier.virtualstylist.ui.theme.GalleryErrorInk
import com.atelier.virtualstylist.ui.theme.GalleryHairline
import com.atelier.virtualstylist.ui.theme.GalleryInk
import com.atelier.virtualstylist.ui.theme.GalleryMuted
import com.atelier.virtualstylist.ui.theme.GalleryPaper
import com.atelier.virtualstylist.ui.theme.HotCoral
import com.atelier.virtualstylist.ui.theme.InnerShape
import com.atelier.virtualstylist.ui.theme.NeonLime
import com.atelier.virtualstylist.ui.theme.PillShape

@Composable
fun ColorDnaSection(analysis: GarmentAnalysis) {
    val clipboard = LocalClipboardManager.current
    var copiedHex by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (analysis.palette.isNotEmpty()) {
            AtelierSurfaceCard(
                containerColor = GalleryCard,
                borderColor = GalleryHairline,
                shape = CardShape
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color Mix", style = MaterialTheme.typography.titleMedium)
                        Text("${analysis.palette.size} shades", style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(InnerShape)
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
                                    .height(22.dp)
                                    .background(parseHexColor(sw.hex))
                            )
                        }
                    }
                }
            }
        }

        analysis.palette.chunked(2).forEach { rowSwatches ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowSwatches.forEach { swatch ->
                    val hexUpper = swatch.hex.uppercase()
                    AtelierSurfaceCard(
                        containerColor = GalleryCard,
                        borderColor = GalleryHairline,
                        shape = CardShape,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                clipboard.setText(AnnotatedString(hexUpper))
                                copiedHex = hexUpper
                            }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .background(parseHexColor(swatch.hex))
                            )
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = swatch.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (copiedHex == hexUpper) "Copied!" else hexUpper,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (copiedHex == hexUpper) HotCoral else GalleryMuted
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

@Composable
fun StyleNotesSection(
    analysis: GarmentAnalysis,
    isLoadingInsight: Boolean,
    insightError: String?,
    onRefreshInsight: () -> Unit,
    isLoadingQuickTips: Boolean,
    quickTipsError: String?,
    onRefreshQuickTips: () -> Unit
) {
    var showQuickTips by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    showQuickTips = true
                    if (analysis.quickTips.isEmpty() && !isLoadingQuickTips) {
                        onRefreshQuickTips()
                    }
                },
                shape = PillShape,
                border = BorderStroke(1.5.dp, GalleryInk),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = NeonLime),
                modifier = Modifier.weight(1f)
            ) {
                Icon(AtelierIcons.Lightbulb, contentDescription = null, tint = GalleryInk, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Quick Tips", style = MaterialTheme.typography.labelMedium, color = GalleryInk)
            }

            OutlinedButton(
                onClick = onRefreshInsight,
                enabled = !isLoadingInsight,
                shape = PillShape,
                border = BorderStroke(1.5.dp, GalleryHairline),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = GalleryCard),
                modifier = Modifier.weight(1f)
            ) {
                if (isLoadingInsight) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                } else {
                    Icon(AtelierIcons.Refresh, contentDescription = null, tint = GalleryInk, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh", style = MaterialTheme.typography.labelMedium, color = GalleryInk)
            }
        }

        if (!insightError.isNullOrBlank()) {
            Text(
                text = insightError,
                color = GalleryErrorInk,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(InnerShape)
                    .background(GalleryErrorBg)
                    .padding(12.dp)
            )
        }

        val insight = analysis.styleInsight
        if (insight != null) {
            val cards = listOf(
                Triple("Fit", insight.fitAndProportion, GalleryBrass),
                Triple("Fabric", insight.materialAndDrape, GalleryInk),
                Triple("Wear With", insight.howToWear, GalleryInk),
                Triple("Avoid", insight.whatToAvoid, HotCoral)
            )
            cards.forEach { (title, body, accent) ->
                AtelierSurfaceCard(
                    containerColor = GalleryCard,
                    borderColor = GalleryHairline
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(title, style = MaterialTheme.typography.labelMedium, color = accent)
                        Text(body, style = MaterialTheme.typography.bodyMedium, color = GalleryInk)
                    }
                }
            }
        } else {
            AtelierSurfaceCard(containerColor = GalleryCard, borderColor = GalleryHairline) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Cut & Texture", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                    Text(analysis.fabricNotes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showQuickTips) {
        Dialog(onDismissRequest = { showQuickTips = false }) {
            AtelierSurfaceCard(
                containerColor = GalleryPaper,
                borderColor = GalleryInk,
                shape = CardShape
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quick Tips", style = MaterialTheme.typography.headlineSmall)
                        IconButton(onClick = { showQuickTips = false }, modifier = Modifier.size(30.dp)) {
                            Icon(AtelierIcons.Close, contentDescription = "Close")
                        }
                    }

                    if (!quickTipsError.isNullOrBlank()) {
                        Text(quickTipsError, color = GalleryErrorInk, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (isLoadingQuickTips) {
                        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = GalleryInk)
                        }
                    } else if (analysis.quickTips.isNotEmpty()) {
                        AtelierSurfaceCard(containerColor = GalleryCard, borderColor = GalleryHairline, shape = InnerShape) {
                            Column {
                                analysis.quickTips.forEachIndexed { idx, tip ->
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("${idx + 1}.", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                                        Text(tip, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (idx < analysis.quickTips.lastIndex) {
                                        HorizontalDivider(thickness = 1.dp, color = GalleryHairline)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRefreshQuickTips,
                            enabled = !isLoadingQuickTips,
                            shape = PillShape,
                            border = BorderStroke(1.5.dp, GalleryHairline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Refresh", color = GalleryInk)
                        }
                        OutlinedButton(
                            onClick = { showQuickTips = false },
                            shape = PillShape,
                            border = BorderStroke(1.5.dp, GalleryInk),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = NeonLime),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", color = GalleryInk)
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
