import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.agp.library); alias(libs.plugins.kotlin.plugin.compose); alias(libs.plugins.kotlin.serialization)
}

// DRS: SDK levels are resolved through the property provider API explicitly,
// never via `by project` delegation (silent same-name key coupling).
val projectMinSdk = providers.gradleProperty("projectMinSdk").get()
val projectCompileSdk = providers.gradleProperty("projectCompileSdk").get()

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget("11"))
        freeCompilerArgs.addAll("-opt-in=kotlin.contracts.ExperimentalContracts")
    }
}

extensions.configure<LibraryExtension> {
    namespace = "org.drs.lib.color"
    compileSdk = projectCompileSdk.toInt()
    defaultConfig { minSdk = projectMinSdk.toInt() }
    buildFeatures { compose = true }

    val sharedProguard = getDefaultProguardFile("proguard-android-optimize.txt")
    buildTypes {
        getByName("release") { isMinifyEnabled = false; proguardFiles(sharedProguard, "proguard-rules.pro") }
        maybeCreate("beta").apply { isMinifyEnabled = false; proguardFiles(sharedProguard, "proguard-rules.pro") }
    }
    compileOptions {
        val javaLevel = JavaVersion.VERSION_11
        sourceCompatibility = javaLevel; targetCompatibility = javaLevel
    }
}

dependencies {
    val bom = platform(libs.androidx.compose.bom)
    implementation(bom)
    api(libs.material.kolor)
    val material3Module = libs.androidx.compose.material3
    implementation(material3Module)
    val jsonSerializationModule = libs.kotlinx.serialization.json
    implementation(jsonSerializationModule)
    implementation(project(path = ":lib:android"))
    implementation(project(path = ":lib:kotlin"))
}
