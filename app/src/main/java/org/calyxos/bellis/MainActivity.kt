/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
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
                setResult(RESULT_OK)
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
