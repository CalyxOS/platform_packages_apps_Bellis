/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.UserManager
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

object PostProvisioningHelper {

    private val TAG = PostProvisioningHelper::class.java.simpleName
    private const val PREFS = "post-provisioning"
    private const val PREF_DONE = "done"
    private val userRestrictions = listOf(
        UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
        UserManager.DISALLOW_BLUETOOTH_SHARING
    )

    // Default apps to enable on creation of new managed profile
    private val systemApps = listOf(
        "com.android.gallery3d",
        "com.android.vending",
        "com.android.camera2",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.stevesoltys.seedvault",
        "org.calyxos.eleven",
        "org.chromium.chrome",
        "org.fdroid.fdroid",
        "org.fitchfamily.android.dejavu",
        "org.microg.nlp.backend.ichnaea",
        "org.microg.nlp.backend.nominatim"
    )

    class SystemAppWorker(private val context: Context, workerParams: WorkerParameters) :
        Worker(context, workerParams) {
        override fun doWork(): Result {
            val componentName = BasicDeviceAdminReceiver.getComponentName(context)
            context.getSystemService(DevicePolicyManager::class.java)?.apply {
                systemApps.forEach {
                    try {
                        enableSystemApp(componentName, it)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Failed to enable $it")
                    }
                }
            }
            return Result.success()
        }
    }

    fun completeProvisioning(context: Context) {
        if (!provisioningComplete(context)) {
            val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
            val componentName = BasicDeviceAdminReceiver.getComponentName(context)
            devicePolicyManager.apply {
                setProfileName(componentName, context.getString(R.string.app_name))
                setProfileEnabled(componentName)

                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<SystemAppWorker>().setExpedited(
                        OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
                    ).build()
                )

                // Clear user restrictions
                userRestrictions.forEach {
                    devicePolicyManager.clearUserRestriction(componentName, it)
                }
            }
        }
    }

    private fun provisioningComplete(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(PREF_DONE, false)
    }
}
