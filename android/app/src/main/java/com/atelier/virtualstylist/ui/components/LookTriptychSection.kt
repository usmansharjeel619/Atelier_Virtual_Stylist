package com.atelier.virtualstylist.ui.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.atelier.virtualstylist.data.model.GeneratedLookState
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

private fun fashionColorToComposeColor(colorName: String, index: Int): Color {
    val lower = colorName.lowercase()
    return when {
        listOf("ivory", "ecru", "cream", "alabaster", "off-white", "bone").any { it in lower } -> Color(0xFFF3EFE6)
        listOf("white", "snow").any { it in lower } -> Color(0xFFFAFAFA)
        listOf("espresso", "chocolate", "mocha", "dark brown", "mahogany").any { it in lower } -> Color(0xFF3E2B23)
        listOf("cognac", "saddle", "tan", "chestnut", "caramel").any { it in lower } -> Color(0xFF9C6137)
        listOf("camel", "oat", "sand", "beige", "stone", "taupe", "khaki", "linen").any { it in lower } -> Color(0xFFC9B396)
        listOf("black", "onyx", "obsidian", "noir").any { it in lower } -> Color(0xFF181818)
        listOf("charcoal", "slate", "graphite").any { it in lower } -> Color(0xFF44464A)
        listOf("gray", "grey", "ash").any { it in lower } -> Color(0xFF8E9196)
        listOf("silver", "pewter", "chrome", "steel").any { it in lower } -> Color(0xFFBFC4CA)
        listOf("gold", "brass", "champagne").any { it in lower } -> Color(0xFFB7934F)
        listOf("navy", "midnight").any { it in lower } -> Color(0xFF1D2944)
        listOf("indigo", "denim", "cobalt", "blue").any { it in lower } -> Color(0xFF2E4775)
        listOf("burgundy", "oxblood", "wine", "merlot", "maroon").any { it in lower } -> Color(0xFF5E202B)
        listOf("crimson", "ruby", "red", "cherry").any { it in lower } -> Color(0xFF9E2A2F)
        listOf("terracotta", "rust", "copper", "sienna").any { it in lower } -> Color(0xFFA84E32)
        listOf("blush", "rose", "pink", "mauve").any { it in lower } -> Color(0xFFD6A6A8)
        listOf("olive", "moss", "sage", "emerald", "forest", "green").any { it in lower } -> Color(0xFF4A5D46)
        else -> listOf(
            Color(0xFF1F1E1C),
            Color(0xFF2B4BFF),
            Color(0xFFD4FF38),
            Color(0xFFFF3D6E),
            Color(0xFF5A5852)
        )[index % 5]
    }
}

private fun extractDominantColorsFromBitmap(bitmap: Bitmap, maxColors: Int = 5): List<Color> {
    val scaled = Bitmap.createScaledBitmap(bitmap, 40, 40, true)
    val buckets = linkedMapOf<Int, FloatArray>()
    for (y in 2 until 38) {
        for (x in 2 until 38) {
            val px = scaled.getPixel(x, y)
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val chroma = max(r, max(g, b)) - min(r, min(g, b))
            if (r > 228 && g > 224 && b > 214 && chroma < 24) continue
            val qr = (r / 24) * 24
            val qg = (g / 24) * 24
            val qb = (b / 24) * 24
            val key = (qr shl 16) or (qg shl 8) or qb
            val w = 1f + chroma / 110f
            val cur = buckets[key]
            if (cur == null) {
                buckets[key] = floatArrayOf(r * w, g * w, b * w, w)
            } else {
                cur[0] += r * w
                cur[1] += g * w
                cur[2] += b * w
                cur[3] += w
            }
        }
    }
    if (scaled != bitmap) scaled.recycle()

    val sorted = buckets.values
        .map { floatArrayOf(it[0] / it[3], it[1] / it[3], it[2] / it[3], it[3]) }
        .sortedByDescending { it[3] }

    val chosen = mutableListOf<FloatArray>()
    for (cand in sorted) {
        val close = chosen.any { c ->
            val dr = c[0] - cand[0]
            val dg = c[1] - cand[1]
            val db = c[2] - cand[2]
            sqrt(dr * dr + dg * dg + db * db) < 42f
        }
        if (!close) {
            chosen.add(cand)
            if (chosen.size >= maxColors) break
        }
    }
    return chosen.map {
        Color(
            red = it[0].roundToInt().coerceIn(0, 255),
            green = it[1].roundToInt().coerceIn(0, 255),
            blue = it[2].roundToInt().coerceIn(0, 255)
        )
    }
}

@Composable
private fun OutfitColorPaletteBar(look: GeneratedLookState) {
    val swatches = remember(look.id, look.imageDataUrl, look.pieces) {
        val bmp = look.imageDataUrl?.let { ImageCodec.decodeDataUrlToBitmap(it) }
        val fromImage = if (bmp != null) {
            try {
                extractDominantColorsFromBitmap(bmp, 5)
            } finally {
                bmp.recycle()
            }
        } else {
            emptyList()
        }
        if (fromImage.isNotEmpty()) {
            fromImage
        } else {
            look.pieces.mapIndexed { idx, p -> fashionColorToComposeColor(p.color, idx) }
                .distinct()
                .take(5)
        }
    }
    if (swatches.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        swatches.forEach { swatchColor ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(swatchColor)
            )
        }
    }
}

@Composable
fun LookTriptychSection(
    looks: List<GeneratedLookState>,
    savingLookId: String?,
    refiningLook: GeneratedLookState?,
    isApplyingEdit: Boolean,
    editError: String?,
    onRetryFlatLay: (String) -> Unit,
    onOpenRefine: (String) -> Unit,
    onCloseRefine: () -> Unit,
    onOpenInEditor: (GeneratedLookState) -> Unit,
    onApplyEdit: (prompt: String, aspectRatio: String, imageSize: String) -> Unit,
    onSelectVersion: (lookId: String, imageUrl: String) -> Unit,
    onSaveLook: (GeneratedLookState) -> Unit,
    onExportImage: (dataUrl: String, fileName: String) -> Unit
) {
    if (looks.isEmpty()) return

    var selectedIdx by remember { mutableIntStateOf(0) }
    var piecesExpanded by remember { mutableStateOf(false) }
    var isCompareMode by remember { mutableStateOf(false) }
    var leftLookId by remember { mutableStateOf("") }
    var rightLookId by remember { mutableStateOf("") }

    LaunchedEffect(looks) {
        if (selectedIdx >= looks.size) selectedIdx = 0
        if (looks.isNotEmpty()) {
            if (looks.none { it.id == leftLookId }) leftLookId = looks[0].id
            if (looks.size < 2) {
                rightLookId = looks[0].id
                isCompareMode = false
            } else if (looks.none { it.id == rightLookId }) {
                rightLookId = looks[1].id
            }
        }
    }

    val canCompare = looks.size >= 2
    val activeLook = looks.getOrElse(selectedIdx) { looks.first() }
    val leftLook = looks.firstOrNull { it.id == leftLookId } ?: looks.first()
    val rightLook = looks.firstOrNull { it.id == rightLookId } ?: looks.getOrElse(1) { looks.first() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Occasion Switcher + 1v1 Toggle on 1 Clean Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(looks) { idx, item ->
                    val active = !isCompareMode && idx == selectedIdx
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (active) GalleryInk else GalleryCard)
                            .border(1.5.dp, if (active) GalleryInk else GalleryHairline, PillShape)
                            .clickable {
                                isCompareMode = false
                                selectedIdx = idx
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item.occasion,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) NeonLime else GalleryInk
                        )
                    }
                }
            }

            if (canCompare) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(if (isCompareMode) GalleryBrass else GalleryCard)
                        .border(1.5.dp, if (isCompareMode) GalleryBrass else GalleryHairline, PillShape)
                        .clickable { isCompareMode = !isCompareMode }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            AtelierIcons.Compare,
                            contentDescription = "Compare 1v1",
                            tint = if (isCompareMode) Color.White else GalleryInk,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "1v1",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCompareMode) Color.White else GalleryInk
                        )
                    }
                }
            }
        }

        if (isCompareMode && canCompare) {
            // 1v1 Comparison Controls + Side-by-Side Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(looks) { _, opt ->
                        val picked = opt.id == leftLook.id
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (picked) NeonLime else GalleryCard)
                                .border(1.dp, GalleryInk, PillShape)
                                .clickable {
                                    if (opt.id == rightLook.id) rightLookId = leftLook.id
                                    leftLookId = opt.id
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(opt.occasion, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
                IconButton(
                    onClick = {
                        val tmp = leftLook.id
                        leftLookId = rightLook.id
                        rightLookId = tmp
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(GalleryCard)
                        .border(1.dp, GalleryHairline, CircleShape)
                ) {
                    Icon(AtelierIcons.Swap, contentDescription = "Swap", modifier = Modifier.size(16.dp))
                }
                LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(looks) { _, opt ->
                        val picked = opt.id == rightLook.id
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (picked) GalleryBrass else GalleryCard)
                                .border(1.dp, if (picked) GalleryBrass else GalleryHairline, PillShape)
                                .clickable {
                                    if (opt.id == leftLook.id) leftLookId = rightLook.id
                                    rightLookId = opt.id
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                opt.occasion,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (picked) Color.White else GalleryInk,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(leftLook, rightLook).forEach { sideLook ->
                    val isSaved = !sideLook.savedLookId.isNullOrBlank()
                    AtelierSurfaceCard(
                        containerColor = GalleryCard,
                        borderColor = GalleryHairline,
                        shape = CardShape,
                        modifier = Modifier.weight(1f)
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
                                    sideLook.isGenerating -> CircularProgressIndicator(strokeWidth = 2.dp, color = GalleryInk)
                                    !sideLook.imageDataUrl.isNullOrBlank() -> {
                                        val bmp = remember(sideLook.imageDataUrl) {
                                            ImageCodec.decodeDataUrlToBitmap(sideLook.imageDataUrl)
                                        }
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = sideLook.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    else -> {
                                        OutlinedButton(
                                            onClick = { onRetryFlatLay(sideLook.id) },
                                            shape = PillShape
                                        ) {
                                            Text("Retry", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                            OutfitColorPaletteBar(look = sideLook)
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(sideLook.occasion, style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                                Text(sideLook.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { onOpenInEditor(sideLook) },
                                        enabled = !sideLook.isGenerating,
                                        shape = PillShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = GalleryInk),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                                    }
                                    IconButton(
                                        onClick = { onSaveLook(sideLook) },
                                        enabled = !sideLook.isGenerating,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (isSaved) AtelierIcons.Check else AtelierIcons.BookmarkBorder,
                                            contentDescription = "Save",
                                            tint = if (isSaved) HotCoral else GalleryInk,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Single Active Visual Fit Card (Zero Scroll Fatigue!)
            val isSaved = !activeLook.savedLookId.isNullOrBlank()
            val isSaving = savingLookId == activeLook.id

            AtelierSurfaceCard(
                containerColor = GalleryCard,
                borderColor = GalleryHairline,
                shape = CardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 3.8f)
                            .background(GalleryStone),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            activeLook.isGenerating -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, color = GalleryInk)
                                    Text("Styling ${activeLook.occasion}...", style = MaterialTheme.typography.titleMedium)
                                }
                            }

                            !activeLook.imageDataUrl.isNullOrBlank() -> {
                                val bmp = remember(activeLook.imageDataUrl) {
                                    ImageCodec.decodeDataUrlToBitmap(activeLook.imageDataUrl)
                                }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = activeLook.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Top-Right Overlaid Save + Download Icons
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(GalleryCard.copy(alpha = 0.92f))
                                            .clickable {
                                                onExportImage(
                                                    activeLook.imageDataUrl,
                                                    "atelier-${activeLook.occasion.lowercase().replace(" ", "-")}-outfit"
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(AtelierIcons.Download, contentDescription = "Download", tint = GalleryInk, modifier = Modifier.size(18.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isSaved) HotCoral else GalleryCard.copy(alpha = 0.92f))
                                            .clickable(enabled = !isSaving) { onSaveLook(activeLook) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isSaved) AtelierIcons.Check else AtelierIcons.BookmarkBorder,
                                            contentDescription = "Save",
                                            tint = if (isSaved) Color.White else GalleryInk,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            else -> {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (!activeLook.error.isNullOrBlank()) {
                                        Text(
                                            text = activeLook.error,
                                            color = GalleryErrorInk,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(InnerShape)
                                                .background(GalleryErrorBg)
                                                .padding(12.dp)
                                        )
                                    }
                                    Button(
                                        onClick = { onRetryFlatLay(activeLook.id) },
                                        shape = PillShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = GalleryInk)
                                    ) {
                                        Icon(AtelierIcons.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generate Photo", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    OutfitColorPaletteBar(look = activeLook)

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(activeLook.title, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = activeLook.stylingRationale,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GalleryMuted,
                            maxLines = if (piecesExpanded) 10 else 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Tap-to-Expand Pieces Accordion (No text clutter by default!)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(InnerShape)
                                .background(GalleryPaper)
                                .clickable { piecesExpanded = !piecesExpanded }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Pieces (${activeLook.pieces.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GalleryInk
                                    )
                                    Text(
                                        if (piecesExpanded) "Hide ↑" else "Show all ↓",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GalleryBrass
                                    )
                                }
                                if (piecesExpanded) {
                                    HorizontalDivider(thickness = 1.dp, color = GalleryHairline)
                                    activeLook.pieces.forEach { piece ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(piece.slot, style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
                                            Text(
                                                "${piece.name} · ${piece.color}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = GalleryInk
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Primary Thumb Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onOpenInEditor(activeLook) },
                                enabled = !activeLook.isGenerating,
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = GalleryInk),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                            ) {
                                Icon(AtelierIcons.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit in Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = { onOpenRefine(activeLook.id) },
                                enabled = !activeLook.isGenerating,
                                shape = PillShape,
                                border = BorderStroke(1.5.dp, GalleryInk),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                            ) {
                                Text("Quick Tweak", style = MaterialTheme.typography.labelMedium, color = GalleryInk)
                            }
                        }
                    }
                }
            }
        }
    }

    if (refiningLook != null) {
        var promptInput by remember(refiningLook.id) { mutableStateOf("") }
        var aspectRatio by remember { mutableStateOf("3:4") }
        var imageSize by remember { mutableStateOf("1K") }

        val suggestions = listOf(
            "Swap shoes for chunky silver sneakers",
            "Add oversized leather moto jacket",
            "Replace bag with metallic mini baguette"
        )

        Dialog(onDismissRequest = onCloseRefine) {
            AtelierSurfaceCard(
                containerColor = GalleryPaper,
                borderColor = GalleryInk,
                shape = CardShape
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quick Tweak", style = MaterialTheme.typography.headlineSmall)
                        IconButton(onClick = onCloseRefine, modifier = Modifier.size(30.dp)) {
                            Icon(AtelierIcons.Close, contentDescription = "Close")
                        }
                    }

                    if (refiningLook.imageHistory.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(refiningLook.imageHistory) { idx, histUrl ->
                                val thumb = remember(histUrl) { ImageCodec.decodeDataUrlToBitmap(histUrl) }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            if (refiningLook.imageDataUrl == histUrl) 2.dp else 1.dp,
                                            if (refiningLook.imageDataUrl == histUrl) GalleryBrass else GalleryHairline,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectVersion(refiningLook.id, histUrl) }
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

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text("What should we change?") },
                        minLines = 2,
                        shape = InnerShape,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(suggestions) { _, hint ->
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(GalleryCard)
                                    .border(1.dp, GalleryHairline, PillShape)
                                    .clickable { promptInput = hint }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(hint, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (!editError.isNullOrBlank()) {
                        Text(editError, color = GalleryErrorInk, style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        onClick = {
                            onApplyEdit(promptInput, aspectRatio, imageSize)
                            promptInput = ""
                        },
                        enabled = promptInput.isNotBlank() && !isApplyingEdit,
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = GalleryInk),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(if (isApplyingEdit) "Applying..." else "Apply Tweak", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
