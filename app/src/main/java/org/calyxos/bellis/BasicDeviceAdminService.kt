/*
 * SPDX-FileCopyrightText: 2007 The Android Open Source Project
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
import android.util.Log
import androidx.core.content.edit

class BasicDeviceAdminService : DeviceAdminService() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var packageReceiver: BroadcastReceiver
    private var managedProfile: Boolean = false
    private val PREF_VERSION: Int = 105

    override fun onCreate() {
        super.onCreate()

        componentName = BasicDeviceAdminReceiver.getComponentName(this)
        devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        managedProfile = devicePolicyManager.isProfileOwnerApp(componentName.packageName)

        // Register Broadcast Receiver for handling additional packages
        // installed after service was created
        registerReceivers()

        // Run required migrations on version upgrade for existing apps
        onUpgrade()
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

    private fun onUpgrade() {
        val sharedPreferences = getSharedPreferences(packageName, MODE_PRIVATE)

        // Run through all steps for new users
        val oldVersion = sharedPreferences.getInt("pref_version", -1)
        val newVersion = PREF_VERSION;

        // If up do date - done.
        if (oldVersion == newVersion) {
            return;
        }

        val curVersion = upgradeIfNeeded(oldVersion, newVersion)
        sharedPreferences.edit { putInt("pref_version", curVersion) }
    }

    private fun upgradeIfNeeded(oldVersion: Int, newVersion: Int): Int {
        var currentVersion = oldVersion

        if (currentVersion == 101) {
            if (managedProfile) {
                devicePolicyManager.apply {
                    clearUserRestriction(componentName, DISALLOW_INSTALL_UNKNOWN_SOURCES)
                    clearUserRestriction(componentName, DISALLOW_BLUETOOTH_SHARING)
                }
            }
            currentVersion = 102
        }
        if (currentVersion == 102) {
            if (managedProfile) {
                devicePolicyManager.apply {
                    setCrossProfileCalendarPackages(componentName, null)
                    setCrossProfilePackages(componentName, getCrossProfilePackages())
                }
            }
            currentVersion = 103
        }
        if (currentVersion == 103) {
            if (managedProfile) {
                devicePolicyManager.apply {
                    setSecureSetting(componentName, "user_setup_complete", "1")
                }
            }
            currentVersion = 104
        }
        if (currentVersion == 104) {
            // Migration is not tied to version code any longer, one time bump to
            // get everyone in sync
            currentVersion = 105
        }

        // Add new migrations / defaults above this point.

        if (currentVersion != newVersion) {
            Log.wtf("Bellis", "warning: upgrading to version " + newVersion + "left it at "
                + currentVersion + "instead; this is probably a bug. Did you update PREF_VERSION?")
        }

        return currentVersion
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
