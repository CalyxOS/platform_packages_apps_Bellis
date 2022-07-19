package org.calyxos.bellis;

import android.app.admin.DeviceAdminService;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.UserManager;

public class BasicDeviceAdminService extends DeviceAdminService {
    private static final String PREF_VERSION = "version";

    @Override
    public void onCreate() {
        super.onCreate();

        DevicePolicyManager mDevicePolicyManager = getSystemService(DevicePolicyManager.class);

        long version = 0;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0)
                    .getLongVersionCode();
        } catch (PackageManager.NameNotFoundException ignored) {

        }

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(),
                Context.MODE_PRIVATE);
        if (version > sharedPreferences.getLong(PREF_VERSION, 0)) {
            mDevicePolicyManager.clearUserRestriction(
                    BasicDeviceAdminReceiver.getComponentName(this),
                    UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
            mDevicePolicyManager.clearUserRestriction(
                    BasicDeviceAdminReceiver.getComponentName(this),
                    UserManager.DISALLOW_BLUETOOTH_SHARING);
        }
        sharedPreferences.edit().putLong(PREF_VERSION, version).apply();
    }
}
