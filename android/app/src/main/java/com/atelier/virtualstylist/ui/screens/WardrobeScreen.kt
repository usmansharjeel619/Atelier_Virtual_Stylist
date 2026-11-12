package com.atelier.virtualstylist.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.atelier.virtualstylist.BuildConfig
import com.atelier.virtualstylist.data.model.SavedGarmentRecord
import com.atelier.virtualstylist.data.model.SavedLookRecord
import com.atelier.virtualstylist.ui.StudioUiState
import com.atelier.virtualstylist.ui.components.AtelierIcons
import com.atelier.virtualstylist.ui.theme.GalleryBorder
import com.atelier.virtualstylist.ui.theme.GalleryBrass
import com.atelier.virtualstylist.ui.theme.GalleryCard
import com.atelier.virtualstylist.ui.theme.GalleryErrorInk
import com.atelier.virtualstylist.ui.theme.GalleryInk
import com.atelier.virtualstylist.ui.theme.GalleryMuted
import com.atelier.virtualstylist.ui.theme.GalleryPaper
import com.atelier.virtualstylist.ui.theme.GalleryStone
import com.atelier.virtualstylist.util.ImageCodec
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private val SEASON_OPTIONS = listOf(
    "All" to "All Seasons",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeScreen(
    state: StudioUiState,
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
    var filterExpanded by remember { mutableStateOf(false) }

    val user = state.user
    if (user == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GalleryPaper)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CLOUD WARDROBE SYNC", style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Sign In to Access Your Saved Archive",
                style = MaterialTheme.typography.headlineMedium,
                color = GalleryInk
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Connect your Google account to sync analyzed pieces and flat-lay looks across web and Android.",
                style = MaterialTheme.typography.bodyMedium,
                color = GalleryMuted
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (!state.isFirebaseConfigured) {
                Surface(color = GalleryCard, border = BorderStroke(1.dp, GalleryBorder)) {
                    Text(
                        text = "Setup note: Download google-services.json from Firebase project atelier-virtual-stylist into android/app/ to enable native Google Sign-In & Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GalleryMuted,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!signInError.isNullOrBlank()) {
                Text(signInError!!, color = GalleryErrorInk, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
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
                        }.onFailure { err ->
                            signInError = err.message ?: "Google Sign-In failed."
                        }
                    }
                },
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = GalleryInk, contentColor = GalleryPaper)
            ) {
                Text("Sign In with Google")
            }
        }
        return
    }

    val filteredGarments = state.wardrobeGarments.filter {
        matchesSeason(it.season, state.wardrobeSeasonFilter)
    }
    val activeGarment = filteredGarments.find { it.id == state.selectedWardrobeGarmentId }
        ?: filteredGarments.firstOrNull()

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SYNCED ARCHIVE · ${user.email ?: "MEMBER"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = GalleryBrass
                        )
                        Text("Personal Cloud Wardrobe", style = MaterialTheme.typography.headlineMedium)
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, GalleryBorder)
                    ) {
                        Text("Sign Out")
                    }
                }

                // Season Filter Dropdown
                val activeLabel = SEASON_OPTIONS.find { it.first == state.wardrobeSeasonFilter }?.second
                    ?: "All Seasons"
                ExposedDropdownMenuBox(
                    expanded = filterExpanded,
                    onExpandedChange = { filterExpanded = !filterExpanded }
                ) {
                    OutlinedTextField(
                        value = activeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by Season") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        SEASON_OPTIONS.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onSelectSeasonFilter(value)
                                    filterExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = GalleryBorder)
        }

        if (filteredGarments.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (state.wardrobeGarments.isEmpty()) "Your Archive Is Empty"
                        else "No Saved Pieces for ${state.wardrobeSeasonFilter}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (state.wardrobeGarments.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onSelectSeasonFilter("All") },
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, GalleryInk)
                        ) {
                            Text("Show All Seasons")
                        }
                    }
                }
            }
        } else {
            items(filteredGarments, key = { it.id }) { garment ->
                val isSelected = activeGarment?.id == garment.id
                Surface(
                    color = GalleryCard,
                    border = BorderStroke(1.dp, if (isSelected) GalleryInk else GalleryBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clickable { onSelectGarment(garment.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val thumb = remember(garment.sourceImageDataUrl) {
                            ImageCodec.decodeDataUrlToBitmap(garment.sourceImageDataUrl)
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 64.dp)
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
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(garment.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${garment.category} · ${garment.season}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GalleryMuted
                            )
                        }

                        IconButton(onClick = { onDeleteGarment(garment.id) }) {
                            Icon(AtelierIcons.Delete, contentDescription = "Delete garment", tint = GalleryMuted)
                        }
                    }
                }
            }

            if (activeGarment != null) {
                item {
                    Surface(
                        color = GalleryCard,
                        border = BorderStroke(1.dp, GalleryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "${activeGarment.category} · ${activeGarment.season} · ${activeGarment.silhouette}",
                                style = MaterialTheme.typography.labelMedium,
                                color = GalleryMuted
                            )
                            Text(activeGarment.title, style = MaterialTheme.typography.headlineSmall)
                            Text(activeGarment.styleSummary, style = MaterialTheme.typography.bodyMedium, color = GalleryMuted)
                            Button(
                                onClick = { onReopenInStudio(activeGarment, state.wardrobeLooks) },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = GalleryInk, contentColor = GalleryPaper),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open in Studio")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(AtelierIcons.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                items(state.wardrobeLooks, key = { it.id }) { look ->
                    Surface(
                        color = GalleryCard,
                        border = BorderStroke(1.dp, GalleryBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Column {
                            val lookBmp = remember(look.flatLayImageDataUrl) {
                                look.flatLayImageDataUrl?.let { ImageCodec.decodeDataUrlToBitmap(it) }
                            }
                            if (lookBmp != null) {
                                Image(
                                    bitmap = lookBmp.asImageBitmap(),
                                    contentDescription = look.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                )
                            }
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(look.occasion.uppercase(), style = MaterialTheme.typography.labelMedium, color = GalleryBrass)
                                Text(look.title, style = MaterialTheme.typography.headlineSmall)
                                Text(look.stylingRationale, style = MaterialTheme.typography.bodyMedium, color = GalleryMuted)
                                HorizontalDivider(color = GalleryBorder)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (!look.flatLayImageDataUrl.isNullOrBlank()) {
                                        IconButton(
                                            onClick = {
                                                onExportImage(
                                                    look.flatLayImageDataUrl,
                                                    look.title.lowercase().replace(Regex("[^a-z0-9]+"), "-")
                                                )
                                            }
                                        ) {
                                            Icon(AtelierIcons.Download, contentDescription = "Download look")
                                        }
                                    }
                                    IconButton(onClick = { onDeleteLook(activeGarment.id, look.id) }) {
                                        Icon(AtelierIcons.Delete, contentDescription = "Delete look", tint = GalleryMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
