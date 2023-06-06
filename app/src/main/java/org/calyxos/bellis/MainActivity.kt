/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        if (!appIsProfileOwner()) {
            navigateToSetupFragment()
        }

        when (intent.action) {
            DevicePolicyManager.ACTION_PROVISIONING_SUCCESSFUL -> {
                launchSUW()
                PostProvisioningHelper.completeProvisioning(this)
            }
        }
    }

    private fun appIsProfileOwner(): Boolean {
        val devicePolicyManager = this.getSystemService(DevicePolicyManager::class.java)
        return devicePolicyManager.isProfileOwnerApp(this.packageName)
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

    private fun launchSUW() {
        val setupWizard = "org.lineageos.setupwizard"
        val setupWizardActivity = ".SetupWizardActivity"

        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(setupWizard, setupWizard + setupWizardActivity)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}
