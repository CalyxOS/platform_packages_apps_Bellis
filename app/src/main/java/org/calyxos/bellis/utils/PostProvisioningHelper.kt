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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.os.UserManager.DISALLOW_DEBUGGING_FEATURES
import android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY
import android.util.Log
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import org.calyxos.bellis.BasicDeviceAdminReceiver
import org.calyxos.bellis.R
import java.util.concurrent.TimeUnit

object PostProvisioningHelper {

    private val TAG = PostProvisioningHelper::class.java.simpleName

    private const val PREFS = "post-provisioning"
    private const val PREFS_GARLIC = "garlic_preferences"

    private const val GARLIC_LEVEL = "garlic_level"
    private const val PREF_DONE = "done"

    private const val ORBOT_PKG = "org.torproject.android"
    private const val ORBOT_ACTION_START = "org.torproject.android.intent.action.START"
    private const val CHROMIUM_PKG = "org.chromium.chrome"

    enum class GarlicLevel {
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
                        GarlicLevel.SAFER -> setupSafer(this, componentName, context)

                        GarlicLevel.SAFEST -> setupSafest(this, componentName, context)

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

    /**
     * Gets garlic level stored in the application's sharedpref
     * To read the value from intent, use the overloaded method that takes Intent as param
     */
    fun getGarlicLevel(context: Context): GarlicLevel {
        val sharedPreferences = context.getSharedPreferences(PREFS_GARLIC, Context.MODE_PRIVATE)
        val garlicLevelInt = sharedPreferences.getInt(GARLIC_LEVEL, GarlicLevel.STANDARD.ordinal)
        return GarlicLevel.values()[garlicLevelInt]
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

    private fun saveGarlicLevel(garlicLevel: GarlicLevel, context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_GARLIC, Context.MODE_PRIVATE)
        sharedPreferences.edit { putInt(GARLIC_LEVEL, garlicLevel.ordinal) }
    }

    private fun setupSafer(dpm: DevicePolicyManager, componentName: ComponentName, ctx: Context) {
        try {
            // Set Orbot as always-on-vpn and start it
            dpm.apply {
                setAlwaysOnVpnPackage(componentName, ORBOT_PKG, true)
                setPermissionGrantState(
                    componentName,
                    ORBOT_PKG,
                    POST_NOTIFICATIONS,
                    PERMISSION_GRANT_STATE_GRANTED
                )
            }
            ctx.sendBroadcast(Intent(ORBOT_ACTION_START).setPackage(ORBOT_PKG))
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to set always-on VPN", exception)
        } finally {
            // Save garlic level
            saveGarlicLevel(GarlicLevel.SAFER, ctx)
        }
    }

    private fun setupSafest(dpm: DevicePolicyManager, componentName: ComponentName, ctx: Context) {
        // Apply Safer settings
        setupSafer(dpm, componentName, ctx)

        dpm.apply {
            // Disable USB data signaling if possible
            if (canUsbDataSignalingBeDisabled()) isUsbDataSignalingEnabled = false

            // Disable debugging features and app installation from unknown sources
            addUserRestriction(componentName, DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)
            getParentProfileInstance(componentName)
                .addUserRestriction(componentName, DISALLOW_DEBUGGING_FEATURES)

            // Wipe profile after 3 failed attempts
            setMaximumFailedPasswordsForWipe(componentName, 3)

            // Require entering strong auth after 1 hour has passed
            setRequiredStrongAuthTimeout(componentName, TimeUnit.HOURS.toMillis(1))

            // Disable Javascript JIT in Chromium
            val bundle = bundleOf("DefaultJavaScriptJitSetting" to 2)
            setApplicationRestrictions(componentName, CHROMIUM_PKG, bundle)
        }

        // Save garlic level
        saveGarlicLevel(GarlicLevel.SAFEST, ctx)
    }
}