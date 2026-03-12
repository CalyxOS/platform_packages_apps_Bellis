/*
 * SPDX-FileCopyrightText: 2019 Google LLC
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    jvmToolchain(21)
}

configure<ApplicationExtension> {
    namespace = "org.calyxos.bellis"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
        targetSdk = 36
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
    lint {
        lintConfig = file("lint.xml")
    }
}

dependencies {
    // https://android.googlesource.com/platform/prebuilts/sdk/+/android-16.0.0_r4/current/androidx/m2repository/androidx/appcompat/appcompat
    implementation("androidx.appcompat:appcompat") {
        version { strictly("1.7.0") } // 1.8.0-alpha01 in AOSP but isn't released
    }

    // https://android.googlesource.com/platform/prebuilts/sdk/+/android-16.0.0_r4/current/androidx/m2repository/androidx/work/work-runtime-ktx/
    implementation("androidx.work:work-runtime-ktx") {
        version { strictly("2.11.0-beta01") }
    }

    // https://android.googlesource.com/platform/prebuilts/sdk/+/refs/tags/android-16.0.0_r4/current/extras/material-design-x/Android.bp#7
    implementation("com.google.android.material:material") {
        version { strictly("1.14.0-alpha02") }
    }

    // https://android.googlesource.com/platform/prebuilts/sdk/+/android-16.0.0_r4/current/androidx/m2repository/androidx/navigation/
    // Navigation Components
    val navVersion = "2.10.0-alpha01"
    implementation("androidx.navigation:navigation-fragment-ktx") {
        version { strictly(navVersion) }
    }
    implementation("androidx.navigation:navigation-ui-ktx:") {
        version { strictly(navVersion) }
    }
}
