/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis.utils

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.calyxos.bellis.BasicDeviceAdminReceiver

class SystemAppWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private val TAG = SystemAppWorker::class.java.simpleName

    // Default apps to enable on creation of new managed profile
    private val systemApps = listOf(
        "com.android.gallery3d",
        "com.android.vending",
        "com.android.camera2",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.stevesoltys.seedvault",
        "org.calyxos.eleven",
        "org.calyxos.glimpse",
        "org.chromium.chrome"
    )

    override fun doWork(): Result {
        val componentName = BasicDeviceAdminReceiver.getComponentName(context)
        context.getSystemService(DevicePolicyManager::class.java)?.apply {
            systemApps.forEach {
                try {
                    enableSystemApp(componentName, it)
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to enable $it", exception)
                }
            }
        }
        return Result.success()
    }
}
