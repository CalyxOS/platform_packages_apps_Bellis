/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2022 The Calyx Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.calyxos.bellis

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PersistableBundle
import android.os.UserManager
import android.util.Log
import androidx.core.os.bundleOf

object PostProvisioningHelper {
    private const val ORBOT_PKG = "org.torproject.android"
    private const val CHROMIUM_PKG = "org.chromium.chrome"

    private const val GARLIC_LEVEL = "garlic_level"

    private val TAG = PostProvisioningHelper::class.java.simpleName

    // Default managed profile restrictions to clear
    private val defaultProfileOwnerRestrictions = listOf(
        UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
        UserManager.DISALLOW_BLUETOOTH_SHARING,
    )

    // Default garlic level restrictions to set
    private val safestProfileOwnerRestrictions = listOf(
        UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,
    )

    // Default apps to enable on creation of new managed profile
    private val systemApps = listOf(
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending",
        "org.fitchfamily.android.dejavu",
        "org.microg.nlp.backend.ichnaea",
        "org.microg.nlp.backend.nominatim",
        "com.stevesoltys.seedvault",
        "org.fdroid.fdroid",
        CHROMIUM_PKG
    )

    private enum class GarlicLevel {
        STANDARD, SAFER, SAFEST
    }

    fun completeProvisioning(context: Context) {
        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
        val componentName = BasicDeviceAdminReceiver.getComponentName(context)
        devicePolicyManager.apply {
            if (isProfileOwnerApp(componentName.packageName)) {
                setProfileName(componentName, context.getString(R.string.app_name))
                setProfileEnabled(componentName)
                defaultProfileOwnerRestrictions.forEach { clearUserRestriction(componentName, it) }
                systemApps.forEach {
                    try {
                        enableSystemApp(componentName, it)
                    } catch (e: IllegalArgumentException) {
                        Log.e("PostProvisioningHelper", "Failed to enable $it")
                    }
                }

                if (context is Activity) {
                    val garlicLevel = context.intent.getParcelableExtra(
                        DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                        PersistableBundle::class.java
                    )?.getInt(GARLIC_LEVEL, 0)
                    if (garlicLevel != GarlicLevel.STANDARD.ordinal) {
                        try {
                            setAlwaysOnVpnPackage(componentName, ORBOT_PKG, true)
                        } catch (exception: PackageManager.NameNotFoundException) {
                            Log.e(TAG, "Failed to set always-on VPN", exception)
                        }

                        if (garlicLevel == GarlicLevel.SAFEST.ordinal) {
                            safestProfileOwnerRestrictions.forEach {
                                addUserRestriction(componentName, it)
                            }
                            val bundle = bundleOf("DefaultJavaScriptJitSetting" to 2)
                            setApplicationRestrictions(componentName, CHROMIUM_PKG, bundle)
                        }
                    }
                }

                try {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        val setupWizard = "org.lineageos.setupwizard"
                        val setupWizardActivity = ".SetupWizardActivity"
                        setClassName(setupWizard, setupWizard + setupWizardActivity)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "Failed to start setup wizard")
                }
            }
        }
    }
}
