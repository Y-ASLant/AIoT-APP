plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp)
}

ktlint {
    android.set(true)
    baseline.set(file("ktlint-baseline.xml"))
    outputColorName.set("NONE")
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
    }
}

android {
    namespace = "compose.iot"

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileSdk = 35

    defaultConfig {
        applicationId = "compose.iot"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("VERSION_CODE") as? String)?.toInt() ?: 20260412
        versionName = (project.findProperty("VERSION_NAME") as? String) ?: "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../ASLant")
            storePassword = "ASLant"
            keyAlias = "ASLant"
            keyPassword = "ASLant"
        }
    }

    buildTypes {
        debug {
            splits {
                abi {
                    isEnable = false
                }
            }
        }

        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // 启用R8完全模式优化
            isDebuggable = false
            splits {
                abi {
                    isEnable = true
                    reset()
                    include("armeabi-v7a", "arm64-v8a")
                    isUniversalApk = false
                }
            }
        }
    }

    // Language resource configurations
    androidResources {
        localeFilters += listOf("zh", "en")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion =
            libs.versions.compose.compiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/**"
            excludes += "kotlin/**"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.text.google.fonts)
    lintChecks(libs.androidx.material3.lint)
    implementation(libs.compose.icons.tabler)

    // OkHttp for WebSocket
    implementation(libs.okhttp)

    implementation(libs.androidx.core.splashscreen)

    // Timber logging
    implementation(libs.timber)

    // MQTT Client
    implementation(libs.hivemq.mqtt.client)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
