/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis.utils

import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import org.calyxos.bellis.BasicDeviceAdminReceiver
import org.calyxos.bellis.R

object PostProvisioningHelper {

    private const val TAG = "PostProvisioningHelper"
    private const val PREFS = "post-provisioning"
    private const val PREF_DONE = "done"

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
            }

            markComplete(context)

            devicePolicyManager.setProfileEnabled(componentName)

            launchSUW(context)
        }
    }

    fun markComplete(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(PREF_DONE, true).apply()
    }

    fun provisioningComplete(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(PREF_DONE, false)
    }

    private fun launchSUW(context: Context) {
        val setupWizard = "org.lineageos.setupwizard"
        val setupWizardActivity = ".SetupWizardActivity"

        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(setupWizard, setupWizard + setupWizardActivity)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // If the activity is not found, it was probably already completed or something.
            // Either way, given that the work SUW can be dismissed by the user anyway as of this
            // writing, this shouldn't be a breaking error.
            Log.w(TAG, "SetupWizard activity not found, skipping", e)
        }
    }
}
