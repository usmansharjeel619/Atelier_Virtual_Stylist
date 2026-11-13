package com.atelier.virtualstylist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atelier.virtualstylist.ui.AtelierTab
import com.atelier.virtualstylist.ui.StylistViewModel
import com.atelier.virtualstylist.ui.components.AtelierIcons
import com.atelier.virtualstylist.ui.screens.FlatLayCanvasScreen
import com.atelier.virtualstylist.ui.screens.StudioScreen
import com.atelier.virtualstylist.ui.screens.WardrobeScreen
import com.atelier.virtualstylist.ui.theme.AtelierSurfaceCard
import com.atelier.virtualstylist.ui.theme.AtelierTheme
import com.atelier.virtualstylist.ui.theme.CardShape
import com.atelier.virtualstylist.ui.theme.GalleryBrass
import com.atelier.virtualstylist.ui.theme.GalleryCard
import com.atelier.virtualstylist.ui.theme.GalleryErrorBg
import com.atelier.virtualstylist.ui.theme.GalleryErrorInk
import com.atelier.virtualstylist.ui.theme.GalleryHairline
import com.atelier.virtualstylist.ui.theme.GalleryInk
import com.atelier.virtualstylist.ui.theme.GalleryMuted
import com.atelier.virtualstylist.ui.theme.GalleryPaper
import com.atelier.virtualstylist.ui.theme.HotCoral
import com.atelier.virtualstylist.ui.theme.NeonLime
import com.atelier.virtualstylist.ui.theme.PillShape

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AtelierTheme {
                val vm: StylistViewModel = viewModel()
                val state by vm.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(state.toastMessage) {
                    val msg = state.toastMessage
                    if (!msg.isNullOrBlank()) {
                        snackbarHostState.showSnackbar(msg)
                        vm.clearToast()
                    }
                }

                Scaffold(
                    containerColor = GalleryPaper,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GalleryPaper)
                                .statusBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(GalleryInk)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "ATELIER",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = NeonLime
                                        )
                                    }
                                    Text(
                                        text = when (state.activeTab) {
                                            AtelierTab.STUDIO -> "Studio"
                                            AtelierTab.EDITOR -> "Editor"
                                            AtelierTab.WARDROBE -> "Closet"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GalleryInk
                                    )
                                }

                                val dotColor = when (state.serverOnline) {
                                    true -> Color(0xFF16A34A)
                                    false -> HotCoral
                                    null -> GalleryMuted
                                }
                                val serverLabel = when {
                                    state.isCheckingServer -> "Checking"
                                    state.serverOnline == true -> "Live"
                                    state.serverOnline == false -> "Setup"
                                    else -> "Server"
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(if (state.serverOnline == false) GalleryErrorBg else GalleryCard)
                                        .border(1.5.dp, if (state.serverOnline == false) HotCoral else GalleryHairline, PillShape)
                                        .clickable { vm.openServerDialog() }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Text(
                                        text = serverLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (state.serverOnline == false) GalleryErrorInk else GalleryInk
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 1.dp, color = GalleryHairline)
                        }
                    },
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GalleryCard)
                                .navigationBarsPadding()
                        ) {
                            HorizontalDivider(thickness = 1.dp, color = GalleryHairline)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(66.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BottomNavPill(
                                    label = "Studio",
                                    icon = AtelierIcons.Style,
                                    selected = state.activeTab == AtelierTab.STUDIO,
                                    activeBg = NeonLime,
                                    activeFg = GalleryInk,
                                    modifier = Modifier.weight(1f),
                                    onClick = { vm.selectTab(AtelierTab.STUDIO) }
                                )
                                BottomNavPill(
                                    label = "Editor",
                                    icon = AtelierIcons.Edit,
                                    selected = state.activeTab == AtelierTab.EDITOR,
                                    activeBg = GalleryBrass,
                                    activeFg = Color.White,
                                    modifier = Modifier.weight(1f),
                                    onClick = { vm.selectTab(AtelierTab.EDITOR) }
                                )
                                val closetLabel = if (state.user != null && state.wardrobeGarments.isNotEmpty()) {
                                    "Closet (${state.wardrobeGarments.size})"
                                } else {
                                    "Closet"
                                }
                                BottomNavPill(
                                    label = closetLabel,
                                    icon = AtelierIcons.Wardrobe,
                                    selected = state.activeTab == AtelierTab.WARDROBE,
                                    activeBg = HotCoral,
                                    activeFg = Color.White,
                                    modifier = Modifier.weight(1f),
                                    onClick = { vm.selectTab(AtelierTab.WARDROBE) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (state.activeTab) {
                            AtelierTab.STUDIO -> StudioScreen(
                                state = state,
                                sampleGarments = vm.sampleGarments,
                                onPickPhoto = vm::onPickGarmentUri,
                                onSelectSample = vm::onSelectSample,
                                onClearPiece = vm::clearPiece,
                                onChangeBrief = vm::updateBrief,
                                onAnalyzeAndStyle = vm::analyzeAndStyle,
                                onSetShowResults = vm::setStudioShowResults,
                                onRefreshInsight = vm::refreshStyleInsight,
                                onRefreshQuickTips = vm::refreshQuickTips,
                                onRetryFlatLay = vm::retryFlatLay,
                                onOpenRefine = vm::openRefineLook,
                                onCloseRefine = vm::closeRefineLook,
                                onOpenInEditor = vm::openLookInEditor,
                                onApplyEdit = vm::applyPromptEdit,
                                onSelectVersion = vm::selectLookVersion,
                                onSaveLook = vm::saveLookToCloud,
                                onExportImage = vm::saveImageToGallery,
                                onOpenServerDialog = vm::openServerDialog
                            )

                            AtelierTab.EDITOR -> FlatLayCanvasScreen(
                                state = state,
                                onUpdateTitle = vm::updateCanvasTitle,
                                onPickReferenceUri = vm::pickCanvasReferenceUri,
                                onClearReference = vm::clearCanvasReference,
                                onGenerate = vm::generateOnCanvas,
                                onSelectVersion = vm::selectCanvasVersion,
                                onExportImage = vm::saveImageToGallery,
                                onSaveToCloud = vm::saveCanvasLookToCloud
                            )

                            AtelierTab.WARDROBE -> WardrobeScreen(
                                state = state,
                                onSignInWithOAuth = { vm.signInWithGoogleOAuth(this@MainActivity) },
                                onSignInWithToken = vm::signInWithGoogleToken,
                                onSignOut = vm::signOut,
                                onSelectGarment = vm::selectWardrobeGarment,
                                onSelectSeasonFilter = vm::setWardrobeSeasonFilter,
                                onDeleteGarment = vm::deleteWardrobeGarment,
                                onDeleteLook = vm::deleteWardrobeLook,
                                onReopenInStudio = vm::reopenGarmentInStudio,
                                onExportImage = vm::saveImageToGallery
                            )
                        }
                    }
                }

                if (state.showServerDialog) {
                    var urlInput by remember(state.serverUrl) { mutableStateOf(state.serverUrl) }
                    Dialog(onDismissRequest = vm::closeServerDialog) {
                        AtelierSurfaceCard(
                            containerColor = GalleryPaper,
                            borderColor = GalleryInk,
                            shape = CardShape
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Server Connection", style = MaterialTheme.typography.headlineSmall)
                                    IconButton(
                                        onClick = vm::closeServerDialog,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(AtelierIcons.Close, contentDescription = "Close")
                                    }
                                }
                                Text(
                                    "Paste your published AI Studio / Share App URL so your phone can reach the AI styling backend.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GalleryMuted
                                )
                                OutlinedTextField(
                                    value = urlInput,
                                    onValueChange = { urlInput = it },
                                    label = { Text("Server URL") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            vm.resetServerUrl()
                                            vm.closeServerDialog()
                                        },
                                        shape = PillShape,
                                        border = BorderStroke(1.5.dp, GalleryHairline),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reset", color = GalleryInk)
                                    }
                                    Button(
                                        onClick = { vm.updateAndCheckServerUrl(urlInput) },
                                        shape = PillShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonLime,
                                            contentColor = GalleryInk
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                        modifier = Modifier.weight(1.4f)
                                    ) {
                                        Text("Save & Connect", fontWeight = FontWeight.Bold)
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

@androidx.compose.runtime.Composable
private fun BottomNavPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeBg: Color,
    activeFg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) activeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeFg else GalleryMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) activeFg else GalleryMuted,
            maxLines = 1
        )
    }
}
