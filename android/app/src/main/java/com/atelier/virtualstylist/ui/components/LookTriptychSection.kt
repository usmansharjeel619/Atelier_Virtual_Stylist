package com.atelier.virtualstylist.ui.components

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.window.Dialog
import com.atelier.virtualstylist.data.model.GeneratedLookState
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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import android.graphics.Bitmap
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
            Color(0xFF8A6A42),
            Color(0xFF5A5852),
            Color(0xFFC5BFB3),
            Color(0xFF3D4856)
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
            .border(0.5.dp, GalleryBorder)
    ) {
        swatches.forEach { swatchColor ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
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
    onApplyEdit: (prompt: String, aspectRatio: String, imageSize: String) -> Unit,
    onSelectVersion: (lookId: String, imageUrl: String) -> Unit,
    onSaveLook: (GeneratedLookState) -> Unit,
    onExportImage: (dataUrl: String, fileName: String) -> Unit
) {
    if (looks.isEmpty()) return

    var isCompareMode by remember { mutableStateOf(false) }
    var leftLookId by remember { mutableStateOf("") }
    var rightLookId by remember { mutableStateOf("") }

    LaunchedEffect(looks) {
        if (looks.isEmpty()) {
            leftLookId = ""
            rightLookId = ""
            isCompareMode = false
        } else {
            if (looks.none { it.id == leftLookId }) {
                leftLookId = looks[0].id
            }
            if (looks.size < 2) {
                rightLookId = looks[0].id
                isCompareMode = false
            } else if (looks.none { it.id == rightLookId }) {
                rightLookId = looks[1].id
            }
        }
    }

    val canCompare = looks.size >= 2
    val leftLook = looks.firstOrNull { it.id == leftLookId } ?: looks.first()
    val rightLook = looks.firstOrNull { it.id == rightLookId } ?: looks.getOrElse(1) { looks.first() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FLAT-LAY LOOKBOOK", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
            Text("Three Curated Occasions", style = MaterialTheme.typography.headlineMedium, color = GalleryInk)

            if (canCompare) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GalleryBorder)
                        .background(GalleryCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { isCompareMode = false },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isCompareMode) GalleryInk else GalleryCard,
                            contentColor = if (!isCompareMode) GalleryPaper else GalleryMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Triptych View", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { isCompareMode = true },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompareMode) GalleryInk else GalleryCard,
                            contentColor = if (isCompareMode) GalleryPaper else GalleryMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(AtelierIcons.Compare, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Side-by-Side Compare", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (isCompareMode && canCompare) {
            // Look A & Look B Pair Selector with Swap
            Surface(
                color = GalleryCard,
                border = BorderStroke(1.dp, GalleryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("COMPARE PAIR", style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
                        OutlinedButton(
                            onClick = {
                                val prevLeft = leftLook.id
                                leftLookId = rightLook.id
                                rightLookId = prevLeft
                            },
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, GalleryBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(AtelierIcons.Swap, contentDescription = null, modifier = Modifier.size(14.dp), tint = GalleryInk)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Swap", style = MaterialTheme.typography.labelMedium, color = GalleryInk)
                        }
                    }

                    Text("LOOK A", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        looks.forEach { option ->
                            val selected = option.id == leftLook.id
                            OutlinedButton(
                                onClick = {
                                    if (option.id == rightLook.id) rightLookId = leftLook.id
                                    leftLookId = option.id
                                },
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, if (selected) GalleryInk else GalleryBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) GalleryInk else GalleryPaper,
                                    contentColor = if (selected) GalleryPaper else GalleryInk
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    option.occasion,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Text("LOOK B", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        looks.forEach { option ->
                            val selected = option.id == rightLook.id
                            OutlinedButton(
                                onClick = {
                                    if (option.id == leftLook.id) leftLookId = rightLook.id
                                    rightLookId = option.id
                                },
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, if (selected) GalleryInk else GalleryBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) GalleryInk else GalleryPaper,
                                    contentColor = if (selected) GalleryPaper else GalleryInk
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    option.occasion,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Side-by-Side Flat-Lay Visuals & Titles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Look A" to leftLook, "Look B" to rightLook).forEach { (slotLabel, look) ->
                    val isSaved = !look.savedLookId.isNullOrBlank()
                    val isSaving = savingLookId == look.id
                    Surface(
                        color = GalleryCard,
                        border = BorderStroke(1.dp, GalleryBorder),
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
                                    look.isGenerating -> {
                                        CircularProgressIndicator(strokeWidth = 2.dp, color = GalleryInk)
                                    }

                                    !look.imageDataUrl.isNullOrBlank() -> {
                                        val bmp = remember(look.imageDataUrl) {
                                            ImageCodec.decodeDataUrlToBitmap(look.imageDataUrl)
                                        }
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "${look.occasion} flat-lay: ${look.title}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    else -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(GalleryPaper)
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(look.occasion.uppercase(), style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                                            OutlinedButton(
                                                onClick = { onRetryFlatLay(look.id) },
                                                shape = RectangleShape,
                                                border = BorderStroke(1.dp, GalleryInk),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Generate", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = GalleryBorder)
                            OutfitColorPaletteBar(look = look)

                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "$slotLabel · ${look.occasion.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GalleryMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    look.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GalleryInk,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    look.stylingRationale,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GalleryMuted,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { onOpenRefine(look.id) },
                                        enabled = !look.isGenerating,
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GalleryInk,
                                            contentColor = GalleryPaper
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Refine", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = { onSaveLook(look) },
                                        enabled = !isSaving && !look.isGenerating,
                                        shape = RectangleShape,
                                        border = BorderStroke(1.dp, if (isSaved) GalleryBrass else GalleryBorder),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            if (isSaved) AtelierIcons.Check else AtelierIcons.BookmarkBorder,
                                            contentDescription = "Save to Wardrobe",
                                            tint = if (isSaved) GalleryBrass else GalleryInk,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (!look.imageDataUrl.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            onExportImage(
                                                look.imageDataUrl,
                                                "atelier-${look.occasion.lowercase().replace(" ", "-")}-outfit"
                                            )
                                        },
                                        shape = RectangleShape,
                                        border = BorderStroke(1.dp, GalleryBorder),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(AtelierIcons.Download, contentDescription = null, tint = GalleryInk, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download Outfit", style = MaterialTheme.typography.labelMedium, color = GalleryInk)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Aligned Piece-by-Piece Silhouette Comparison
            val orderedSlots = remember(leftLook.id, rightLook.id, leftLook.pieces, rightLook.pieces) {
                val slots = mutableListOf<String>()
                val seen = mutableSetOf<String>()
                for (p in leftLook.pieces + rightLook.pieces) {
                    val key = p.slot.trim()
                    if (key.isNotEmpty() && seen.add(key.lowercase())) {
                        slots.add(key)
                    }
                }
                slots
            }

            Surface(
                color = GalleryCard,
                border = BorderStroke(1.dp, GalleryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "PIECE-BY-PIECE SILHOUETTE COMPARISON",
                        style = MaterialTheme.typography.labelMedium,
                        color = GalleryBrass
                    )
                    HorizontalDivider(color = GalleryBorder)

                    orderedSlots.forEachIndexed { idx, slotName ->
                        val leftPiece = leftLook.pieces.firstOrNull { it.slot.equals(slotName, ignoreCase = true) }
                        val rightPiece = rightLook.pieces.firstOrNull { it.slot.equals(slotName, ignoreCase = true) }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(slotName.uppercase(), style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        leftPiece?.name ?: "—",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GalleryInk
                                    )
                                    if (leftPiece != null) {
                                        Text(
                                            "${leftPiece.color} · ${leftPiece.material}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GalleryMuted
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        rightPiece?.name ?: "—",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GalleryInk
                                    )
                                    if (rightPiece != null) {
                                        Text(
                                            "${rightPiece.color} · ${rightPiece.material}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GalleryMuted
                                        )
                                    }
                                }
                            }
                        }

                        if (idx < orderedSlots.lastIndex) {
                            HorizontalDivider(color = GalleryBorder)
                        }
                    }
                }
            }
        } else {
            looks.forEachIndexed { index, look ->
                val indexLabel = String.format("%02d", index + 1)
                val isSaved = !look.savedLookId.isNullOrBlank()
                val isSaving = savingLookId == look.id

                Surface(
                    color = GalleryCard,
                    border = BorderStroke(1.dp, GalleryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Flat-Lay Visual Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .background(GalleryStone),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                look.isGenerating -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, color = GalleryInk)
                                        Text(
                                            "Composing ${look.occasion} Flat-Lay",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = GalleryInk
                                        )
                                    }
                                }

                                !look.imageDataUrl.isNullOrBlank() -> {
                                    val bmp = remember(look.imageDataUrl) {
                                        ImageCodec.decodeDataUrlToBitmap(look.imageDataUrl)
                                    }
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "${look.occasion} flat-lay: ${look.title}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                else -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(GalleryPaper)
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                "EDITORIAL PIECE BOARD · ${look.occasion.uppercase()}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = GalleryBrass
                                            )
                                            Text(look.title, style = MaterialTheme.typography.headlineSmall)
                                        }

                                        if (!look.error.isNullOrBlank()) {
                                            Text(
                                                text = look.error,
                                                color = GalleryErrorInk,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(GalleryErrorBg)
                                                    .padding(12.dp)
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { onRetryFlatLay(look.id) },
                                            shape = RectangleShape,
                                            border = BorderStroke(1.dp, GalleryInk),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(AtelierIcons.Refresh, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                            Text("Generate Flat-Lay Photo")
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = GalleryBorder)
                        OutfitColorPaletteBar(look = look)

                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "$indexLabel · ${look.occasion.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GalleryMuted
                                )
                                if (look.editPromptHistory.isNotEmpty()) {
                                    Text(
                                        "Edited · v${look.imageHistory.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GalleryBrass
                                    )
                                }
                            }

                            Text(look.title, style = MaterialTheme.typography.headlineSmall, color = GalleryInk)
                            Text(look.stylingRationale, style = MaterialTheme.typography.bodyMedium, color = GalleryMuted)

                            HorizontalDivider(color = GalleryBorder)
                            Text("OUTFIT PIECES", style = MaterialTheme.typography.labelMedium, color = GalleryMuted)

                            look.pieces.forEach { piece ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(piece.slot, style = MaterialTheme.typography.bodyMedium, color = GalleryMuted)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(piece.name, style = MaterialTheme.typography.titleMedium, color = GalleryInk)
                                        Text(
                                            "${piece.color} · ${piece.material}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GalleryMuted
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = GalleryBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onOpenRefine(look.id) },
                                    enabled = !look.isGenerating,
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GalleryInk,
                                        contentColor = GalleryPaper
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(AtelierIcons.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Refine Prompt")
                                }

                                OutlinedButton(
                                    onClick = { onSaveLook(look) },
                                    enabled = !isSaving && !look.isGenerating,
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, if (isSaved) GalleryBrass else GalleryBorder)
                                ) {
                                    Icon(
                                        if (isSaved) AtelierIcons.Check else AtelierIcons.BookmarkBorder,
                                        contentDescription = "Save to Wardrobe",
                                        tint = if (isSaved) GalleryBrass else GalleryInk
                                    )
                                }
                            }
                            if (!look.imageDataUrl.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        onExportImage(
                                            look.imageDataUrl,
                                            "atelier-${look.occasion.lowercase().replace(" ", "-")}-outfit"
                                        )
                                    },
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, GalleryBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(AtelierIcons.Download, contentDescription = null, tint = GalleryInk, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download Outfit", style = MaterialTheme.typography.bodyMedium, color = GalleryInk)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Refine Prompt Dialog
    if (refiningLook != null) {
        var promptInput by remember(refiningLook.id) { mutableStateOf("") }
        var aspectRatio by remember { mutableStateOf("3:4") }
        var imageSize by remember { mutableStateOf("1K") }

        val suggestions = listOf(
            "Swap shoes for white leather sneakers",
            "Add a camel wool trench coat beside the look",
            "Replace the bag with an espresso woven leather tote"
        )

        Dialog(onDismissRequest = onCloseRefine) {
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
                            Text("PROMPT FLAT-LAY EDITOR", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                            Text(refiningLook.title, style = MaterialTheme.typography.headlineSmall)
                        }
                        IconButton(onClick = onCloseRefine) {
                            Icon(AtelierIcons.Close, contentDescription = "Close editor")
                        }
                    }
                    HorizontalDivider(color = GalleryBorder)

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (refiningLook.imageHistory.size > 1) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(refiningLook.imageHistory) { idx, histUrl ->
                                    val thumb = remember(histUrl) { ImageCodec.decodeDataUrlToBitmap(histUrl) }
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .border(
                                                if (refiningLook.imageDataUrl == histUrl) 2.dp else 1.dp,
                                                if (refiningLook.imageDataUrl == histUrl) GalleryInk else GalleryBorder
                                            )
                                            .clickable { onSelectVersion(refiningLook.id, histUrl) }
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

                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            label = { Text("Describe your flat-lay refinement") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        suggestions.forEach { hint ->
                            Surface(
                                color = GalleryCard,
                                border = BorderStroke(1.dp, GalleryBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { promptInput = hint }
                            ) {
                                Text(
                                    text = hint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("3:4", "1:1", "4:3").forEach { r ->
                                OutlinedButton(
                                    onClick = { aspectRatio = r },
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, if (aspectRatio == r) GalleryInk else GalleryBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (aspectRatio == r) GalleryInk else GalleryCard,
                                        contentColor = if (aspectRatio == r) GalleryPaper else GalleryInk
                                    )
                                ) {
                                    Text(r)
                                }
                            }
                            listOf("1K", "2K").forEach { sz ->
                                OutlinedButton(
                                    onClick = { imageSize = sz },
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, if (imageSize == sz) GalleryInk else GalleryBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (imageSize == sz) GalleryInk else GalleryCard,
                                        contentColor = if (imageSize == sz) GalleryPaper else GalleryInk
                                    )
                                ) {
                                    Text(sz)
                                }
                            }
                        }

                        if (!editError.isNullOrBlank()) {
                            Text(
                                text = editError,
                                color = GalleryErrorInk,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GalleryErrorBg)
                                    .padding(10.dp)
                            )
                        }

                        Button(
                            onClick = {
                                onApplyEdit(promptInput, aspectRatio, imageSize)
                                promptInput = ""
                            },
                            enabled = promptInput.isNotBlank() && !isApplyingEdit,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GalleryInk,
                                contentColor = GalleryPaper
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isApplyingEdit) "Applying Edit..." else "Apply Prompt Edit")
                        }
                    }
                }
            }
        }
    }
}
