/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.os.Bundle
import android.os.PersistableBundle

class SetupActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            DevicePolicyManager.ACTION_GET_PROVISIONING_MODE -> {
                val garlicLevel = intent.getParcelableExtra(
                    DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                    PersistableBundle::class.java
                )?.getInt("garlic_level", GarlicLevel.STANDARD.value)

                val provisioningMode =
                    if (GarlicLevel.getByValue(garlicLevel ?: 0) == GarlicLevel.SAFER) {
                        DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
                    } else {
                        DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
                    }
                intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, provisioningMode)
                setResult(RESULT_OK, intent)
                finish()
            }
            DevicePolicyManager.ACTION_ADMIN_POLICY_COMPLIANCE -> {
                finish()
            }
            DevicePolicyManager.ACTION_PROVISIONING_SUCCESSFUL -> {
                PostProvisioningHelper.completeProvisioning(this)
            }
        }
    }
}
