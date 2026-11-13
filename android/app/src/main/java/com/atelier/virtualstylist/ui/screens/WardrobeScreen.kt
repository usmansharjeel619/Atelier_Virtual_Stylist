package com.atelier.virtualstylist.ui.screens

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.atelier.virtualstylist.BuildConfig
import com.atelier.virtualstylist.data.model.SavedGarmentRecord
import com.atelier.virtualstylist.data.model.SavedLookRecord
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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private val SEASON_OPTIONS = listOf(
    "All" to "All",
    "Spring" to "Spring",
    "Summer" to "Summer",
    "Autumn" to "Autumn",
    "Winter" to "Winter",
    "Year-Round" to "Year-Round"
)

private fun matchesSeason(garmentSeason: String, filter: String): Boolean {
    if (filter == "All") return true
    val lower = garmentSeason.lowercase()
    return when (filter) {
        "Spring" -> lower.contains("spring")
        "Summer" -> lower.contains("summer")
        "Autumn" -> lower.contains("autumn")
        "Winter" -> lower.contains("winter")
        else -> lower.contains("year-round")
    }
}

@Composable
fun WardrobeScreen(
    state: StudioUiState,
    onSignInWithOAuth: () -> Unit,
    onSignInWithToken: (String) -> Unit,
    onSignOut: () -> Unit,
    onSelectGarment: (String) -> Unit,
    onSelectSeasonFilter: (String) -> Unit,
    onDeleteGarment: (String) -> Unit,
    onDeleteLook: (String, String) -> Unit,
    onReopenInStudio: (SavedGarmentRecord, List<SavedLookRecord>) -> Unit,
    onExportImage: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var signInError by remember { mutableStateOf<String?>(null) }
    var viewingDetail by remember { mutableStateOf(false) }

    val user = state.user
    if (user == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GalleryPaper)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AtelierSurfaceCard(
                containerColor = GalleryCard,
                borderColor = GalleryHairline,
                shape = CardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(NeonLime),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AtelierIcons.Wardrobe, contentDescription = null, tint = GalleryInk)
                    }
                    Text("Your Cloud Closet", style = MaterialTheme.typography.displaySmall)
                    Text(
                        "Save outfits and sync them across phone and web.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GalleryMuted
                    )

                    val displayedError = signInError ?: state.authError
                    if (!displayedError.isNullOrBlank()) {
                        Text(
                            text = displayedError,
                            color = GalleryErrorInk,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(InnerShape)
                                .background(GalleryErrorBg)
                                .padding(10.dp)
                        )
                    }

                    Button(
                        onClick = {
                            signInError = null
                            onSignInWithOAuth()
                        },
                        enabled = !state.isSigningIn && state.isFirebaseConfigured,
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = GalleryInk),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (state.isSigningIn) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = GalleryInk)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Signing in...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Continue with Google", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (BuildConfig.WEB_OAUTH_CLIENT_ID.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    signInError = null
                                    runCatching {
                                        val credentialManager = CredentialManager.create(context)
                                        val googleIdOption = GetGoogleIdOption.Builder()
                                            .setFilterByAuthorizedAccounts(false)
                                            .setServerClientId(BuildConfig.WEB_OAUTH_CLIENT_ID)
                                            .build()
                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleIdOption)
                                            .build()
                                        val result = credentialManager.getCredential(context, request)
                                        val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
                                        onSignInWithToken(googleCred.idToken)
                                    }.onFailure {
                                        onSignInWithOAuth()
                                    }
                                }
                            },
                            enabled = !state.isSigningIn,
                            shape = PillShape,
                            border = BorderStroke(1.5.dp, GalleryHairline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Quick Account Picker", color = GalleryInk)
                        }
                    }
                }
            }
        }
        return
    }

    val filteredGarments = state.wardrobeGarments.filter {
        matchesSeason(it.season, state.wardrobeSeasonFilter)
    }
    val activeGarment = filteredGarments.find { it.id == state.selectedWardrobeGarmentId }
        ?: filteredGarments.firstOrNull()

    if (viewingDetail && activeGarment != null) {
        // Clean Drill-Down Detail View for a Selected Garment & its Saved Looks
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(GalleryPaper),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewingDetail = false },
                        shape = PillShape,
                        border = BorderStroke(1.5.dp, GalleryInk),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("← All Pieces", color = GalleryInk)
                    }
                    Button(
                        onClick = { onReopenInStudio(activeGarment, state.wardrobeLooks) },
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = GalleryInk),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Open in Studio", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Text(activeGarment.title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${activeGarment.category} · ${activeGarment.season} · ${state.wardrobeLooks.size} saved fits",
                    style = MaterialTheme.typography.labelMedium,
                    color = GalleryMuted
                )
            }

            items(state.wardrobeLooks, key = { it.id }) { look ->
                AtelierSurfaceCard(
                    containerColor = GalleryCard,
                    borderColor = GalleryHairline,
                    shape = CardShape
                ) {
                    Column {
                        val lookBmp = remember(look.flatLayImageDataUrl) {
                            look.flatLayImageDataUrl?.let { ImageCodec.decodeDataUrlToBitmap(it) }
                        }
                        if (lookBmp != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 3.8f)
                            ) {
                                Image(
                                    bitmap = lookBmp.asImageBitmap(),
                                    contentDescription = look.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!look.flatLayImageDataUrl.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(GalleryCard.copy(alpha = 0.92f))
                                                .clickable {
                                                    onExportImage(
                                                        look.flatLayImageDataUrl,
                                                        look.title.lowercase().replace(Regex("[^a-z0-9]+"), "-")
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(AtelierIcons.Download, contentDescription = "Download", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(GalleryCard.copy(alpha = 0.92f))
                                            .clickable { onDeleteLook(activeGarment.id, look.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(AtelierIcons.Delete, contentDescription = "Delete", tint = HotCoral, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(look.occasion, style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                            Text(look.title, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        return
    }

    // Visual 2-Column Closet Grid
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaper),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cloud Closet", style = MaterialTheme.typography.displaySmall)
                    Text(user.email ?: "Signed in", style = MaterialTheme.typography.labelMedium, color = GalleryMuted)
                }
                OutlinedButton(
                    onClick = onSignOut,
                    shape = PillShape,
                    border = BorderStroke(1.dp, GalleryHairline),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Sign Out", style = MaterialTheme.typography.labelMedium, color = GalleryInk)
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SEASON_OPTIONS) { (value, label) ->
                    val active = state.wardrobeSeasonFilter == value
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (active) GalleryInk else GalleryCard)
                            .border(1.dp, if (active) GalleryInk else GalleryHairline, PillShape)
                            .clickable { onSelectSeasonFilter(value) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) NeonLime else GalleryInk
                        )
                    }
                }
            }
        }

        if (filteredGarments.isEmpty()) {
            item {
                AtelierSurfaceCard(containerColor = GalleryCard, borderColor = GalleryHairline) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (state.wardrobeGarments.isEmpty()) "No saved outfits yet"
                            else "Nothing in ${state.wardrobeSeasonFilter}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Save any outfit from Studio or Editor to see it here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GalleryMuted
                        )
                    }
                }
            }
        } else {
            items(filteredGarments.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { garment ->
                        val thumb = remember(garment.sourceImageDataUrl) {
                            ImageCodec.decodeDataUrlToBitmap(garment.sourceImageDataUrl)
                        }
                        AtelierSurfaceCard(
                            containerColor = GalleryCard,
                            borderColor = GalleryHairline,
                            shape = CardShape,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSelectGarment(garment.id)
                                    viewingDetail = true
                                }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 3.6f)
                                        .background(GalleryStone)
                                ) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb.asImageBitmap(),
                                            contentDescription = garment.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(GalleryCard.copy(alpha = 0.9f))
                                            .clickable { onDeleteGarment(garment.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(AtelierIcons.Delete, contentDescription = "Delete", tint = GalleryInk, modifier = Modifier.size(15.dp))
                                    }
                                }
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        garment.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        garment.season,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GalleryMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
