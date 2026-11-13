package com.atelier.virtualstylist.ui

import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.atelier.virtualstylist.data.model.AnalyzeRequest
import com.atelier.virtualstylist.data.model.EditFlatLayRequest
import com.atelier.virtualstylist.data.model.GarmentAnalysis
import com.atelier.virtualstylist.data.model.GenerateFlatLayRequest
import com.atelier.virtualstylist.data.model.GeneratedLookState
import com.atelier.virtualstylist.data.model.InsightRequest
import com.atelier.virtualstylist.data.model.OutfitPiece
import com.atelier.virtualstylist.data.model.PaletteSwatch
import com.atelier.virtualstylist.data.model.SampleGarment
import com.atelier.virtualstylist.data.model.SavedGarmentRecord
import com.atelier.virtualstylist.data.model.SavedLookRecord
import com.atelier.virtualstylist.data.model.StyleBrief
import com.atelier.virtualstylist.data.network.ServerConfigStore
import com.atelier.virtualstylist.data.network.StylistApiService
import com.atelier.virtualstylist.data.repository.WardrobeRepository
import com.atelier.virtualstylist.util.ImageCodec
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AtelierTab {
    STUDIO, EDITOR, WARDROBE
}

data class StudioUiState(
    val activeTab: AtelierTab = AtelierTab.STUDIO,
    val studioShowResults: Boolean = false,
    val sourceImageDataUrl: String? = null,
    val activeGarmentLabel: String? = null,
    val activeGarmentId: String? = null,
    val brief: StyleBrief = StyleBrief(),
    val isAnalyzing: Boolean = false,
    val analyzeError: String? = null,
    val analysis: GarmentAnalysis? = null,
    val isLoadingInsight: Boolean = false,
    val insightError: String? = null,
    val isLoadingQuickTips: Boolean = false,
    val quickTipsError: String? = null,
    val looks: List<GeneratedLookState> = emptyList(),
    val refiningLookId: String? = null,
    val isApplyingEdit: Boolean = false,
    val editError: String? = null,
    val savingLookId: String? = null,
    val toastMessage: String? = null,
    // Server configuration state
    val serverUrl: String = "",
    val serverOnline: Boolean? = null,
    val isCheckingServer: Boolean = false,
    val showServerDialog: Boolean = false,
    // Flat-Lay Editor tab
    val canvasTitle: String = "Custom Fit Remix",
    val canvasReferenceDataUrl: String? = null,
    val canvasGeneratedUrl: String? = null,
    val canvasHistory: List<String> = emptyList(),
    val canvasPrompts: List<String> = emptyList(),
    val isCanvasGenerating: Boolean = false,
    val canvasError: String? = null,
    // Wardrobe tab
    val user: FirebaseUser? = null,
    val isSigningIn: Boolean = false,
    val authError: String? = null,
    val isFirebaseConfigured: Boolean = false,
    val wardrobeGarments: List<SavedGarmentRecord> = emptyList(),
    val selectedWardrobeGarmentId: String? = null,
    val wardrobeLooks: List<SavedLookRecord> = emptyList(),
    val wardrobeSeasonFilter: String = "All"
)

class StylistViewModel(application: Application) : AndroidViewModel(application) {

    private var api: StylistApiService =
        StylistApiService.create(ServerConfigStore.getBaseUrl(application))
    private val repository = WardrobeRepository(application)
    private val gson = Gson()

    private val _uiState = MutableStateFlow(
        StudioUiState(
            isFirebaseConfigured = repository.isFirebaseConfigured,
            serverUrl = ServerConfigStore.getBaseUrl(application).trimEnd('/')
        )
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val sampleGarments: List<SampleGarment> = listOf(
        SampleGarment(
            id = "sample-patterned-skirt",
            title = "Geometric Pleated Silk Midi Skirt",
            subtitle = "Cobalt · Ochre · Espresso",
            sampleUrl = "samples/sample_patterned_skirt_1794515920648.jpg",
            assetPath = "samples/sample_patterned_skirt_1794515920648.jpg",
            defaultSeason = "Year-Round"
        ),
        SampleGarment(
            id = "sample-emerald-blazer",
            title = "Emerald Jacquard Satin-Lapel Blazer",
            subtitle = "Emerald · Onyx · Tortoise",
            sampleUrl = "samples/sample_emerald_blazer_1794515920362.jpg",
            assetPath = "samples/sample_emerald_blazer_1794515920362.jpg",
            defaultSeason = "Autumn/Winter"
        ),
        SampleGarment(
            id = "sample-tartan-trousers",
            title = "Rust & Navy Tartan Pleated Trousers",
            subtitle = "Rust · Navy · Cream",
            sampleUrl = "samples/sample_tartan_trousers_1794515921817.jpg",
            assetPath = "samples/sample_tartan_trousers_1794515921817.jpg",
            defaultSeason = "Autumn/Winter"
        )
    )

    init {
        viewModelScope.launch {
            runCatching { repository.checkPendingOAuthResult() }
            repository.observeAuthUser().collect { user ->
                _uiState.update { it.copy(user = user, isSigningIn = false) }
                if (user != null) {
                    observeWardrobe(user.uid)
                } else {
                    _uiState.update {
                        it.copy(
                            wardrobeGarments = emptyList(),
                            wardrobeLooks = emptyList(),
                            selectedWardrobeGarmentId = null
                        )
                    }
                }
            }
        }
        checkServerHealth()
    }

    fun openServerDialog() {
        _uiState.update { it.copy(showServerDialog = true) }
    }

    fun closeServerDialog() {
        _uiState.update { it.copy(showServerDialog = false) }
    }

    fun updateAndCheckServerUrl(rawUrl: String) {
        val clean = ServerConfigStore.setBaseUrl(getApplication(), rawUrl)
        api = StylistApiService.create(clean)
        _uiState.update { it.copy(serverUrl = clean.trimEnd('/'), showServerDialog = false) }
        checkServerHealth()
    }

    fun resetServerUrl() {
        val clean = ServerConfigStore.resetBaseUrl(getApplication())
        api = StylistApiService.create(clean)
        _uiState.update { it.copy(serverUrl = clean.trimEnd('/')) }
        checkServerHealth()
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingServer = true) }
            val ok = runCatching {
                val res = api.healthCheck()
                res.isSuccessful && res.body()?.ok == true
            }.getOrDefault(false)
            _uiState.update { it.copy(isCheckingServer = false, serverOnline = ok) }
        }
    }

    private fun observeWardrobe(uid: String) {
        viewModelScope.launch {
            runCatching {
                repository.observeGarments(uid).collect { items ->
                    _uiState.update { state ->
                        val nextSelected = state.selectedWardrobeGarmentId
                            ?.takeIf { id -> items.any { it.id == id } }
                            ?: items.firstOrNull()?.id
                        state.copy(
                            wardrobeGarments = items,
                            selectedWardrobeGarmentId = nextSelected
                        )
                    }
                    val targetId = _uiState.value.selectedWardrobeGarmentId
                    if (targetId != null) {
                        observeSelectedGarmentLooks(uid, targetId)
                    }
                }
            }.onFailure { err ->
                showToast(err.message ?: "Unable to sync wardrobe.")
            }
        }
    }

    private var looksJob: kotlinx.coroutines.Job? = null

    private fun observeSelectedGarmentLooks(uid: String, garmentId: String) {
        looksJob?.cancel()
        looksJob = viewModelScope.launch {
            runCatching {
                repository.observeLooks(uid, garmentId).collect { looks ->
                    _uiState.update { it.copy(wardrobeLooks = looks) }
                }
            }
        }
    }

    fun selectTab(tab: AtelierTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setStudioShowResults(show: Boolean) {
        _uiState.update { it.copy(studioShowResults = show) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun updateBrief(brief: StyleBrief) {
        _uiState.update { it.copy(brief = brief) }
    }

    fun onPickGarmentUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val encoded = ImageCodec.encodeUri(getApplication(), uri)
                _uiState.update {
                    it.copy(
                        studioShowResults = false,
                        sourceImageDataUrl = encoded.dataUrl,
                        activeGarmentLabel = "Uploaded Piece",
                        activeGarmentId = null,
                        analysis = null,
                        looks = emptyList(),
                        analyzeError = null
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(analyzeError = err.message ?: "Unable to read photo.") }
            }
        }
    }

    fun onSelectSample(sample: SampleGarment) {
        viewModelScope.launch {
            _uiState.update { it.copy(analyzeError = null) }
            runCatching {
                val encoded = ImageCodec.loadAssetAndEncode(getApplication(), sample.assetPath)
                _uiState.update {
                    it.copy(
                        studioShowResults = false,
                        sourceImageDataUrl = encoded.dataUrl,
                        activeGarmentLabel = sample.title,
                        activeGarmentId = null,
                        brief = it.brief.copy(season = sample.defaultSeason),
                        analysis = null,
                        looks = emptyList()
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(analyzeError = err.message ?: "Unable to load sample.") }
            }
        }
    }

    fun clearPiece() {
        _uiState.update {
            it.copy(
                studioShowResults = false,
                sourceImageDataUrl = null,
                activeGarmentLabel = null,
                activeGarmentId = null,
                analysis = null,
                looks = emptyList(),
                analyzeError = null
            )
        }
    }

    fun analyzeAndStyle() {
        val state = _uiState.value
        val sourceUrl = state.sourceImageDataUrl ?: return
        val (mime, base64) = ImageCodec.splitDataUrl(sourceUrl)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    analyzeError = null,
                    analysis = null,
                    looks = emptyList()
                )
            }

            try {
                val response = api.analyzeGarment(
                    AnalyzeRequest(
                        imageBase64 = base64,
                        mimeType = mime,
                        season = state.brief.season,
                        silhouette = state.brief.silhouette,
                        budgetTier = state.brief.budgetTier
                    )
                )

                if (!response.isSuccessful || response.body() == null) {
                    val errText = extractServerError(response.errorBody()?.string())
                    throw IllegalStateException(errText ?: "Garment analysis failed (HTTP ${response.code()}).")
                }

                val analysis = response.body()!!
                val newGarmentId = "garment_${System.currentTimeMillis()}"
                val initialLooks = analysis.outfits.mapIndexed { idx, blueprint ->
                    GeneratedLookState(
                        id = "look_${idx}_${System.currentTimeMillis()}",
                        occasion = blueprint.occasion,
                        title = blueprint.title,
                        stylingRationale = blueprint.stylingRationale,
                        flatLayDirection = blueprint.flatLayDirection,
                        pieces = blueprint.pieces,
                        isGenerating = true
                    )
                }

                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        studioShowResults = true,
                        serverOnline = true,
                        analysis = analysis,
                        activeGarmentLabel = analysis.garmentTitle,
                        activeGarmentId = newGarmentId,
                        looks = initialLooks
                    )
                }

                initialLooks.map { look ->
                    async {
                        generateFlatLayInternal(look, sourceUrl, analysis.garmentTitle, state.brief)
                    }
                }.awaitAll()
            } catch (err: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        analyzeError = err.message ?: "Unable to analyze garment."
                    )
                }
            }
        }
    }

    fun retryFlatLay(lookId: String) {
        val state = _uiState.value
        val look = state.looks.find { it.id == lookId } ?: return
        val sourceUrl = state.sourceImageDataUrl ?: return
        val garmentTitle = state.analysis?.garmentTitle ?: state.activeGarmentLabel ?: "Statement Garment"

        viewModelScope.launch {
            generateFlatLayInternal(look, sourceUrl, garmentTitle, state.brief)
        }
    }

    private suspend fun generateFlatLayInternal(
        look: GeneratedLookState,
        sourceUrl: String,
        garmentTitle: String,
        brief: StyleBrief
    ) {
        _uiState.update { state ->
            state.copy(
                looks = state.looks.map {
                    if (it.id == look.id) it.copy(isGenerating = true, error = null) else it
                }
            )
        }

        val (mime, base64) = ImageCodec.splitDataUrl(sourceUrl)
        try {
            val response = api.generateFlatLay(
                GenerateFlatLayRequest(
                    garmentImageBase64 = base64,
                    garmentMimeType = mime,
                    occasion = look.occasion,
                    garmentTitle = garmentTitle,
                    lookTitle = look.title,
                    flatLayDirection = look.flatLayDirection,
                    pieces = look.pieces,
                    season = brief.season,
                    silhouette = brief.silhouette
                )
            )
            val body = response.body()
            if (!response.isSuccessful || body?.imageDataUrl == null) {
                val errMsg = body?.error ?: extractServerError(response.errorBody()?.string())
                    ?: "Could not generate flat-lay image."
                _uiState.update { state ->
                    state.copy(
                        looks = state.looks.map {
                            if (it.id == look.id) it.copy(isGenerating = false, error = errMsg) else it
                        }
                    )
                }
                return
            }

            val imgUrl = body.imageDataUrl
            _uiState.update { state ->
                state.copy(
                    looks = state.looks.map {
                        if (it.id == look.id) {
                            it.copy(
                                isGenerating = false,
                                imageDataUrl = imgUrl,
                                imageHistory = listOf(imgUrl),
                                error = null
                            )
                        } else it
                    }
                )
            }
        } catch (err: Exception) {
            _uiState.update { state ->
                state.copy(
                    looks = state.looks.map {
                        if (it.id == look.id) {
                            it.copy(isGenerating = false, error = err.message ?: "Network error.")
                        } else it
                    }
                )
            }
        }
    }

    fun refreshStyleInsight() {
        val state = _uiState.value
        val current = state.analysis ?: return
        val sourceSplit = state.sourceImageDataUrl?.let { ImageCodec.splitDataUrl(it) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInsight = true, insightError = null) }
            try {
                val res = api.fetchStyleInsight(
                    InsightRequest(
                        imageBase64 = sourceSplit?.second,
                        mimeType = sourceSplit?.first ?: "image/jpeg",
                        garmentTitle = current.garmentTitle,
                        category = current.category,
                        fabricNotes = current.fabricNotes,
                        season = state.brief.season,
                        silhouette = state.brief.silhouette
                    )
                )
                if (!res.isSuccessful || res.body() == null) {
                    throw IllegalStateException(
                        extractServerError(res.errorBody()?.string()) ?: "Unable to refresh Style Insight."
                    )
                }
                _uiState.update {
                    it.copy(
                        isLoadingInsight = false,
                        analysis = it.analysis?.copy(styleInsight = res.body())
                    )
                }
            } catch (err: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingInsight = false,
                        insightError = err.message ?: "Unable to refresh Style Insight."
                    )
                }
            }
        }
    }

    fun refreshQuickTips() {
        val state = _uiState.value
        val current = state.analysis ?: return
        val sourceSplit = state.sourceImageDataUrl?.let { ImageCodec.splitDataUrl(it) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuickTips = true, quickTipsError = null) }
            try {
                val res = api.fetchQuickTips(
                    InsightRequest(
                        imageBase64 = sourceSplit?.second,
                        mimeType = sourceSplit?.first ?: "image/jpeg",
                        garmentTitle = current.garmentTitle,
                        category = current.category,
                        fabricNotes = current.fabricNotes,
                        season = state.brief.season,
                        silhouette = state.brief.silhouette
                    )
                )
                if (!res.isSuccessful || res.body() == null) {
                    throw IllegalStateException(
                        extractServerError(res.errorBody()?.string()) ?: "Unable to refresh Quick Tips."
                    )
                }
                _uiState.update {
                    it.copy(
                        isLoadingQuickTips = false,
                        analysis = it.analysis?.copy(quickTips = res.body()!!.quickTips)
                    )
                }
            } catch (err: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingQuickTips = false,
                        quickTipsError = err.message ?: "Unable to refresh Quick Tips."
                    )
                }
            }
        }
    }

    fun openRefineLook(lookId: String) {
        _uiState.update { it.copy(refiningLookId = lookId, editError = null) }
    }

    fun closeRefineLook() {
        _uiState.update { it.copy(refiningLookId = null, editError = null) }
    }

    fun openLookInEditor(look: GeneratedLookState) {
        val initialHistory = look.imageHistory.ifEmpty {
            listOfNotNull(look.imageDataUrl)
        }
        _uiState.update {
            it.copy(
                activeTab = AtelierTab.EDITOR,
                canvasTitle = look.title.ifBlank { "${look.occasion} Remix" },
                canvasReferenceDataUrl = it.sourceImageDataUrl,
                canvasGeneratedUrl = look.imageDataUrl,
                canvasHistory = initialHistory,
                canvasPrompts = look.editPromptHistory,
                canvasError = null
            )
        }
    }

    fun applyPromptEdit(prompt: String, aspectRatio: String, imageSize: String) {
        val state = _uiState.value
        val look = state.looks.find { it.id == state.refiningLookId } ?: return
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingEdit = true, editError = null) }
            try {
                val baseSplit = look.imageDataUrl?.let { ImageCodec.splitDataUrl(it) }
                val anchorSplit = state.sourceImageDataUrl?.let { ImageCodec.splitDataUrl(it) }

                val res = api.editFlatLay(
                    EditFlatLayRequest(
                        baseImageBase64 = baseSplit?.second,
                        baseMimeType = baseSplit?.first ?: "image/jpeg",
                        sourceGarmentBase64 = anchorSplit?.second,
                        sourceGarmentMimeType = anchorSplit?.first ?: "image/jpeg",
                        editPrompt = trimmed,
                        aspectRatio = aspectRatio,
                        imageSize = imageSize
                    )
                )
                val body = res.body()
                if (!res.isSuccessful || body?.imageDataUrl == null) {
                    throw IllegalStateException(
                        body?.error ?: extractServerError(res.errorBody()?.string()) ?: "Edit failed."
                    )
                }

                val updatedImg = body.imageDataUrl
                _uiState.update { current ->
                    current.copy(
                        isApplyingEdit = false,
                        looks = current.looks.map { l ->
                            if (l.id == look.id) {
                                l.copy(
                                    imageDataUrl = updatedImg,
                                    imageHistory = l.imageHistory + updatedImg,
                                    editPromptHistory = l.editPromptHistory + trimmed,
                                    error = null
                                )
                            } else l
                        }
                    )
                }
            } catch (err: Exception) {
                _uiState.update {
                    it.copy(
                        isApplyingEdit = false,
                        editError = err.message ?: "Unable to apply flat-lay edit."
                    )
                }
            }
        }
    }

    fun selectLookVersion(lookId: String, imageUrl: String) {
        _uiState.update { state ->
            state.copy(
                looks = state.looks.map {
                    if (it.id == lookId) it.copy(imageDataUrl = imageUrl) else it
                }
            )
        }
    }

    // Flat-Lay Editor operations
    fun updateCanvasTitle(title: String) {
        _uiState.update { it.copy(canvasTitle = title) }
    }

    fun pickCanvasReferenceUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val encoded = ImageCodec.encodeUri(getApplication(), uri)
                _uiState.update { it.copy(canvasReferenceDataUrl = encoded.dataUrl, canvasError = null) }
            }.onFailure { err ->
                _uiState.update { it.copy(canvasError = err.message) }
            }
        }
    }

    fun clearCanvasReference() {
        _uiState.update { it.copy(canvasReferenceDataUrl = null) }
    }

    fun generateOnCanvas(prompt: String, editCurrent: Boolean, aspectRatio: String, imageSize: String) {
        val state = _uiState.value
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCanvasGenerating = true, canvasError = null) }
            try {
                val baseUrl = if (editCurrent && state.canvasGeneratedUrl != null) {
                    state.canvasGeneratedUrl
                } else {
                    state.canvasReferenceDataUrl
                }
                val baseSplit = baseUrl?.let { ImageCodec.splitDataUrl(it) }
                val anchorSplit = state.canvasReferenceDataUrl?.takeIf { editCurrent }?.let {
                    ImageCodec.splitDataUrl(it)
                }

                val res = api.editFlatLay(
                    EditFlatLayRequest(
                        baseImageBase64 = baseSplit?.second,
                        baseMimeType = baseSplit?.first ?: "image/jpeg",
                        sourceGarmentBase64 = anchorSplit?.second,
                        sourceGarmentMimeType = anchorSplit?.first ?: "image/jpeg",
                        editPrompt = trimmed,
                        aspectRatio = aspectRatio,
                        imageSize = imageSize
                    )
                )
                val body = res.body()
                if (!res.isSuccessful || body?.imageDataUrl == null) {
                    throw IllegalStateException(
                        body?.error ?: extractServerError(res.errorBody()?.string()) ?: "Generation failed."
                    )
                }
                val imgUrl = body.imageDataUrl
                _uiState.update {
                    it.copy(
                        isCanvasGenerating = false,
                        serverOnline = true,
                        canvasGeneratedUrl = imgUrl,
                        canvasHistory = it.canvasHistory + imgUrl,
                        canvasPrompts = it.canvasPrompts + trimmed
                    )
                }
            } catch (err: Exception) {
                _uiState.update {
                    it.copy(
                        isCanvasGenerating = false,
                        canvasError = err.message ?: "Unable to generate flat-lay."
                    )
                }
            }
        }
    }

    fun selectCanvasVersion(url: String) {
        _uiState.update { it.copy(canvasGeneratedUrl = url) }
    }

    // Save & Wardrobe Sync
    fun saveLookToCloud(look: GeneratedLookState) {
        val state = _uiState.value
        val user = state.user
        val analysis = state.analysis ?: return
        val sourceUrl = state.sourceImageDataUrl ?: return

        if (user == null) {
            _uiState.update { it.copy(activeTab = AtelierTab.WARDROBE) }
            showToast("Sign in on the Closet tab to save outfits to your Cloud Closet.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(savingLookId = look.id) }
            runCatching {
                val (garmentId, lookId) = repository.saveGarmentAndLook(
                    user = user,
                    garmentIdInput = state.activeGarmentId,
                    analysis = analysis,
                    brief = state.brief,
                    sourceImageDataUrl = sourceUrl,
                    look = look
                )
                _uiState.update { cur ->
                    cur.copy(
                        savingLookId = null,
                        activeGarmentId = garmentId,
                        looks = cur.looks.map { l ->
                            if (l.id == look.id) l.copy(savedLookId = lookId) else l
                        }
                    )
                }
                showToast("Saved \"${look.title}\" to Cloud Closet.")
            }.onFailure { err ->
                _uiState.update { it.copy(savingLookId = null) }
                showToast(err.message ?: "Failed to save look.")
            }
        }
    }

    fun saveCanvasLookToCloud() {
        val state = _uiState.value
        val user = state.user
        val imgUrl = state.canvasGeneratedUrl ?: return
        if (user == null) {
            _uiState.update { it.copy(activeTab = AtelierTab.WARDROBE) }
            showToast("Sign in on the Closet tab to save custom flat-lays.")
            return
        }

        val promptDesc = state.canvasPrompts.joinToString(" -> ").ifBlank { "Custom flat-lay remix" }
        val customAnalysis = GarmentAnalysis(
            garmentTitle = state.canvasTitle.ifBlank { "Custom Fit Remix" },
            category = "Custom Flat-Lay",
            fabricNotes = "Prompt-composed flat-lay remix.",
            styleSummary = promptDesc,
            palette = listOf(PaletteSwatch(hex = "#111111", name = "Obsidian Ink", role = "Primary"))
        )
        val customLook = GeneratedLookState(
            id = "custom_${System.currentTimeMillis()}",
            occasion = "Custom",
            title = customAnalysis.garmentTitle,
            stylingRationale = promptDesc,
            flatLayDirection = promptDesc,
            pieces = listOf(
                OutfitPiece("Composition", customAnalysis.garmentTitle, "Curated", "Flat-Lay Editor")
            ),
            imageDataUrl = imgUrl,
            imageHistory = listOf(imgUrl),
            editPromptHistory = state.canvasPrompts
        )

        viewModelScope.launch {
            runCatching {
                repository.saveGarmentAndLook(
                    user = user,
                    garmentIdInput = null,
                    analysis = customAnalysis,
                    brief = state.brief,
                    sourceImageDataUrl = state.canvasReferenceDataUrl ?: imgUrl,
                    look = customLook
                )
                showToast("Saved \"${customAnalysis.garmentTitle}\" to Cloud Closet.")
            }.onFailure { err ->
                showToast(err.message ?: "Failed to save custom look.")
            }
        }
    }

    fun signInWithGoogleOAuth(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, authError = null) }
            runCatching {
                repository.signInWithGoogleOAuth(activity)
                _uiState.update { it.copy(isSigningIn = false, authError = null) }
                showToast("Signed in to Atelier Cloud Closet.")
            }.onFailure { err ->
                val msg = err.message ?: "Google Sign-In failed."
                _uiState.update { it.copy(isSigningIn = false, authError = msg) }
                showToast(msg)
            }
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, authError = null) }
            runCatching {
                repository.signInWithGoogleIdToken(idToken)
                _uiState.update { it.copy(isSigningIn = false, authError = null) }
                showToast("Signed in to Atelier Cloud Closet.")
            }.onFailure { err ->
                val msg = err.message ?: "Google Sign-In failed."
                _uiState.update { it.copy(isSigningIn = false, authError = msg) }
                showToast(msg)
            }
        }
    }

    fun signOut() {
        repository.signOut()
        showToast("Signed out.")
    }

    fun selectWardrobeGarment(garmentId: String) {
        _uiState.update { it.copy(selectedWardrobeGarmentId = garmentId) }
        val uid = _uiState.value.user?.uid ?: return
        observeSelectedGarmentLooks(uid, garmentId)
    }

    fun setWardrobeSeasonFilter(filter: String) {
        _uiState.update { it.copy(wardrobeSeasonFilter = filter) }
    }

    fun deleteWardrobeGarment(garmentId: String) {
        val uid = _uiState.value.user?.uid ?: return
        viewModelScope.launch {
            runCatching { repository.deleteGarment(uid, garmentId) }
                .onFailure { err -> showToast(err.message ?: "Unable to delete garment.") }
        }
    }

    fun deleteWardrobeLook(garmentId: String, lookId: String) {
        val uid = _uiState.value.user?.uid ?: return
        viewModelScope.launch {
            runCatching { repository.deleteLook(uid, garmentId, lookId) }
                .onFailure { err -> showToast(err.message ?: "Unable to delete look.") }
        }
    }

    fun reopenGarmentInStudio(garment: SavedGarmentRecord, savedLooks: List<SavedLookRecord>) {
        _uiState.update {
            it.copy(
                activeTab = AtelierTab.STUDIO,
                studioShowResults = true,
                sourceImageDataUrl = garment.sourceImageDataUrl,
                activeGarmentLabel = garment.title,
                activeGarmentId = garment.id,
                brief = StyleBrief(garment.season, garment.silhouette, garment.budgetTier),
                analysis = GarmentAnalysis(
                    garmentTitle = garment.title,
                    category = garment.category,
                    fabricNotes = garment.fabricNotes,
                    styleSummary = garment.styleSummary,
                    palette = garment.palette,
                    styleInsight = garment.styleInsight,
                    quickTips = garment.quickTips
                ),
                looks = savedLooks.map { sl ->
                    GeneratedLookState(
                        id = sl.id,
                        savedLookId = sl.id,
                        occasion = sl.occasion,
                        title = sl.title,
                        stylingRationale = sl.stylingRationale,
                        flatLayDirection = sl.stylingRationale,
                        pieces = sl.pieces,
                        imageDataUrl = sl.flatLayImageDataUrl,
                        imageHistory = listOfNotNull(sl.flatLayImageDataUrl),
                        editPromptHistory = sl.editPromptHistory
                    )
                }
            )
        }
    }

    fun saveImageToGallery(dataUrl: String, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = ImageCodec.decodeDataUrlToBitmap(dataUrl)
            if (bitmap == null) {
                withContext(Dispatchers.Main) { showToast("Unable to decode image for export.") }
                return@launch
            }
            runCatching {
                val resolver = getApplication<Application>().contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AtelierStylist")
                    }
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Could not create MediaStore entry.")
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                withContext(Dispatchers.Main) { showToast("Saved JPEG to Pictures/AtelierStylist.") }
            }.onFailure { err ->
                withContext(Dispatchers.Main) { showToast(err.message ?: "Export failed.") }
            }
        }
    }

    private fun extractServerError(rawJson: String?): String? {
        if (rawJson.isNullOrBlank()) return null
        return runCatching {
            val map = gson.fromJson(rawJson, Map::class.java)
            map["error"]?.toString()
        }.getOrNull()
    }
}
