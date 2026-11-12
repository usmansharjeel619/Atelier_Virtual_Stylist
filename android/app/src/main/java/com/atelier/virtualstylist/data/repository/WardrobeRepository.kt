package com.atelier.virtualstylist.data.repository

import android.content.Context
import com.atelier.virtualstylist.BuildConfig
import com.atelier.virtualstylist.data.model.GarmentAnalysis
import com.atelier.virtualstylist.data.model.GeneratedLookState
import com.atelier.virtualstylist.data.model.OutfitPiece
import com.atelier.virtualstylist.data.model.PaletteSwatch
import com.atelier.virtualstylist.data.model.SavedGarmentRecord
import com.atelier.virtualstylist.data.model.SavedLookRecord
import com.atelier.virtualstylist.data.model.StyleBrief
import com.atelier.virtualstylist.data.model.StyleInsight
import com.atelier.virtualstylist.util.ImageCodec
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WardrobeRepository(context: Context) {

    private val appContext = context.applicationContext

    private val firebaseApp: FirebaseApp? = run {
        val existing = FirebaseApp.getApps(appContext).firstOrNull()
        if (existing != null) return@run existing
        val fromResources = runCatching { FirebaseApp.initializeApp(appContext) }.getOrNull()
        if (fromResources != null) return@run fromResources
        if (BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()
        ) {
            runCatching {
                val options = FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .apply {
                        if (BuildConfig.FIREBASE_STORAGE_BUCKET.isNotBlank()) {
                            setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                        }
                        if (BuildConfig.FIREBASE_SENDER_ID.isNotBlank()) {
                            setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                        }
                    }
                    .build()
                FirebaseApp.initializeApp(appContext, options)
            }.getOrNull()
        } else {
            null
        }
    }

    val isFirebaseConfigured: Boolean = firebaseApp != null

    private val auth: FirebaseAuth?
        get() = firebaseApp?.let { FirebaseAuth.getInstance(it) }

    private val db: FirebaseFirestore?
        get() = firebaseApp?.let {
            FirebaseFirestore.getInstance(it, BuildConfig.FIRESTORE_DATABASE_ID)
        }

    fun currentUser(): FirebaseUser? = auth?.currentUser

    fun observeAuthUser(): Flow<FirebaseUser?> = callbackFlow {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { state ->
            trySend(state.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): FirebaseUser {
        val firebaseAuth = auth
            ?: throw IllegalStateException("Add google-services.json to android/app/ to enable Cloud Wardrobe sync.")
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        val user = result.user ?: throw IllegalStateException("Google Sign-In failed.")
        ensureUserProfile(user)
        return user
    }

    fun signOut() {
        auth?.signOut()
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
        return cleaned.ifBlank { "item_${System.currentTimeMillis()}" }
    }

    private fun clamp(value: String?, maxLen: Int, fallback: String): String {
        val trimmed = value?.trim().orEmpty()
        return if (trimmed.isEmpty()) fallback else trimmed.take(maxLen)
    }

    private suspend fun ensureUserProfile(user: FirebaseUser) {
        val firestore = db ?: return
        val uid = sanitizeId(user.uid)
        val ref = firestore.collection("users").document(uid)
        val existing = ref.get().await()
        if (!existing.exists()) {
            ref.set(
                mapOf(
                    "uid" to uid,
                    "displayName" to clamp(user.displayName, 120, "Atelier Member"),
                    "email" to clamp(user.email, 200, "member@atelier.local"),
                    "photoUrl" to clamp(user.photoUrl?.toString(), 500, ""),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            ref.update(
                mapOf(
                    "displayName" to clamp(user.displayName, 120, "Atelier Member"),
                    "photoUrl" to clamp(user.photoUrl?.toString(), 500, ""),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun saveGarmentAndLook(
        user: FirebaseUser,
        garmentIdInput: String?,
        analysis: GarmentAnalysis,
        brief: StyleBrief,
        sourceImageDataUrl: String,
        look: GeneratedLookState
    ): Pair<String, String> {
        val firestore = db
            ?: throw IllegalStateException("Add google-services.json to android/app/ to enable Cloud Wardrobe sync.")

        ensureUserProfile(user)
        val uid = sanitizeId(user.uid)
        val garmentId = sanitizeId(garmentIdInput ?: "garment_${System.currentTimeMillis()}")
        val lookId = sanitizeId(
            look.savedLookId ?: "look_${look.occasion.lowercase().replace(Regex("[^a-z0-9]"), "_")}_${System.currentTimeMillis()}"
        )

        val compressedSource = ImageCodec.compressDataUrlForFirestore(sourceImageDataUrl, 900, 340_000)
        val compressedLook = look.imageDataUrl?.let {
            ImageCodec.compressDataUrlForFirestore(it, 960, 440_000)
        }

        val paletteMaps = analysis.palette.take(6).map { sw ->
            mapOf(
                "hex" to clamp(sw.hex, 20, "#141413"),
                "name" to clamp(sw.name, 60, "Neutral"),
                "role" to clamp(sw.role, 40, "Accent")
            )
        }.ifEmpty {
            listOf(mapOf("hex" to "#141413", "name" to "Espresso Ink", "role" to "Primary"))
        }

        val garmentRef = firestore.collection("users").document(uid).collection("garments").document(garmentId)
        val existingGarment = garmentRef.get().await()

        val insightMap = analysis.styleInsight?.let { ins ->
            mapOf(
                "fitAndProportion" to clamp(ins.fitAndProportion, 600, "Balance proportions cleanly."),
                "materialAndDrape" to clamp(ins.materialAndDrape, 600, "Pair with contrasting textures."),
                "howToWear" to clamp(ins.howToWear, 600, "Anchor at the waist or shoulder."),
                "whatToAvoid" to clamp(ins.whatToAvoid, 600, "Avoid competing busy patterns.")
            )
        }
        val quickTipsList = analysis.quickTips.take(6).map { clamp(it, 200, "Balance proportions.") }

        if (!existingGarment.exists()) {
            val doc = hashMapOf<String, Any>(
                "id" to garmentId,
                "ownerId" to uid,
                "title" to clamp(analysis.garmentTitle, 160, "Curated Garment"),
                "category" to clamp(analysis.category, 80, "Apparel"),
                "fabricNotes" to clamp(analysis.fabricNotes, 600, "Textured finish"),
                "styleSummary" to clamp(analysis.styleSummary, 1200, "Editorial pairing anchor."),
                "season" to brief.season,
                "silhouette" to brief.silhouette,
                "budgetTier" to brief.budgetTier,
                "palette" to paletteMaps,
                "sourceImageDataUrl" to compressedSource,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (insightMap != null) doc["styleInsight"] = insightMap
            if (quickTipsList.isNotEmpty()) doc["quickTips"] = quickTipsList
            garmentRef.set(doc).await()
        } else {
            val updates = hashMapOf<String, Any>(
                "title" to clamp(analysis.garmentTitle, 160, "Curated Garment"),
                "styleSummary" to clamp(analysis.styleSummary, 1200, "Editorial pairing anchor."),
                "season" to brief.season,
                "silhouette" to brief.silhouette,
                "budgetTier" to brief.budgetTier,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (insightMap != null) updates["styleInsight"] = insightMap
            if (quickTipsList.isNotEmpty()) updates["quickTips"] = quickTipsList
            garmentRef.update(updates).await()
        }

        val piecesList = look.pieces.take(10).map { p ->
            mapOf(
                "slot" to clamp(p.slot, 40, "Piece"),
                "name" to clamp(p.name, 120, "Curated Item"),
                "color" to clamp(p.color, 60, "Neutral"),
                "material" to clamp(p.material, 80, "Woven")
            )
        }.ifEmpty {
            listOf(mapOf("slot" to "Anchor Piece", "name" to analysis.garmentTitle, "color" to "Multi", "material" to "Signature"))
        }

        val lookRef = garmentRef.collection("looks").document(lookId)
        val existingLook = lookRef.get().await()
        val lookPayload = hashMapOf<String, Any>(
            "id" to lookId,
            "ownerId" to uid,
            "garmentId" to garmentId,
            "occasion" to look.occasion,
            "title" to clamp(look.title, 160, "${look.occasion} Look"),
            "stylingRationale" to clamp(look.stylingRationale, 1200, "Balanced color and silhouette harmony."),
            "pieces" to piecesList,
            "editPromptHistory" to look.editPromptHistory.take(12).map { clamp(it, 300, "Edit") },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (compressedLook != null) {
            lookPayload["flatLayImageDataUrl"] = compressedLook
        }
        if (!existingLook.exists()) {
            lookPayload["createdAt"] = FieldValue.serverTimestamp()
            lookRef.set(lookPayload).await()
        } else {
            lookRef.update(
                lookPayload.filterKeys {
                    it in setOf("title", "stylingRationale", "pieces", "flatLayImageDataUrl", "editPromptHistory", "updatedAt")
                }
            ).await()
        }

        return garmentId to lookId
    }

    fun observeGarments(uid: String): Flow<List<SavedGarmentRecord>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val safeUid = sanitizeId(uid)
        val registration = firestore.collection("users").document(safeUid)
            .collection("garments")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SavedGarmentRecord::class.java)
                }.orEmpty()
                trySend(records)
            }
        awaitClose { registration.remove() }
    }

    fun observeLooks(uid: String, garmentId: String): Flow<List<SavedLookRecord>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val safeUid = sanitizeId(uid)
        val safeGid = sanitizeId(garmentId)
        val registration = firestore.collection("users").document(safeUid)
            .collection("garments").document(safeGid)
            .collection("looks")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SavedLookRecord::class.java)
                }.orEmpty()
                trySend(records)
            }
        awaitClose { registration.remove() }
    }

    suspend fun deleteGarment(uid: String, garmentId: String) {
        db?.collection("users")?.document(sanitizeId(uid))
            ?.collection("garments")?.document(sanitizeId(garmentId))
            ?.delete()?.await()
    }

    suspend fun deleteLook(uid: String, garmentId: String, lookId: String) {
        db?.collection("users")?.document(sanitizeId(uid))
            ?.collection("garments")?.document(sanitizeId(garmentId))
            ?.collection("looks")?.document(sanitizeId(lookId))
            ?.delete()?.await()
    }
}
