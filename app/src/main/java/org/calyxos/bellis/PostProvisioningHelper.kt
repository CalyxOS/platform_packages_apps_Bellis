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

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.UserManager
import android.util.Log
import androidx.core.os.bundleOf

object PostProvisioningHelper {
    private const val ORBOT_PKG = "org.torproject.android"
    private const val CHROMIUM_PKG = "org.chromium.chrome"

    private val TAG = PostProvisioningHelper::class.java.simpleName

    private val profileOwnerRestrictions = listOf(
        UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
        UserManager.DISALLOW_BLUETOOTH_SHARING
    )
    private val deviceOwnerRestrictions = listOf(
        UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,
        UserManager.DISALLOW_DEBUGGING_FEATURES
    )
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

    fun completeProvisioning(context: Context) {
        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
        val componentName = BasicDeviceAdminReceiver.getComponentName(context)
        devicePolicyManager.apply {
            if (isProfileOwnerApp(componentName.packageName)) {
                setProfileName(componentName, context.getString(R.string.app_name))
                setProfileEnabled(componentName)
                profileOwnerRestrictions.forEach { clearUserRestriction(componentName, it) }
                systemApps.forEach { enableSystemApp(componentName, it) }

                val setupWizard = "org.lineageos.setupwizard"
                val setupWizardActivity = ".SetupWizardActivity"

                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setClassName(setupWizard, setupWizard + setupWizardActivity)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else if (isDeviceOwnerApp(componentName.packageName)) {
                try {
                    setAlwaysOnVpnPackage(componentName, ORBOT_PKG, true)
                    addUserRestriction(componentName, UserManager.DISALLOW_CONFIG_VPN)
                } catch (exception: PackageManager.NameNotFoundException) {
                    Log.e(TAG, "Failed to set always-on VPN", exception)
                }
                deviceOwnerRestrictions.forEach { addUserRestriction(componentName, it) }

                val bundle = bundleOf("DefaultJavaScriptJitSetting" to 2)
                setApplicationRestrictions(componentName, CHROMIUM_PKG, bundle)
            }
        }
    }
}
