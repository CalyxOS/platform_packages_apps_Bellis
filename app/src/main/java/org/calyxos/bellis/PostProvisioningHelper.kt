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
import android.content.pm.PackageManager
import android.os.UserManager

object PostProvisioningHelper {

    private const val PREFS = "post-provisioning"
    private const val PREF_DONE = "done"

    private val userRestrictions = listOf(
        UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
        UserManager.DISALLOW_BLUETOOTH_SHARING
    )

    private val requiredPackages = listOf(
        "com.stevesoltys.seedvault",
        "org.fdroid.fdroid",
        "org.chromium.chrome"
    )
    private val gmsPackages = listOf(
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending",
        "org.fitchfamily.android.dejavu",
        "org.microg.nlp.backend.ichnaea",
        "org.microg.nlp.backend.nominatim"
    )

    fun completeProvisioning(context: Context) {
        if (!provisioningComplete(context)) {
            val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
            val componentName = BasicDeviceAdminReceiver.getComponentName(context)
            devicePolicyManager.apply {
                setProfileName(componentName, context.getString(R.string.app_name))
                setProfileEnabled(componentName)

                // Clear user restrictions
                userRestrictions.forEach {
                    devicePolicyManager.clearUserRestriction(componentName, it)
                }

                setBackupServiceEnabled(componentName, true)
            }
        }
    }

    fun enableRequiredPackages(context: Context, enableGMS: Boolean = false) {
        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
        val componentName = BasicDeviceAdminReceiver.getComponentName(context)

        // Enable GMS packages separately to respect setup wizard
        if (enableGMS) {
            val packageManager = context.packageManager
            gmsPackages.forEach { packageName ->
                try {
                    // Store the package status and reset it as setup wizard explicit disables it
                    val packageStatus = packageManager.getApplicationEnabledSetting(packageName)
                    packageManager.setApplicationEnabledSetting(
                        packageName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        0
                    )
                    if (packageStatus == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                        devicePolicyManager.enableSystemApp(componentName, packageName)
                    }
                } catch (exception: Exception) {
                }
            }
        } else {
            requiredPackages.forEach { packageName ->
                try {
                    devicePolicyManager.enableSystemApp(componentName, packageName)
                } catch (exception: Exception) {}
            }
        }
    }

    private fun provisioningComplete(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(PREF_DONE, false)
    }
}
