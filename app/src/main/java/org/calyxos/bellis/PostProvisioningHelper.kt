/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.atomic.AtomicBoolean

object PostProvisioningHelper {

    private val TAG = PostProvisioningHelper::class.java.simpleName
    private const val PREFS = "post-provisioning"
    private const val PREF_DONE = "done"
    private var postProvisioningInProgress = AtomicBoolean()

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
        val isInProgress = postProvisioningInProgress.getAndSet(true)
        if (isInProgress) {
            return
        }
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

            val sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean(PREF_DONE, true).apply()

            devicePolicyManager.setProfileEnabled(componentName)

            launchSUW(context)
        }
        postProvisioningInProgress.set(false)
    }

    private fun provisioningComplete(context: Context): Boolean {
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
        context.startActivity(intent)
    }
}
