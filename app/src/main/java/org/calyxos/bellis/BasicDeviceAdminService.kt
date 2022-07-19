package org.calyxos.bellis

import android.app.admin.DeviceAdminService
import android.app.admin.DevicePolicyManager
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
        val version = packageManager.getPackageInfo(packageName, 0).longVersionCode

        if (sharedPreferences.getLong(prefVersion, 0) < 102) {
            devicePolicyManager.apply {
                clearUserRestriction(componentName, DISALLOW_INSTALL_UNKNOWN_SOURCES)
                clearUserRestriction(componentName, DISALLOW_BLUETOOTH_SHARING)
            }
        }

        sharedPreferences.edit { putLong(prefVersion, version) }
    }

}