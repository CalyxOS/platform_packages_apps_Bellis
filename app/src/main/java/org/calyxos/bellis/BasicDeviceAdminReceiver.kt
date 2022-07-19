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

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager.DISALLOW_BLUETOOTH_SHARING
import android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES
import android.util.Log

class BasicDeviceAdminReceiver : DeviceAdminReceiver() {

    private val TAG = BasicDeviceAdminReceiver::class.java.simpleName

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, BasicDeviceAdminReceiver::class.java.name)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val componentName = getComponentName(context)
                val version = context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .longVersionCode

                if (version < 200) {
                    getManager(context).apply {
                        clearUserRestriction(componentName, DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        clearUserRestriction(componentName, DISALLOW_BLUETOOTH_SHARING)
                    }
                }

            }
            else -> {
                Log.d(TAG, "Got an unhandled intent!")
            }
        }
    }

}