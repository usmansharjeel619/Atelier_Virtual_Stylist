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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.atelier.virtualstylist.ui.StudioUiState
import com.atelier.virtualstylist.ui.components.AtelierIcons
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

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickReferenceUri(uri)
    }

    val starters = listOf(
        "Minimalist Parisian autumn flat-lay: oversized camel cashmere coat, crisp white poplin shirt, raw indigo denim, espresso loafers on warm alabaster paper",
        "Gallery night-out flat-lay: black silk slip dress, silver metallic heels, sculptural onyx clutch, crystal drop earrings on pale limestone"
    )

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
                Text("DIRECT IMAGE CREATION & EDITING", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                Text("Flat-Lay Prompt Canvas", style = MaterialTheme.typography.displaySmall, color = GalleryInk)
                Text(
                    "Compose original flat-lay outfits from text prompts or upload a reference piece to refine with gemini-nano-banana-2.1.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GalleryMuted
                )

                // Canvas Output Preview
                Surface(
                    color = GalleryCard,
                    border = BorderStroke(1.dp, GalleryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .background(GalleryStone),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                state.isCanvasGenerating -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, color = GalleryInk)
                                        Text("Composing on Gallery Paper", style = MaterialTheme.typography.headlineSmall)
                                    }
                                }

                                state.canvasGeneratedUrl != null -> {
                                    val bmp = remember(state.canvasGeneratedUrl) {
                                        ImageCodec.decodeDataUrlToBitmap(state.canvasGeneratedUrl)
                                    }
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = state.canvasTitle,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                else -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Text("Blank Gallery Paper", style = MaterialTheme.typography.headlineSmall)
                                        Text(
                                            "Enter a prompt below or attach a reference photo to begin.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = GalleryMuted
                                        )
                                    }
                                }
                            }
                        }

                        if (state.canvasGeneratedUrl != null) {
                            HorizontalDivider(color = GalleryBorder)
                            if (state.canvasHistory.size > 1) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    itemsIndexed(state.canvasHistory) { idx, url ->
                                        val thumb = remember(url) { ImageCodec.decodeDataUrlToBitmap(url) }
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .border(
                                                    if (state.canvasGeneratedUrl == url) 2.dp else 1.dp,
                                                    if (state.canvasGeneratedUrl == url) GalleryInk else GalleryBorder
                                                )
                                                .clickable { onSelectVersion(url) }
                                        ) {
                                            if (thumb != null) {
                                                Image(
                                                    bitmap = thumb.asImageBitmap(),
                                                    contentDescription = "Version ${idx + 1}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onExportImage(
                                            state.canvasGeneratedUrl,
                                            state.canvasTitle.lowercase().replace(Regex("[^a-z0-9]+"), "-")
                                        )
                                    },
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, GalleryBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(AtelierIcons.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Export")
                                }
                                Button(
                                    onClick = onSaveToCloud,
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GalleryInk,
                                        contentColor = GalleryPaper
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(AtelierIcons.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Save Look")
                                }
                            }
                        }
                    }
                }

                // Controls Card
                Surface(
                    color = GalleryCard,
                    border = BorderStroke(1.dp, GalleryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.canvasTitle,
                            onValueChange = onUpdateTitle,
                            label = { Text("Look Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (state.canvasReferenceDataUrl == null) {
                            OutlinedButton(
                                onClick = {
                                    picker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, GalleryBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(AtelierIcons.AddPhoto, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text("Attach Optional Reference Photo")
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GalleryBorder)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reference image attached", style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = onClearReference) {
                                    Icon(AtelierIcons.Delete, contentDescription = "Remove reference")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            label = {
                                Text(
                                    if (state.canvasGeneratedUrl != null) "Follow-up edit or new prompt"
                                    else "Describe garments, colors, and gallery surface"
                                )
                            },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("3:4", "1:1", "4:3").forEach { ratio ->
                                OutlinedButton(
                                    onClick = { aspectRatio = ratio },
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, if (aspectRatio == ratio) GalleryInk else GalleryBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (aspectRatio == ratio) GalleryInk else GalleryPaper,
                                        contentColor = if (aspectRatio == ratio) GalleryPaper else GalleryInk
                                    )
                                ) {
                                    Text(ratio)
                                }
                            }
                            listOf("1K", "2K").forEach { sz ->
                                OutlinedButton(
                                    onClick = { imageSize = sz },
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, if (imageSize == sz) GalleryInk else GalleryBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (imageSize == sz) GalleryInk else GalleryPaper,
                                        contentColor = if (imageSize == sz) GalleryPaper else GalleryInk
                                    )
                                ) {
                                    Text(sz)
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
                                    .background(GalleryErrorBg)
                                    .padding(10.dp)
                            )
                        }

                        if (state.canvasGeneratedUrl != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onGenerate(prompt, true, aspectRatio, imageSize)
                                        prompt = ""
                                    },
                                    enabled = prompt.isNotBlank() && !state.isCanvasGenerating,
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GalleryInk,
                                        contentColor = GalleryPaper
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Edit Flat-Lay")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onGenerate(prompt, false, aspectRatio, imageSize)
                                        prompt = ""
                                    },
                                    enabled = prompt.isNotBlank() && !state.isCanvasGenerating,
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, GalleryInk)
                                ) {
                                    Text("New")
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    onGenerate(prompt, false, aspectRatio, imageSize)
                                    prompt = ""
                                },
                                enabled = prompt.isNotBlank() && !state.isCanvasGenerating,
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GalleryInk,
                                    contentColor = GalleryPaper
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (state.isCanvasGenerating) "Creating Flat-Lay..." else "Create Flat-Lay Image")
                            }
                        }
                    }
                }

                // Editorial Prompt Starters
                Text("EDITORIAL PROMPT STARTERS", style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
                starters.forEach { tpl ->
                    Surface(
                        color = GalleryCard,
                        border = BorderStroke(1.dp, GalleryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { prompt = tpl }
                    ) {
                        Text(
                            text = tpl,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}
