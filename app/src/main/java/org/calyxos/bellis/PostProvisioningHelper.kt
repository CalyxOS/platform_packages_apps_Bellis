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
import android.os.Bundle
import android.os.UserManager
import android.util.Log

object PostProvisioningHelper {

    private const val PREFS = "post-provisioning"
    private const val PREF_DONE = "done"
    private const val ORBOT_PKG = "org.torproject.android"
    private const val CHROMIUM_PKG = "org.chromium.chrome"
    private const val SETUPWIZARD_PKG = "org.lineageos.setupwizard"
    private const val SETUPWIZARD_ACTIVITY = ".SetupWizardActivity"

    private val TAG = PostProvisioningHelper::class.java.simpleName
    private val userRestrictions = listOf(
        UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
        UserManager.DISALLOW_BLUETOOTH_SHARING
    )
    private val requiredPackages = listOf(
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
        if (!provisioningComplete(context)) {
            val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
            val componentName = BasicDeviceAdminReceiver.getComponentName(context)
            devicePolicyManager.apply {
                if (isProfileOwnerApp(componentName.packageName)) {
                    setProfileName(componentName, context.getString(R.string.app_name))
                    setProfileEnabled(componentName)
                    userRestrictions.forEach { clearUserRestriction(componentName, it) }
                    requiredPackages.forEach { enableSystemApp(componentName, it) }
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        setClassName(SETUPWIZARD_PKG, "$SETUPWIZARD_PKG$SETUPWIZARD_ACTIVITY")
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
                    addUserRestriction(componentName, UserManager.DISALLOW_DEBUGGING_FEATURES)
                    addUserRestriction(
                        componentName,
                        UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY
                    )

                    val bundle = Bundle()
                    bundle.putInt("DefaultJavaScriptJitSetting", 2)
                    setApplicationRestrictions(componentName, CHROMIUM_PKG, bundle)
                }
            }
        }
    }

    private fun provisioningComplete(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(PREF_DONE, false)
    }
}
