/*
 * SPDX-FileCopyrightText: 2007 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis

import android.app.admin.DeviceAdminService
import android.app.admin.DevicePolicyManager
import android.appwidget.AppWidgetManager
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
import org.calyxos.bellis.utils.PostProvisioningHelper

// DO NOT RENAME OR RELOCATE: BREAKS EXISTING PROFILES
class BasicDeviceAdminService : DeviceAdminService() {

    private val TAG = BasicDeviceAdminService::class.java.simpleName
    private val PREFS = "migration"
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var packageReceiver: BroadcastReceiver
    private var managedProfile: Boolean = false
    private val DEFAULT_VERSION: Int = 1 // DO NOT CHANGE
    private val PREF_VERSION: Int = 7

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
                        getCrossProfileWidgetProviders().forEach {
                            addCrossProfileWidgetProvider(componentName, it)
                        }
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
        val sharedPreferences = getSharedPreferences(PREFS, MODE_PRIVATE)

        // Run through all steps for new users
        val oldVersion = sharedPreferences.getInt("pref_version", DEFAULT_VERSION)
        val newVersion = PREF_VERSION

        // If up do date - done.
        if (oldVersion == newVersion) {
            return
        }

        val curVersion = upgradeIfNeeded(oldVersion, newVersion)
        sharedPreferences.edit { putInt("pref_version", curVersion) }
    }

    private fun upgradeIfNeeded(oldVersion: Int, newVersion: Int): Int {
        Log.d(TAG, "Upgrading from version $oldVersion to version $newVersion")
        var currentVersion = oldVersion

        if (currentVersion == DEFAULT_VERSION) {
            if (managedProfile) {
                devicePolicyManager.apply {
                    clearUserRestriction(componentName, DISALLOW_INSTALL_UNKNOWN_SOURCES)
                    clearUserRestriction(componentName, DISALLOW_BLUETOOTH_SHARING)
                }
            }
            currentVersion = 2 // Previously 102
        }
        if (currentVersion == 2) {
            if (managedProfile) {
                devicePolicyManager.apply {
                    setCrossProfileCalendarPackages(componentName, null)
                    setCrossProfilePackages(componentName, getCrossProfilePackages())
                }
            }
            currentVersion = 3 // Previously 103
        }
        if (currentVersion == 3) {
            // This previously set user_setup_complete, but this required a framework change.
            // It is no longer needed, so do nothing here.
            currentVersion = 4 // Previously 104
        }
        if (currentVersion == 4) {
            // Migration is not tied to version code any longer, one time bump to
            // get everyone in sync
            currentVersion = 5
        }
        if (currentVersion == 5) {
            if (managedProfile) {
                devicePolicyManager.apply {
                    getCrossProfileWidgetProviders().forEach {
                        addCrossProfileWidgetProvider(componentName, it)
                    }
                }
            }
            currentVersion = 6
        }
        if (currentVersion == 6) {
            val isExistingInstall = oldVersion != DEFAULT_VERSION
            if (isExistingInstall) {
                // Mark provisioning complete for existing installs. Otherwise, if it is not already
                // marked, it never will be, since there is no retry mechanism and older installs
                // didn't mark it. On the other hand, don't mark it here for new installs,
                // or it will *always* be marked.
                if (!PostProvisioningHelper.provisioningComplete(this)) {
                    PostProvisioningHelper.markComplete(this)
                    Log.w(TAG, "warning: provisioning was not marked complete, but we are "
                        + "upgrading, so we assume it must have been completed and mark it now.")
                }
            }
            currentVersion = 7
        }

        // Add new migrations / defaults above this point.

        if (currentVersion != newVersion) {
            Log.wtf(
                TAG,
                "warning: upgrading to version $newVersion left it at " +
                    "$currentVersion instead; this is probably a bug. Did you update PREF_VERSION?"
            )
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

    private fun getCrossProfileWidgetProviders(): Set<String> {
        return getSystemService(AppWidgetManager::class.java).installedProviders.map {
            it.provider.packageName
        }.toSet()
    }
}
