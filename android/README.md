# Atelier Virtual Stylist — Native Android App (Kotlin + Jetpack Compose)

This folder contains the native Android application (`com.atelier.virtualstylist`) built with **Kotlin**, **Jetpack Compose (Material 3)**, **Android Photo Picker**, **Retrofit + OkHttp**, **Coil**, and **Firebase Auth + Cloud Firestore**.

## Features Included
- **Styling Studio (`StudioScreen`)**: Pick any garment photo from your device via Android Photo Picker or test one-click curated sample garments, set your Season / Silhouette / Budget brief, and run multimodal palette & cut analysis.
- **Garment Dossier (`DossierSection`)**: Visual **Color Palette** grid with tonal proportion bar, HEX copy, **Style Insight** (Proportion & Cut, Material & Drape, How to Wear It, What to Avoid), and **Quick Tips** modal overlay.
- **3-Occasion Flat-Lay Lookbook (`LookTriptychSection`)**: Generates **Casual**, **Business**, and **Night Out** flat-lay outfits, supports iterative **Prompt Flat-Lay Editing** with version history, MediaStore gallery export, and Cloud Wardrobe saving.
- **Flat-Lay Canvas (`FlatLayCanvasScreen`)**: Standalone prompt-driven flat-lay creation and editing using `gemini-nano-banana-2.1`.
- **Cloud Wardrobe (`WardrobeScreen`)**: Google Sign-In via Android Credential Manager, real-time Firestore sync (`ai-studio-0799d294-3624-4e4c-9a51-104a4a07720d`), and a **Season Filter Dropdown** (`All Seasons`, `Spring`, `Summer`, `Autumn`, `Winter`, `Year-Round`).

## Direct APK Installation
- The debug APK is compiled from this project and served at `/api/apk/atelier-virtual-stylist.apk` (also linked as **Download APK** in the top bar of the web app).
- Install `atelier-virtual-stylist.apk` on Android 8.0+ (API 26+) devices or any Android emulator.

## Optional Local Rebuild (Android Studio)
1. Open the `android/` directory in **Android Studio** (Ladybug or newer, JDK 17) and sync Gradle.
2. Firebase project details are automatically read from `../firebase-applet-config.json` into `BuildConfig`. For Google Sign-In on Android with your own signing key, register your package name `com.atelier.virtualstylist` and SHA-1 fingerprint in Firebase project `gen-lang-client-0642761589`.
