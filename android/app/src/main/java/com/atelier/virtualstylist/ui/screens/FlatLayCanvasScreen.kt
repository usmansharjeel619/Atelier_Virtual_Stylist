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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atelier.virtualstylist.ui.StudioUiState
import com.atelier.virtualstylist.ui.components.AtelierIcons
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

@Composable
fun FlatLayCanvasScreen(
    state: StudioUiState,
    onUpdateTitle: (String) -> Unit,
    onPickReferenceUri: (android.net.Uri) -> Unit,
    onClearReference: () -> Unit,
    onGenerate: (prompt: String, editCurrent: Boolean, aspectRatio: String, imageSize: String) -> Unit,
    onSelectVersion: (String) -> Unit,
    onExportImage: (String, String) -> Unit,
    onSaveToCloud: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var aspectRatio by remember { mutableStateOf("3:4") }
    var imageSize by remember { mutableStateOf("1K") }
    var showOriginal by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickReferenceUri(uri)
    }

    val quickMoves = listOf(
        "Silver sneakers" to "Swap shoes for chunky silver sneakers",
        "Leather moto" to "Add oversized boxy leather moto jacket",
        "Steel floor" to "Change background to brushed steel and concrete",
        "Oval shades" to "Add oval tinted sunglasses and chrome mini bag"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaper),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Visual Canvas First
            AtelierSurfaceCard(
                containerColor = GalleryCard,
                borderColor = GalleryHairline,
                shape = CardShape
            ) {
                Column {
                    val activeUrl = if (showOriginal && state.canvasHistory.isNotEmpty()) {
                        state.canvasHistory.first()
                    } else {
                        state.canvasGeneratedUrl
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 3.8f)
                            .background(GalleryStone),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            state.isCanvasGenerating -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, color = GalleryInk)
                                    Text("Rendering...", style = MaterialTheme.typography.titleMedium)
                                }
                            }

                            activeUrl != null -> {
                                val bmp = remember(activeUrl) {
                                    ImageCodec.decodeDataUrlToBitmap(activeUrl)
                                }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = state.canvasTitle,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Top overlaid action pills
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (state.canvasHistory.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .clip(PillShape)
                                                .background(if (showOriginal) NeonLime else GalleryCard.copy(alpha = 0.92f))
                                                .clickable { showOriginal = !showOriginal }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                if (showOriginal) "Original" else "Compare v1",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(GalleryCard.copy(alpha = 0.92f))
                                            .clickable {
                                                onExportImage(
                                                    activeUrl,
                                                    state.canvasTitle.lowercase().replace(Regex("[^a-z0-9]+"), "-")
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(AtelierIcons.Download, contentDescription = "Export", tint = GalleryInk, modifier = Modifier.size(18.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(HotCoral)
                                            .clickable { onSaveToCloud() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(AtelierIcons.BookmarkBorder, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            else -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(NeonLime),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(AtelierIcons.Edit, contentDescription = null, tint = GalleryInk)
                                    }
                                    Text("Remix & Edit Studio", style = MaterialTheme.typography.headlineSmall)
                                    Text(
                                        "Pick a move below or type any change.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GalleryMuted
                                    )
                                }
                            }
                        }
                    }

                    if (state.canvasHistory.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            itemsIndexed(state.canvasHistory) { idx, url ->
                                val thumb = remember(url) { ImageCodec.decodeDataUrlToBitmap(url) }
                                val isCurrent = state.canvasGeneratedUrl == url
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            if (isCurrent) 2.dp else 1.dp,
                                            if (isCurrent) GalleryBrass else GalleryHairline,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            showOriginal = false
                                            onSelectVersion(url)
                                        }
                                ) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb.asImageBitmap(),
                                            contentDescription = "v${idx + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // Horizontal Quick Remix Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickMoves) { (chipLabel, fullPrompt) ->
                    val picked = prompt == fullPrompt
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (picked) NeonLime else GalleryCard)
                            .border(1.5.dp, if (picked) GalleryInk else GalleryHairline, PillShape)
                            .clickable { prompt = fullPrompt }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text("+ $chipLabel", style = MaterialTheme.typography.labelMedium, color = GalleryInk)
                    }
                }
            }
        }

        item {
            AtelierSurfaceCard(
                containerColor = GalleryCard,
                borderColor = GalleryHairline,
                shape = CardShape
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = {
                            Text(
                                if (state.canvasGeneratedUrl != null) "What should we change?"
                                else "Describe your outfit"
                            )
                        },
                        minLines = 2,
                        shape = InnerShape,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Collapsible Options Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(if (state.canvasReferenceDataUrl != null) NeonLime else GalleryPaper)
                                    .border(1.dp, GalleryHairline, PillShape)
                                    .clickable {
                                        if (state.canvasReferenceDataUrl == null) {
                                            picker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        } else {
                                            onClearReference()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    if (state.canvasReferenceDataUrl != null) "Photo Attached ✕" else "+ Attach Photo",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(if (showOptions) GalleryInk else GalleryPaper)
                                    .border(1.dp, GalleryHairline, PillShape)
                                    .clickable { showOptions = !showOptions }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    "$aspectRatio · $imageSize",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (showOptions) NeonLime else GalleryInk
                                )
                            }
                        }
                    }

                    if (showOptions) {
                        OutlinedTextField(
                            value = state.canvasTitle,
                            onValueChange = onUpdateTitle,
                            label = { Text("Look Name") },
                            singleLine = true,
                            shape = InnerShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("3:4", "1:1", "4:3", "9:16")) { ratio ->
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(if (aspectRatio == ratio) NeonLime else GalleryPaper)
                                        .border(1.dp, GalleryHairline, PillShape)
                                        .clickable { aspectRatio = ratio }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(ratio, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            items(listOf("1K", "2K")) { sz ->
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(if (imageSize == sz) GalleryBrass else GalleryPaper)
                                        .border(1.dp, GalleryHairline, PillShape)
                                        .clickable { imageSize = sz }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(sz, style = MaterialTheme.typography.labelMedium, color = if (imageSize == sz) Color.White else GalleryInk)
                                }
                            }
                        }
                    }

                    if (!state.canvasError.isNullOrBlank()) {
                        Text(
                            text = state.canvasError,
                            color = GalleryErrorInk,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(InnerShape)
                                .background(GalleryErrorBg)
                                .padding(10.dp)
                        )
                    }

                    if (state.canvasGeneratedUrl != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    showOriginal = false
                                    onGenerate(prompt, true, aspectRatio, imageSize)
                                    prompt = ""
                                },
                                enabled = prompt.isNotBlank() && !state.isCanvasGenerating,
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = GalleryInk),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                            ) {
                                Text("Apply Edit", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    showOriginal = false
                                    onGenerate(prompt, false, aspectRatio, imageSize)
                                    prompt = ""
                                },
                                enabled = prompt.isNotBlank() && !state.isCanvasGenerating,
                                shape = PillShape,
                                border = BorderStroke(1.5.dp, GalleryInk),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                            ) {
                                Text("Fresh Shot", color = GalleryInk)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                onGenerate(prompt, false, aspectRatio, imageSize)
                                prompt = ""
                            },
                            enabled = prompt.isNotBlank() && !state.isCanvasGenerating,
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonLime,
                                contentColor = GalleryInk,
                                disabledContainerColor = GalleryStone,
                                disabledContentColor = GalleryMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(AtelierIcons.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.isCanvasGenerating) "Rendering..." else "Generate Flat-Lay", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
