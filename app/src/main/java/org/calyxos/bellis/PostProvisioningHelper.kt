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
import androidx.core.content.edit
import androidx.core.os.bundleOf
import java.util.concurrent.TimeUnit

object PostProvisioningHelper {
    private const val ORBOT_PKG = "org.torproject.android"
    private const val ORBOT_ACTION_START = "org.torproject.android.intent.action.START"
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
    private val safestParentProfileOwnerRestrictions = listOf(
        UserManager.DISALLOW_DEBUGGING_FEATURES,
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

    enum class GarlicLevel {
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
                        Log.e(TAG, "Failed to enable $it")
                    }
                }

                if (context is Activity) {
                    val garlicLevel = context.intent.getParcelableExtra(
                        DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                        PersistableBundle::class.java
                    )?.getInt(GARLIC_LEVEL, 0) ?: GarlicLevel.STANDARD.ordinal
                    if (garlicLevel != GarlicLevel.STANDARD.ordinal) {
                        val sharedPreferences = context.getSharedPreferences(context.packageName,
                            Context.MODE_PRIVATE)
                        sharedPreferences.edit { putInt("garlicLevel", garlicLevel) }
                        try {
                            setAlwaysOnVpnPackage(componentName, ORBOT_PKG, true)
                        } catch (exception: PackageManager.NameNotFoundException) {
                            Log.e(TAG, "Failed to set always-on VPN", exception)
                        }

                        context.sendBroadcast(Intent(ORBOT_ACTION_START).setPackage(ORBOT_PKG))

                        if (garlicLevel == GarlicLevel.SAFEST.ordinal) {
                            // Set garlic level restrictions
                            if (canUsbDataSignalingBeDisabled()) {
                                isUsbDataSignalingEnabled = false
                            }
                            safestProfileOwnerRestrictions.forEach {
                                addUserRestriction(componentName, it)
                            }
                            safestParentProfileOwnerRestrictions.forEach {
                                getParentProfileInstance(componentName)
                                    .addUserRestriction(componentName, it)
                            }
                            setMaximumFailedPasswordsForWipe(componentName, 3)
                            setRequiredStrongAuthTimeout(componentName, TimeUnit.HOURS.toMillis(1))
                            // Disable Javascript JIT in Chromium
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
