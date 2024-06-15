/*
 * SPDX-FileCopyrightText: 2019 Google LLC
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("com.android.application") version "8.4.2" apply false
    // https://android.googlesource.com/platform/external/kotlinc/+/refs/tags/android-14.0.0_r29/build.txt
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1" apply false
}
