/*
 * Copyright (C) 2020 The Android Open Source Project
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

package org.calyxos.bellis;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

class PostProvisioningHelper {

    private static final String PREFS = "post-provisioning";
    private static final String PREF_DONE = "done";

    private static final String[] REQUIRED_PACKAGES = new String[]{
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending",
            "org.fitchfamily.android.dejavu",
            "org.microg.nlp.backend.ichnaea",
            "org.microg.nlp.backend.nominatim",
            "com.stevesoltys.seedvault",
            "org.fdroid.fdroid",
            "org.chromium.chrome",
    };

    private final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    private final SharedPreferences mSharedPrefs;

    PostProvisioningHelper(@NonNull Context context) {
        mContext = context;
        mDevicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mSharedPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void completeProvisioning() {
        if (isDone()) {
            return;
        }
        ComponentName componentName = BasicDeviceAdminReceiver.getComponentName(mContext);
        // This is the name for the newly created managed profile.
        mDevicePolicyManager.setProfileName(
                componentName,
                mContext.getString(R.string.app_name)
        );
        // We enable the profile here.
        mDevicePolicyManager.setProfileEnabled(componentName);

        for (String pkg : REQUIRED_PACKAGES) {
            mDevicePolicyManager.enableSystemApp(componentName, pkg);
        }

        mDevicePolicyManager.setBackupServiceEnabled(componentName, true);
    }

    public boolean isDone() {
        return mSharedPrefs.getBoolean(PREF_DONE, false);
    }
}
