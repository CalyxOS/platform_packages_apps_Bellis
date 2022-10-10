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
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.UserManager.DISALLOW_BLUETOOTH_SHARING
import android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES

import androidx.core.content.edit

class BasicDeviceAdminService : DeviceAdminService() {

    private val prefVersion = "version"

    override fun onCreate() {
        super.onCreate()

        val devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        val sharedPreferences = getSharedPreferences(packageName, MODE_PRIVATE)
        val componentName = BasicDeviceAdminReceiver.getComponentName(this)
        val version = packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0)
        ).longVersionCode

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
                devicePolicyManager?.apply {
                    setCrossProfilePackages(componentName, packageManager.getInstalledPackages(
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                        .filter{
                            it?.requestedPermissions?.contains(
                                android.Manifest.permission.INTERACT_ACROSS_PROFILES) ?: false
                        }
                        .map{ it.packageName }.toSet())
                }
            }
        }, packageIntentFilter)

        if (sharedPreferences.getLong(prefVersion, 0) < 102) {
            devicePolicyManager?.apply {
                clearUserRestriction(componentName, DISALLOW_INSTALL_UNKNOWN_SOURCES)
                clearUserRestriction(componentName, DISALLOW_BLUETOOTH_SHARING)
            }
        }

        if (sharedPreferences.getLong(prefVersion, 0) < 103) {
            devicePolicyManager?.apply {
                setCrossProfileCalendarPackages(componentName, null)
                setCrossProfilePackages(componentName, packageManager.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                    .filter{
                        it?.requestedPermissions?.contains(
                            android.Manifest.permission.INTERACT_ACROSS_PROFILES) ?: false
                    }
                    .map{ it.packageName }.toSet())
            }
        }

        sharedPreferences.edit { putLong(prefVersion, version) }
    }
}
