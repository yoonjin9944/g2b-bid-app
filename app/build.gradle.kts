import java.util.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
    alias(libs.plugins.google.services)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

val kakaoNativeKey = localProps.getProperty("KAKAO_NATIVE_APP_KEY") ?: ""

android {
    namespace = "com.g2b.bidapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.g2b.bidapp"
        minSdk = 33
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.4"

        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "com.g2b.bidapp.HiltTestRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${localProps.getProperty("GOOGLE_CLIENT_ID", "")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
        buildConfigField("String", "GOGOV_API_KEY", "\"${localProps.getProperty("GOGOV_API_KEY", "")}\"")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"${localProps.getProperty("KAKAO_NATIVE_APP_KEY", "")}\"")
        buildConfigField("String", "KAKAO_ADMIN_KEY", "\"${localProps.getProperty("KAKAO_ADMIN_KEY", "")}\"")
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
}

dependencies {
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.hilt.common)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
    implementation(libs.kakao.all)

    // ── AndroidX Core ────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.splash.screen)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.messaging)
    implementation(libs.coroutines.play.services)

    // ── Compose BOM ──────────────────────────────────────────────────────────
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ── Navigation ───────────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Lifecycle / ViewModel ─────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ── Hilt ─────────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── Room ─────────────────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.room.paging)

    // ── Network (Retrofit + OkHttp + Gson) ───────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // ── Supabase ─────────────────────────────────────────────────────────────
    val supabaseBom = platform(libs.supabase.bom)
    implementation(supabaseBom)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.functions)
    implementation(libs.supabase.compose.auth)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)

    // ── Paging 3 ─────────────────────────────────────────────────────────────
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // ── Coroutines ───────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Kotlin Serialization ─────────────────────────────────────────────────
    implementation(libs.kotlinx.serialization.json)

    // ── Firebase (FCM) ───────────────────────────────────────────────────────
//    val firebaseBom = platform(libs.firebase.bom)
//    implementation(firebaseBom)
//    implementation(libs.firebase.messaging)
//    implementation(libs.firebase.analytics)

    // ── WorkManager + Hilt Work ───────────────────────────────────────────────
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // ── DataStore ─────────────────────────────────────────────────────────────
    implementation(libs.datastore.preferences)

    // ── Credential Manager (Google Sign-In) ──────────────────────────────────
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // ── Security ─────────────────────────────────────────────────────────────
    implementation(libs.security.crypto)

    // ── Unit Test ────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.paging.testing)

    // ── Instrumented Test ─────────────────────────────────────────────────────
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    kspAndroidTest(libs.hilt.android.compiler)
}