/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
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
