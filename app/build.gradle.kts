plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.aether.agent"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.aether.agent"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        versionName = "0.1.$versionCode"
    }
    signingConfigs { create("release") {
        System.getenv("KEYSTORE_PATH")?.let { storeFile = file(it) }
        storePassword = System.getenv("KEYSTORE_PASSWORD"); keyAlias = System.getenv("KEY_ALIAS"); keyPassword = System.getenv("KEY_PASSWORD")
    } }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui); implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose); implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose); implementation(libs.androidx.lifecycle.runtime.compose); implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android); implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.android); implementation(libs.ktor.client.content.negotiation); implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.androidx.datastore.preferences)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.core:core-ktx:1.13.1"); implementation(libs.androidx.room.runtime); implementation(libs.androidx.room.ktx)
    ksp("androidx.room:room-compiler:2.6.1")
    implementation(libs.markdown.renderer)

    // Android binding for SLF4J 1.7, required by LoggerFactory's StaticLoggerBinder lookup.
    implementation("org.slf4j:slf4j-android:1.7.36")
}
