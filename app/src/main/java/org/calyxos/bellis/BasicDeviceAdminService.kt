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

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private var managedProfile: Boolean = false

    override fun onCreate() {
        super.onCreate()

        componentName = BasicDeviceAdminReceiver.getComponentName(this)
        devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        managedProfile = devicePolicyManager.isProfileOwnerApp(componentName.packageName)

        // Register broadcast receivers for any changes to packages
        registerReceivers()

        // Run all necessary migrations on version upgrade for existing device/profile owners
        onUpgrade()
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

            registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.getBooleanExtra(Intent.EXTRA_REPLACING, false) == true) {
                            return
                        }
                        devicePolicyManager.apply {
                            setCrossProfilePackages(componentName, getCrossProfilePackages())
                        }
                    }
                },
                packageIntentFilter
            )
        }
    }

    private fun onUpgrade() {
        val sharedPreferences = getSharedPreferences(packageName, MODE_PRIVATE)
        val prefVersion = sharedPreferences.getLong("version", 0)
        val currentVersion = packageManager.getPackageInfo(packageName, PackageInfoFlags.of(0))

        when {
            prefVersion < 102 -> {
                if (managedProfile) {
                    devicePolicyManager.apply {
                        clearUserRestriction(componentName, DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        clearUserRestriction(componentName, DISALLOW_BLUETOOTH_SHARING)
                    }
                }
            }
            prefVersion < 103 -> {
                if (managedProfile) {
                    devicePolicyManager.apply {
                        setCrossProfileCalendarPackages(componentName, null)
                        setCrossProfilePackages(componentName, getCrossProfilePackages())
                    }
                }
            }
            prefVersion < 104 -> {
                if (managedProfile) {
                    devicePolicyManager.apply {
                        setSecureSetting(componentName, "user_setup_complete", "1")
                    }
                }
            }
        }

        sharedPreferences.edit { putLong("version", currentVersion.longVersionCode) }
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
