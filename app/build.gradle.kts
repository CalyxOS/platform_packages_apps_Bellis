/*
 * SPDX-FileCopyrightText: 2019 Google LLC
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "org.calyxos.bellis"
    compileSdk = 33

    defaultConfig {
        minSdk = 33
        targetSdk = 33
    }

    signingConfigs {
        create("aosp") {
            // Generated from the AOSP testkey:
            // https://android.googlesource.com/platform/build/+/refs/tags/android-11.0.0_r29/target/product/security/testkey.pk8
            keyAlias = "testkey"
            keyPassword = "testkey"
            storeFile = file("testkey.jks")
            storePassword = "testkey"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("aosp")
        }
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
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    lint {
        lintConfig = file("lint.xml")
    }
}

dependencies {
    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r1/current/androidx/Android.bp#272
    implementation("androidx.appcompat:appcompat") {
        version { strictly("1.7.0-alpha03") }
    }

    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r1/current/androidx/Android.bp#7029
    implementation("androidx.work:work-runtime-ktx") {
        version { strictly("2.9.0-alpha01") }
    }

    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r1/current/extras/material-design-x/Android.bp#15
    implementation("com.google.android.material:material") {
        version { strictly("1.7.0-alpha03") }
    }

    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r1/current/androidx/Android.bp#4640
    // Navigation Components
    val navVersion = "2.6.0-alpha08"
    implementation("androidx.navigation:navigation-fragment-ktx") {
        version { strictly(navVersion) }
    }
    implementation("androidx.navigation:navigation-ui-ktx:") {
        version { strictly(navVersion) }
    }
}
