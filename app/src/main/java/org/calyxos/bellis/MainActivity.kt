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

import android.app.admin.DevicePolicyManager
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            DevicePolicyManager.ACTION_GET_PROVISIONING_MODE -> {
                val provisioningMode = intent.getParcelableExtra(
                    DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                    PersistableBundle::class.java
                )?.getInt(DevicePolicyManager.EXTRA_PROVISIONING_MODE, 0)

                intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, provisioningMode)
                setResult(RESULT_OK, intent)
                finish()
                return
            }
            DevicePolicyManager.ACTION_ADMIN_POLICY_COMPLIANCE -> {
                PostProvisioningHelper.completeProvisioning(this)
                finish()
                return
            }
            DevicePolicyManager.ACTION_PROVISIONING_SUCCESSFUL -> {
                PostProvisioningHelper.completeProvisioning(this)
            }
        }

        setContentView(R.layout.main_activity)

        if (!isProfileOwnerApp()) {
            navigateToSetupFragment()
        }
    }

    private fun isProfileOwnerApp(): Boolean {
        val devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        return devicePolicyManager.isProfileOwnerApp(packageName)
    }

    private fun navigateToSetupFragment() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.navigation_resource, true)
            .build()
        navOptions.shouldLaunchSingleTop()

        navController.navigate(R.id.setupProfileFragment, null, navOptions)
    }
}
