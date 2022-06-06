/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class SetupActivity extends Activity {

    private static final String SETUPWIZARD_PACKAGE = "org.lineageos.setupwizard";
    private static final String SETUPWIZARD_ACTIVITY_CLASS = ".SetupWizardActivity";

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (String pkg : REQUIRED_PACKAGES) {
            getSystemService(DevicePolicyManager.class)
                    .enableSystemApp(BasicDeviceAdminReceiver.getComponentName(this), pkg);
        }
        new PostProvisioningHelper(this).completeProvisioning();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(SETUPWIZARD_PACKAGE,
                SETUPWIZARD_PACKAGE + SETUPWIZARD_ACTIVITY_CLASS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
