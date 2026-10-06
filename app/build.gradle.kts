plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.drs.keyboard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.drs.keyboard"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-rules.pro"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes.add("META-INF/*.kotlin_module")
    }
}

// Zero external runtime dependencies — the whole keyboard is built on the
// Android framework SDK only. This keeps the APK tiny, the build simple and
// guarantees fully-offline behaviour (no analytics, no network code at all).
dependencies {
}
