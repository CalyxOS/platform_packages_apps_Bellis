/*
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis

import android.app.admin.DeviceAdminService
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.UserManager.DISALLOW_BLUETOOTH_SHARING
import android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES
import androidx.core.content.edit

class BasicDeviceAdminService : DeviceAdminService() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var packageReceiver: BroadcastReceiver
    private var managedProfile: Boolean = false

    override fun onCreate() {
        super.onCreate()

        componentName = BasicDeviceAdminReceiver.getComponentName(this)
        devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        managedProfile = devicePolicyManager.isProfileOwnerApp(componentName.packageName)

        // Register Broadcast Receiver for handling additional packages
        // installed after service was created
        registerReceivers()
    }

    override fun onDestroy() {
        unregisterReceivers()
        super.onDestroy()
    }

    private fun registerReceivers() {
        if (managedProfile) {
            val packageIntentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }

            packageReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.getBooleanExtra(Intent.EXTRA_REPLACING, false) == true) {
                        return
                    }
                    devicePolicyManager.apply {
                        setCrossProfilePackages(componentName, getCrossProfilePackages())
                    }
                }
            }
            registerReceiver(packageReceiver, packageIntentFilter)
        }
    }

    private fun unregisterReceivers() {
        if (managedProfile) {
            unregisterReceiver(packageReceiver)
        }
    }

    private fun getCrossProfilePackages(): Set<String> {
        val installedPackages = packageManager.getInstalledPackages(
            PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        )
        return installedPackages.filter {
            it?.requestedPermissions?.contains(
                android.Manifest.permission.INTERACT_ACROSS_PROFILES
            ) ?: false
        }.map { it.packageName }.toSet()
    }
}
