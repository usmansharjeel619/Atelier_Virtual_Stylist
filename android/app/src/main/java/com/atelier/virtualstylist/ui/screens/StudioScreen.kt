package com.atelier.virtualstylist.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atelier.virtualstylist.data.model.SampleGarment
import com.atelier.virtualstylist.data.model.StyleBrief
import com.atelier.virtualstylist.ui.StudioUiState
import com.atelier.virtualstylist.ui.components.AtelierIcons
import com.atelier.virtualstylist.ui.components.DossierSection
import com.atelier.virtualstylist.ui.components.LookTriptychSection
import com.atelier.virtualstylist.ui.theme.GalleryBorder
import com.atelier.virtualstylist.ui.theme.GalleryBrass
import com.atelier.virtualstylist.ui.theme.GalleryCard
import com.atelier.virtualstylist.ui.theme.GalleryErrorBg
import com.atelier.virtualstylist.ui.theme.GalleryErrorInk
import com.atelier.virtualstylist.ui.theme.GalleryInk
import com.atelier.virtualstylist.ui.theme.GalleryMuted
import com.atelier.virtualstylist.ui.theme.GalleryPaper
import com.atelier.virtualstylist.ui.theme.GalleryStone
import com.atelier.virtualstylist.util.ImageCodec

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    state: StudioUiState,
    sampleGarments: List<SampleGarment>,
    onPickPhoto: (android.net.Uri) -> Unit,
    onSelectSample: (SampleGarment) -> Unit,
    onClearPiece: () -> Unit,
    onChangeBrief: (StyleBrief) -> Unit,
    onAnalyzeAndStyle: () -> Unit,
    onRefreshInsight: () -> Unit,
    onRefreshQuickTips: () -> Unit,
    onRetryFlatLay: (String) -> Unit,
    onOpenRefine: (String) -> Unit,
    onCloseRefine: () -> Unit,
    onApplyEdit: (String, String, String) -> Unit,
    onSelectVersion: (String, String) -> Unit,
    onSaveLook: (com.atelier.virtualstylist.data.model.GeneratedLookState) -> Unit,
    onExportImage: (String, String) -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickPhoto(uri)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaper)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("EDITORIAL WARDROBE STUDIO", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                Text(
                    "One Difficult Piece. Three Complete Looks.",
                    style = MaterialTheme.typography.displaySmall,
                    color = GalleryInk
                )
                Text(
                    "Upload a garment photo or pick a sample piece to extract its true color palette and generate Casual, Business, and Night Out flat-lay outfits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GalleryMuted
                )

                // Upload / Active Garment Preview
                if (state.sourceImageDataUrl == null) {
                    Surface(
                        color = GalleryCard,
                        border = BorderStroke(1.dp, GalleryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(AtelierIcons.AddPhoto, contentDescription = null, tint = GalleryInk)
                            Text("Choose Garment Photo", style = MaterialTheme.typography.titleMedium, color = GalleryInk)
                            Text(
                                "Tap to open Android Photo Picker",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GalleryMuted
                            )
                        }
                    }
                } else {
                    Surface(
                        color = GalleryCard,
                        border = BorderStroke(1.dp, GalleryBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            val bmp = remember(state.sourceImageDataUrl) {
                                ImageCodec.decodeDataUrlToBitmap(state.sourceImageDataUrl)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                                    .background(GalleryCard),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = state.activeGarmentLabel ?: "Selected garment",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = GalleryBorder)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SELECTED PIECE", style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
                                    Text(
                                        state.activeGarmentLabel ?: "Uploaded Garment",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Row {
                                    OutlinedButton(
                                        onClick = {
                                            photoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RectangleShape,
                                        border = BorderStroke(1.dp, GalleryBorder)
                                    ) {
                                        Text("Replace")
                                    }
                                    IconButton(onClick = onClearPiece) {
                                        Icon(AtelierIcons.Refresh, contentDescription = "Reset piece")
                                    }
                                }
                            }
                        }
                    }
                }

                // Curated Sample Starters
                Text("CURATED DIFFICULT PIECES", style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sampleGarments) { sample ->
                        Surface(
                            color = GalleryCard,
                            border = BorderStroke(1.dp, GalleryBorder),
                            modifier = Modifier
                                .width(156.dp)
                                .clickable { onSelectSample(sample) }
                        ) {
                            Column {
                                AsyncImage(
                                    model = sample.sampleUrl,
                                    contentDescription = sample.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                        .background(GalleryStone)
                                )
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(sample.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(sample.subtitle, style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
                                }
                            }
                        }
                    }
                }

                // Styling Brief Selectors
                Surface(
                    color = GalleryCard,
                    border = BorderStroke(1.dp, GalleryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("STYLING BRIEF", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)

                        BriefChipRow(
                            label = "Season",
                            options = listOf("Spring/Summer", "Autumn/Winter", "Year-Round"),
                            selected = state.brief.season,
                            onSelect = { onChangeBrief(state.brief.copy(season = it)) }
                        )
                        BriefChipRow(
                            label = "Silhouette",
                            options = listOf("Tailored", "Relaxed", "Sculptural"),
                            selected = state.brief.silhouette,
                            onSelect = { onChangeBrief(state.brief.copy(silhouette = it)) }
                        )
                        BriefChipRow(
                            label = "Budget Tier",
                            options = listOf("Accessible", "Contemporary", "Designer"),
                            selected = state.brief.budgetTier,
                            onSelect = { onChangeBrief(state.brief.copy(budgetTier = it)) }
                        )
                    }
                }

                if (!state.analyzeError.isNullOrBlank()) {
                    Text(
                        text = state.analyzeError,
                        color = GalleryErrorInk,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GalleryErrorBg)
                            .padding(12.dp)
                    )
                }

                Button(
                    onClick = onAnalyzeAndStyle,
                    enabled = state.sourceImageDataUrl != null && !state.isAnalyzing,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GalleryInk,
                        contentColor = GalleryPaper
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(if (state.isAnalyzing) "Analyzing Palette & Composing Looks..." else "Analyze & Style 3 Outfits")
                }
            }
            HorizontalDivider(color = GalleryBorder)
        }

        val analysis = state.analysis
        if (analysis != null) {
            item {
                DossierSection(
                    analysis = analysis,
                    brief = state.brief,
                    isLoadingInsight = state.isLoadingInsight,
                    insightError = state.insightError,
                    onRefreshInsight = onRefreshInsight,
                    isLoadingQuickTips = state.isLoadingQuickTips,
                    quickTipsError = state.quickTipsError,
                    onRefreshQuickTips = onRefreshQuickTips
                )
                HorizontalDivider(color = GalleryBorder)
            }

            item {
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
                    onApplyEdit = onApplyEdit,
                    onSelectVersion = onSelectVersion,
                    onSaveLook = onSaveLook,
                    onExportImage = onExportImage
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BriefChipRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = GalleryMuted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selected == option
                OutlinedButton(
                    onClick = { onSelect(option) },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (isSelected) GalleryInk else GalleryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) GalleryInk else GalleryPaper,
                        contentColor = if (isSelected) GalleryPaper else GalleryInk
                    )
                ) {
                    Text(option)
                }
            }
        }
    }
}
