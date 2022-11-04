/*
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

    override fun onCreate() {
        super.onCreate()

        val devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        val componentName = BasicDeviceAdminReceiver.getComponentName(this)

        if (devicePolicyManager.isProfileOwnerApp(this.packageName)) {
            // Register Broadcast Receiver for handling additional packages
            // installed after profile was created
            registerBroadcastReceiver(devicePolicyManager, componentName)

            // Run required migrations on version upgrade for existing profiles
            onUpgrade(devicePolicyManager, componentName)
        }
    }

    private fun registerBroadcastReceiver(
        devicePolicyManager: DevicePolicyManager,
        componentName: ComponentName
    ) {
        val packageIntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.getBooleanExtra(Intent.EXTRA_REPLACING, false) == true) {
                    return
                }
                devicePolicyManager.apply {
                    setCrossProfilePackages(componentName, getCrossProfilePackages(packageManager))
                }
            }
        }, packageIntentFilter)
    }

    private fun onUpgrade(devicePolicyManager: DevicePolicyManager, componentName: ComponentName) {
        val sharedPreferences = getSharedPreferences(packageName, MODE_PRIVATE)

        val prefVersion = sharedPreferences.getLong("version", 0)
        val currentVersion = packageManager.getPackageInfo(packageName, PackageInfoFlags.of(0))

        when {
            prefVersion < 102 -> {
                devicePolicyManager.apply {
                    clearUserRestriction(componentName, DISALLOW_INSTALL_UNKNOWN_SOURCES)
                    clearUserRestriction(componentName, DISALLOW_BLUETOOTH_SHARING)
                }
            }
            prefVersion < 103 -> {
                devicePolicyManager.apply {
                    setCrossProfileCalendarPackages(componentName, null)
                    setCrossProfilePackages(componentName, getCrossProfilePackages(packageManager))
                }
            }
        }

        sharedPreferences.edit { putLong("version", currentVersion.longVersionCode) }
    }

    private fun getCrossProfilePackages(packageManager: PackageManager): Set<String> {
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
