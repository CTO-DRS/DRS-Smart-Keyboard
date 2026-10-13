import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.agp.library); alias(libs.plugins.kotlin.serialization)
}

// DRS: SDK levels resolve through the explicit property provider API —
// never through `by project` delegation (silent same-name key coupling).
val projectMinSdk = providers.gradleProperty("projectMinSdk").get()
val projectCompileSdk = providers.gradleProperty("projectCompileSdk").get()

kotlin { compilerOptions { jvmTarget.set(JvmTarget.fromTarget("11")); freeCompilerArgs.addAll("-opt-in=kotlin.contracts.ExperimentalContracts") } }

extensions.configure<LibraryExtension> {
    namespace = "org.drs.lib.android"
    compileSdk = projectCompileSdk.toInt()
    defaultConfig { minSdk = projectMinSdk.toInt(); testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"; consumerProguardFiles("consumer-rules.pro") }

    val sharedProguard = getDefaultProguardFile("proguard-android-optimize.txt")
    buildTypes { getByName("release") { isMinifyEnabled = false; proguardFiles(sharedProguard, "proguard-rules.pro") }
    maybeCreate("beta").apply { isMinifyEnabled = false; proguardFiles(sharedProguard, "proguard-rules.pro") }
    }
    compileOptions { val javaLevel = JavaVersion.VERSION_11; sourceCompatibility = javaLevel; targetCompatibility = javaLevel }
    lint { disable.addAll(listOf("UElementAsPsi", "ApplySharedPref", "CommitTransaction", "Recycle", "CommitPrefEdits")) }
}

dependencies { implementation(project(path = ":lib:kotlin")); implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.serialization.json) }
