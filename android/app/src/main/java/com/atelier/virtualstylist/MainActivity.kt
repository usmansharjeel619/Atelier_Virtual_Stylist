package com.atelier.virtualstylist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atelier.virtualstylist.ui.AtelierTab
import com.atelier.virtualstylist.ui.StylistViewModel
import com.atelier.virtualstylist.ui.components.AtelierIcons
import com.atelier.virtualstylist.ui.screens.FlatLayCanvasScreen
import com.atelier.virtualstylist.ui.screens.StudioScreen
import com.atelier.virtualstylist.ui.screens.WardrobeScreen
import com.atelier.virtualstylist.ui.theme.AtelierTheme
import com.atelier.virtualstylist.ui.theme.GalleryBorder
import com.atelier.virtualstylist.ui.theme.GalleryCard
import com.atelier.virtualstylist.ui.theme.GalleryInk
import com.atelier.virtualstylist.ui.theme.GalleryPaper

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
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
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Atelier",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = GalleryInk
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = GalleryPaper,
                                titleContentColor = GalleryInk
                            )
                        )
                    },
                    bottomBar = {
                        Column {
                            HorizontalDivider(color = GalleryBorder)
                            NavigationBar(containerColor = GalleryCard) {
                            NavigationBarItem(
                                selected = state.activeTab == AtelierTab.STUDIO,
                                onClick = { vm.selectTab(AtelierTab.STUDIO) },
                                icon = { Icon(AtelierIcons.Style, contentDescription = null) },
                                label = { Text("Studio") }
                            )
                            NavigationBarItem(
                                selected = state.activeTab == AtelierTab.CANVAS,
                                onClick = { vm.selectTab(AtelierTab.CANVAS) },
                                icon = { Icon(AtelierIcons.Canvas, contentDescription = null) },
                                label = { Text("Canvas") }
                            )
                            NavigationBarItem(
                                selected = state.activeTab == AtelierTab.WARDROBE,
                                onClick = { vm.selectTab(AtelierTab.WARDROBE) },
                                icon = { Icon(AtelierIcons.Wardrobe, contentDescription = null) },
                                label = {
                                    Text(
                                        if (state.user != null && state.wardrobeGarments.isNotEmpty()) {
                                            "Wardrobe (${state.wardrobeGarments.size})"
                                        } else {
                                            "Wardrobe"
                                        }
                                    )
                                }
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
                                onRefreshInsight = vm::refreshStyleInsight,
                                onRefreshQuickTips = vm::refreshQuickTips,
                                onRetryFlatLay = vm::retryFlatLay,
                                onOpenRefine = vm::openRefineLook,
                                onCloseRefine = vm::closeRefineLook,
                                onApplyEdit = vm::applyPromptEdit,
                                onSelectVersion = vm::selectLookVersion,
                                onSaveLook = vm::saveLookToCloud,
                                onExportImage = vm::saveImageToGallery
                            )

                            AtelierTab.CANVAS -> FlatLayCanvasScreen(
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
            }
        }
    }
}
