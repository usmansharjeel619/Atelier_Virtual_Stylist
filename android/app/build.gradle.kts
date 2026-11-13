import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val firebaseConfigFile = rootProject.file("../firebase-applet-config.json")
@Suppress("UNCHECKED_CAST")
val firebaseCfg: Map<String, Any> = if (firebaseConfigFile.exists()) {
    JsonSlurper().parse(firebaseConfigFile) as Map<String, Any>
} else {
    emptyMap()
}

android {
    namespace = "com.atelier.virtualstylist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.atelier.virtualstylist"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"https://ais-pre-5gup3x3kfq56lbngbfls42-659412078073.europe-west3.run.app\""
        )
        buildConfigField(
            "String",
            "FIRESTORE_DATABASE_ID",
            "\"${firebaseCfg["firestoreDatabaseId"] ?: "ai-studio-0799d294-3624-4e4c-9a51-104a4a07720d"}\""
        )
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${firebaseCfg["projectId"] ?: ""}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${firebaseCfg["appId"] ?: ""}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${firebaseCfg["apiKey"] ?: ""}\"")
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"${firebaseCfg["storageBucket"] ?: ""}\"")
        buildConfigField("String", "FIREBASE_SENDER_ID", "\"${firebaseCfg["messagingSenderId"] ?: ""}\"")
        buildConfigField("String", "WEB_OAUTH_CLIENT_ID", "\"${firebaseCfg["oAuthClientId"] ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
    implementation(libs.gson)
    implementation(libs.coil.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.androidx.browser)
}
