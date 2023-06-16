/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        if (!appIsProfileOwner()) {
            navigateToFragment(R.id.setupProfileFragment)
        }

        when (intent.action) {
            DevicePolicyManager.ACTION_PROVISIONING_SUCCESSFUL -> {
                navigateToFragment(R.id.pleaseWaitFragment)
                MainScope().launch {
                    PostProvisioningHelper.completeProvisioning(this@MainActivity)
                    launchSUW()
                }
            }
        }
    }

    private fun appIsProfileOwner(): Boolean {
        val devicePolicyManager = this.getSystemService(DevicePolicyManager::class.java)
        return devicePolicyManager.isProfileOwnerApp(this.packageName)
    }

    private fun navigateToFragment(fragmentId: Int) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.navigation_resource, true)
            .build()
        navOptions.shouldLaunchSingleTop()

        navController.navigate(fragmentId, null, navOptions)
    }

    private val startSuwForResult = registerForActivityResult(StartActivityForResult()) {
        // Regardless of the result, we want to navigate out of the Please Wait screen.
        navigateToFragment(R.id.basicManagedProfileFragment)
    }

    private fun launchSUW() {
        val setupWizard = "org.lineageos.setupwizard"
        val setupWizardActivity = ".SetupWizardActivity"

        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(setupWizard, setupWizard + setupWizardActivity)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startSuwForResult.launch(intent)
    }
}
