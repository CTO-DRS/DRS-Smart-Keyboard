/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins { alias(libs.plugins.agp.library); alias(libs.plugins.kotlin.plugin.compose) }

// DRS: SDK levels resolve through the explicit property provider API —
// never through `by project` delegation (silent same-name key coupling).
val projectMinSdk = providers.gradleProperty("projectMinSdk").get()
val projectCompileSdk = providers.gradleProperty("projectCompileSdk").get()

kotlin { compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("11"))
    // Context parameters power the density-receiver conversion helpers.
    freeCompilerArgs.addAll(
        "-opt-in=kotlin.contracts.ExperimentalContracts", "-Xcontext-parameters",
    )
} }

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    namespace = "org.drs.lib.compose"
    compileSdk = projectCompileSdk.toInt()
    defaultConfig { minSdk = projectMinSdk.toInt();
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"; consumerProguardFiles("consumer-rules.pro") }

    buildFeatures { compose = true }
    val sharedProguard = getDefaultProguardFile("proguard-android-optimize.txt")
    buildTypes { getByName("release") { isMinifyEnabled = false; proguardFiles(sharedProguard, "proguard-rules.pro") }
    maybeCreate("beta").apply { isMinifyEnabled = false; proguardFiles(sharedProguard, "proguard-rules.pro") }
    }
    compileOptions { val javaLevel = JavaVersion.VERSION_11; sourceCompatibility = javaLevel; targetCompatibility = javaLevel }
}

dependencies { listOf(":lib:android", ":lib:color", ":lib:kotlin").forEach { implementation(project(path = it)) }

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material.icons); implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.serialization.json)
    implementation(project(path = ":lib:jetpref:material-ui"))
    debugImplementation(libs.androidx.compose.ui.tooling); testImplementation(libs.kotlin.test.junit5)
}
