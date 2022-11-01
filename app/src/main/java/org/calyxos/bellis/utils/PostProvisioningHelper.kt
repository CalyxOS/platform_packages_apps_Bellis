/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis.utils

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import org.calyxos.bellis.BasicDeviceAdminReceiver
import org.calyxos.bellis.R

object PostProvisioningHelper {

    private val TAG = PostProvisioningHelper::class.java.simpleName

    private const val GARLIC_LEVEL = "garlic_level"
    private const val PREFS = "post-provisioning"
    private const val PREF_DONE = "done"

    private const val ORBOT_PKG = "org.torproject.android"
    private const val ORBOT_ACTION_START = "org.torproject.android.intent.action.START"

    private enum class GarlicLevel {
        STANDARD, SAFER, SAFEST
    }

    fun completeProvisioning(context: Context) {
        if (!provisioningComplete(context)) {
            val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
            val componentName = BasicDeviceAdminReceiver.getComponentName(context)
            devicePolicyManager.apply {
                setProfileName(componentName, context.getString(R.string.app_name))

                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<SystemAppWorker>().setExpedited(
                        OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
                    ).build()
                )

                // Do required setup for Garlic Level
                if (context is Activity) {
                    when (getGarlicLevel(context.intent)) {
                        GarlicLevel.SAFER -> setupSafer(context)

                        else -> Log.i(TAG, "Garlic Level: Standard, nothing to do!")
                    }
                }
            }

            val sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean(PREF_DONE, true).apply()

            devicePolicyManager.setProfileEnabled(componentName)

            launchSUW(context)
        }
    }

    private fun provisioningComplete(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(PREF_DONE, false)
    }

    private fun launchSUW(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                val setupWizard = "org.lineageos.setupwizard"
                val setupWizardActivity = ".SetupWizardActivity"
                setClassName(setupWizard, setupWizard + setupWizardActivity)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            context.startActivity(intent)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to start setup wizard", exception)
        }
    }

    private fun getGarlicLevel(intent: Intent): GarlicLevel {
        val garlicLevelInt = intent.getParcelableExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
            PersistableBundle::class.java
        )?.getInt(GARLIC_LEVEL, GarlicLevel.STANDARD.ordinal)
            ?: GarlicLevel.STANDARD.ordinal

        return GarlicLevel.values()[garlicLevelInt]
    }

    private fun setupOrbot(context: Context) {
        try {
            val componentName = BasicDeviceAdminReceiver.getComponentName(context)

            // Set Orbot as always-on-vpn and start it
            context.getSystemService(DevicePolicyManager::class.java).apply {
                setAlwaysOnVpnPackage(componentName, ORBOT_PKG, true)
                setPermissionGrantState(
                    componentName,
                    ORBOT_PKG,
                    POST_NOTIFICATIONS,
                    PERMISSION_GRANT_STATE_GRANTED
                )
            }
            context.sendBroadcast(Intent(ORBOT_ACTION_START).setPackage(ORBOT_PKG))
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to set always-on VPN", exception)
        }
    }

    private fun setupSafer(context: Context) {
        // Do required setup for orbot
        setupOrbot(context)
    }
}
