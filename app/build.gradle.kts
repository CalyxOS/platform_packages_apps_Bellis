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
    compileSdk = 34

    defaultConfig {
        minSdk = 33
        targetSdk = 34
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
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android-14.0.0_r50/current/androidx/m2repository/androidx/appcompat/appcompat/1.7.0-alpha04/Android.bp
    implementation("androidx.appcompat:appcompat") {
        version { strictly("1.7.0-alpha03") } // 1.7.0-alpha04 in AOSP but isn't released
    }

    // https://android.googlesource.com/platform/prebuilts/sdk/+/android-14.0.0_r50/current/androidx/m2repository/androidx/work/work-runtime-ktx/2.10.0-alpha01/Android.bp
    implementation("androidx.work:work-runtime-ktx") {
        version { strictly("2.10.0-alpha01") }
    }

    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-14.0.0_r50/current/extras/material-design-x/Android.bp#15
    implementation("com.google.android.material:material") {
        version { strictly("1.7.0-alpha03") }
    }

    // https://android.googlesource.com/platform/prebuilts/sdk/+/android-14.0.0_r50/current/androidx/m2repository/androidx/navigation/
    // Navigation Components
    val navVersion = "2.8.0-alpha03"
    implementation("androidx.navigation:navigation-fragment-ktx") {
        version { strictly(navVersion) }
    }
    implementation("androidx.navigation:navigation-ui-ktx:") {
        version { strictly(navVersion) }
    }
}
