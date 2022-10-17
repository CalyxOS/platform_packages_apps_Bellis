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
import android.os.UserManager.DISALLOW_BLUETOOTH_SHARING
import android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES
import android.util.Log
import androidx.core.content.edit

import lineageos.providers.LineageSettings

class BasicDeviceAdminService : DeviceAdminService() {

    private val prefVersion = "version"

    override fun onCreate() {
        super.onCreate()

        val devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        val sharedPreferences = getSharedPreferences(packageName, MODE_PRIVATE)
        val componentName = BasicDeviceAdminReceiver.getComponentName(this)
        val version = packageManager.getPackageInfo(packageName, 0).longVersionCode

        if (sharedPreferences.getLong(prefVersion, 0) < 102) {
            devicePolicyManager.apply {
                clearUserRestriction(componentName, DISALLOW_INSTALL_UNKNOWN_SOURCES)
                clearUserRestriction(componentName, DISALLOW_BLUETOOTH_SHARING)
            }
        }

        sharedPreferences.edit { putLong(prefVersion, version) }

        Log.d("BasicDeviceAdminService", "Cleartext network policy: " +
            LineageSettings.Global.getInt(contentResolver,
                LineageSettings.Global.CLEARTEXT_NETWORK_POLICY, 0))
    }
}
