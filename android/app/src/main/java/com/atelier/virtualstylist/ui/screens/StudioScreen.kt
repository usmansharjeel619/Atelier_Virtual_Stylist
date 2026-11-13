package com.atelier.virtualstylist.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atelier.virtualstylist.data.model.GeneratedLookState
import com.atelier.virtualstylist.data.model.SampleGarment
import com.atelier.virtualstylist.data.model.StyleBrief
import com.atelier.virtualstylist.ui.StudioUiState
import com.atelier.virtualstylist.ui.components.AtelierIcons
import com.atelier.virtualstylist.ui.components.ColorDnaSection
import com.atelier.virtualstylist.ui.components.LookTriptychSection
import com.atelier.virtualstylist.ui.components.StyleNotesSection
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
import com.atelier.virtualstylist.ui.theme.GalleryStone
import com.atelier.virtualstylist.ui.theme.HotCoral
import com.atelier.virtualstylist.ui.theme.InnerShape
import com.atelier.virtualstylist.ui.theme.NeonLime
import com.atelier.virtualstylist.ui.theme.PillShape
import com.atelier.virtualstylist.util.ImageCodec

private enum class ResultsSectionTab {
    OUTFITS, COLORS, NOTES
}

@Composable
fun StudioScreen(
    state: StudioUiState,
    sampleGarments: List<SampleGarment>,
    onPickPhoto: (android.net.Uri) -> Unit,
    onSelectSample: (SampleGarment) -> Unit,
    onClearPiece: () -> Unit,
    onChangeBrief: (StyleBrief) -> Unit,
    onAnalyzeAndStyle: () -> Unit,
    onSetShowResults: (Boolean) -> Unit,
    onRefreshInsight: () -> Unit,
    onRefreshQuickTips: () -> Unit,
    onRetryFlatLay: (String) -> Unit,
    onOpenRefine: (String) -> Unit,
    onCloseRefine: () -> Unit,
    onOpenInEditor: (GeneratedLookState) -> Unit,
    onApplyEdit: (String, String, String) -> Unit,
    onSelectVersion: (String, String) -> Unit,
    onSaveLook: (GeneratedLookState) -> Unit,
    onExportImage: (String, String) -> Unit,
    onOpenServerDialog: () -> Unit
) {
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickPhoto(uri)
    }

    var resultsTab by remember { mutableStateOf(ResultsSectionTab.OUTFITS) }

    val activeAnalysis = state.analysis
    if (state.studioShowResults && activeAnalysis != null) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(GalleryPaper),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Compact Anchor Garment Strip
                    AtelierSurfaceCard(
                        containerColor = GalleryCard,
                        borderColor = GalleryHairline
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val bmp = remember(state.sourceImageDataUrl) {
                                state.sourceImageDataUrl?.let { ImageCodec.decodeDataUrlToBitmap(it) }
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(InnerShape)
                                    .background(GalleryStone),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = activeAnalysis.garmentTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeAnalysis.garmentTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${state.brief.season} · ${state.brief.silhouette}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GalleryMuted,
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = { onSetShowResults(false) },
                                shape = PillShape,
                                border = BorderStroke(1.5.dp, GalleryInk),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("New Piece", style = MaterialTheme.typography.labelMedium, color = GalleryInk)
                            }
                        }
                    }

                    // 3 Clean Segmented Tabs: Outfits | Colors | Notes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PillShape)
                            .background(GalleryStone)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple(ResultsSectionTab.OUTFITS, "Outfits (${state.looks.size})", NeonLime),
                            Triple(ResultsSectionTab.COLORS, "Colors", GalleryBrass),
                            Triple(ResultsSectionTab.NOTES, "Notes", GalleryInk)
                        ).forEach { (tab, label, activeBg) ->
                            val selected = resultsTab == tab
                            val fg = when {
                                !selected -> GalleryMuted
                                activeBg == NeonLime -> GalleryInk
                                else -> Color.White
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(PillShape)
                                    .background(if (selected) activeBg else Color.Transparent)
                                    .clickable { resultsTab = tab }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = fg,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            item {
                when (resultsTab) {
                    ResultsSectionTab.OUTFITS -> {
                        val refiningLook = state.looks.find { it.id == state.refiningLookId }
                        LookTriptychSection(
                            looks = state.looks,
                            savingLookId = state.savingLookId,
                            refiningLook = refiningLook,
                            isApplyingEdit = state.isApplyingEdit,
                            editError = state.editError,
                            onRetryFlatLay = onRetryFlatLay,
                            onOpenRefine = onOpenRefine,
                            onCloseRefine = onCloseRefine,
                            onOpenInEditor = onOpenInEditor,
                            onApplyEdit = onApplyEdit,
                            onSelectVersion = onSelectVersion,
                            onSaveLook = onSaveLook,
                            onExportImage = onExportImage
                        )
                    }

                    ResultsSectionTab.COLORS -> {
                        ColorDnaSection(analysis = activeAnalysis)
                    }

                    ResultsSectionTab.NOTES -> {
                        StyleNotesSection(
                            analysis = activeAnalysis,
                            isLoadingInsight = state.isLoadingInsight,
                            insightError = state.insightError,
                            onRefreshInsight = onRefreshInsight,
                            isLoadingQuickTips = state.isLoadingQuickTips,
                            quickTipsError = state.quickTipsError,
                            onRefreshQuickTips = onRefreshQuickTips
                        )
                    }
                }
            }
        }
        return
    }

    // STAGE 1: Clean Visual Piece Picker & Vibe Selector
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaper),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pick a piece. Get 3 fits.", style = MaterialTheme.typography.displaySmall)
                }
                if (activeAnalysis != null) {
                    OutlinedButton(
                        onClick = { onSetShowResults(true) },
                        shape = PillShape,
                        border = BorderStroke(1.5.dp, GalleryInk),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = NeonLime),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("My Fits →", style = MaterialTheme.typography.labelMedium, color = GalleryInk)
                    }
                }
            }
        }

        if (state.serverOnline == false) {
            item {
                AtelierSurfaceCard(
                    containerColor = GalleryErrorBg,
                    borderColor = HotCoral,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenServerDialog() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Server not connected", style = MaterialTheme.typography.titleMedium, color = GalleryErrorInk)
                            Text("Tap to set your public app URL.", style = MaterialTheme.typography.bodyMedium, color = GalleryErrorInk)
                        }
                        Icon(AtelierIcons.ServerLink, contentDescription = null, tint = GalleryErrorInk)
                    }
                }
            }
        }

        item {
            // Visual Drop Frame
            if (state.sourceImageDataUrl == null) {
                AtelierSurfaceCard(
                    containerColor = GalleryCard,
                    borderColor = GalleryHairline,
                    shape = CardShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NeonLime),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AtelierIcons.AddPhoto, contentDescription = null, tint = GalleryInk)
                        }
                        Text("Tap to upload a garment", style = MaterialTheme.typography.headlineSmall)
                        Text("Jacket, skirt, trousers, top — anything", style = MaterialTheme.typography.bodyMedium, color = GalleryMuted)
                    }
                }
            } else {
                AtelierSurfaceCard(
                    containerColor = GalleryCard,
                    borderColor = GalleryInk,
                    borderWidth = 1.5.dp,
                    shape = CardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val bmp = remember(state.sourceImageDataUrl) {
                        ImageCodec.decodeDataUrlToBitmap(state.sourceImageDataUrl)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.25f)
                            .background(GalleryStone)
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = state.activeGarmentLabel ?: "Selected piece",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(PillShape)
                                    .background(GalleryInk.copy(alpha = 0.85f))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = state.activeGarmentLabel ?: "Selected Piece",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(GalleryCard)
                                        .border(1.dp, GalleryInk, PillShape)
                                        .clickable {
                                            photoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text("Swap", style = MaterialTheme.typography.labelMedium)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(GalleryCard)
                                        .border(1.dp, GalleryInk, CircleShape)
                                        .clickable { onClearPiece() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(AtelierIcons.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp), tint = GalleryInk)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Or try a starter piece", style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(sampleGarments) { sample ->
                        val previewBmp = remember(sample.assetPath) {
                            ImageCodec.decodeAssetBitmap(context, sample.assetPath)
                        }
                        val isPicked = state.activeGarmentLabel == sample.title
                        AtelierSurfaceCard(
                            containerColor = if (isPicked) NeonLime else GalleryCard,
                            borderColor = if (isPicked) GalleryInk else GalleryHairline,
                            borderWidth = if (isPicked) 2.dp else 1.5.dp,
                            shape = InnerShape,
                            modifier = Modifier
                                .width(124.dp)
                                .clickable { onSelectSample(sample) }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 3.4f)
                                        .background(GalleryStone)
                                ) {
                                    if (previewBmp != null) {
                                        Image(
                                            bitmap = previewBmp.asImageBitmap(),
                                            contentDescription = sample.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Text(
                                    text = sample.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Compact Horizontal Vibe Selectors
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactPillStrip(
                    title = "Season",
                    options = listOf("Year-Round", "Spring/Summer", "Autumn/Winter"),
                    selected = state.brief.season,
                    onSelect = { onChangeBrief(state.brief.copy(season = it)) }
                )
                CompactPillStrip(
                    title = "Fit",
                    options = listOf("Tailored", "Relaxed", "Sculptural"),
                    selected = state.brief.silhouette,
                    onSelect = { onChangeBrief(state.brief.copy(silhouette = it)) }
                )
                CompactPillStrip(
                    title = "Budget",
                    options = listOf("Accessible", "Contemporary", "Designer"),
                    selected = state.brief.budgetTier,
                    onSelect = { onChangeBrief(state.brief.copy(budgetTier = it)) }
                )
            }
        }

        if (!state.analyzeError.isNullOrBlank()) {
            item {
                AtelierSurfaceCard(
                    containerColor = GalleryErrorBg,
                    borderColor = HotCoral,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(state.analyzeError, style = MaterialTheme.typography.bodyMedium, color = GalleryErrorInk)
                        if (state.analyzeError.contains("Server", true) ||
                            state.analyzeError.contains("Deploy", true) ||
                            state.analyzeError.contains("URL", true)
                        ) {
                            OutlinedButton(
                                onClick = onOpenServerDialog,
                                shape = PillShape,
                                border = BorderStroke(1.5.dp, GalleryErrorInk)
                            ) {
                                Text("Set Server URL", color = GalleryErrorInk)
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onAnalyzeAndStyle,
                enabled = state.sourceImageDataUrl != null && !state.isAnalyzing,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonLime,
                    contentColor = GalleryInk,
                    disabledContainerColor = GalleryStone,
                    disabledContentColor = GalleryMuted
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (state.isAnalyzing) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = GalleryInk)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Styling 3 Outfits...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Icon(AtelierIcons.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Style 3 Outfits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CompactPillStrip(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(if (isSelected) GalleryInk else GalleryCard)
                        .border(1.5.dp, if (isSelected) GalleryInk else GalleryHairline, PillShape)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) NeonLime else GalleryInk
                    )
                }
            }
        }
    }
}
