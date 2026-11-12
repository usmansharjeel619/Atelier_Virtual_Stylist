package com.atelier.virtualstylist.data.model

data class StyleBrief(
    val season: String = "Year-Round",
    val silhouette: String = "Tailored",
    val budgetTier: String = "Contemporary"
)

data class PaletteSwatch(
    val hex: String = "#141413",
    val name: String = "Espresso Ink",
    val role: String = "Primary"
)

data class OutfitPiece(
    val slot: String = "",
    val name: String = "",
    val color: String = "",
    val material: String = ""
)

data class OutfitBlueprint(
    val occasion: String = "Casual",
    val title: String = "",
    val stylingRationale: String = "",
    val flatLayDirection: String = "",
    val pieces: List<OutfitPiece> = emptyList()
)

data class StyleInsight(
    val fitAndProportion: String = "",
    val materialAndDrape: String = "",
    val howToWear: String = "",
    val whatToAvoid: String = ""
)

data class GarmentAnalysis(
    val garmentTitle: String = "",
    val category: String = "",
    val fabricNotes: String = "",
    val styleSummary: String = "",
    val palette: List<PaletteSwatch> = emptyList(),
    val styleInsight: StyleInsight? = null,
    val quickTips: List<String> = emptyList(),
    val outfits: List<OutfitBlueprint> = emptyList()
)

data class GeneratedLookState(
    val id: String,
    val occasion: String,
    val title: String,
    val stylingRationale: String,
    val flatLayDirection: String,
    val pieces: List<OutfitPiece>,
    val imageDataUrl: String? = null,
    val imageHistory: List<String> = emptyList(),
    val editPromptHistory: List<String> = emptyList(),
    val isGenerating: Boolean = false,
    val error: String? = null,
    val savedLookId: String? = null
)

data class SampleGarment(
    val id: String,
    val title: String,
    val subtitle: String,
    val sampleUrl: String,
    val defaultSeason: String
)

data class SavedGarmentRecord(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val category: String = "",
    val fabricNotes: String = "",
    val styleSummary: String = "",
    val season: String = "Year-Round",
    val silhouette: String = "Tailored",
    val budgetTier: String = "Contemporary",
    val palette: List<PaletteSwatch> = emptyList(),
    val styleInsight: StyleInsight? = null,
    val quickTips: List<String> = emptyList(),
    val sourceImageDataUrl: String = ""
)

data class SavedLookRecord(
    val id: String = "",
    val ownerId: String = "",
    val garmentId: String = "",
    val occasion: String = "Casual",
    val title: String = "",
    val stylingRationale: String = "",
    val pieces: List<OutfitPiece> = emptyList(),
    val flatLayImageDataUrl: String? = null,
    val editPromptHistory: List<String> = emptyList()
)

// API DTOs
data class AnalyzeRequest(
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
    val season: String,
    val silhouette: String,
    val budgetTier: String
)

data class InsightRequest(
    val imageBase64: String? = null,
    val mimeType: String = "image/jpeg",
    val garmentTitle: String,
    val category: String,
    val fabricNotes: String,
    val season: String,
    val silhouette: String
)

data class QuickTipsResponse(
    val quickTips: List<String> = emptyList()
)

data class GenerateFlatLayRequest(
    val garmentImageBase64: String,
    val garmentMimeType: String = "image/jpeg",
    val occasion: String,
    val garmentTitle: String,
    val lookTitle: String,
    val flatLayDirection: String,
    val pieces: List<OutfitPiece>,
    val season: String,
    val silhouette: String
)

data class EditFlatLayRequest(
    val baseImageBase64: String? = null,
    val baseMimeType: String = "image/jpeg",
    val sourceGarmentBase64: String? = null,
    val sourceGarmentMimeType: String = "image/jpeg",
    val editPrompt: String,
    val aspectRatio: String = "3:4",
    val imageSize: String = "1K"
)

data class FlatLayImageResponse(
    val imageDataUrl: String? = null,
    val caption: String? = null,
    val model: String? = null,
    val error: String? = null
)
