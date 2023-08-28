/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis.ui

import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.CrossProfileApps
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import org.calyxos.bellis.R
import org.calyxos.bellis.utils.BasicDeviceAdminReceiver

class SetupProfileFragment : Fragment(R.layout.setup_profile_fragment) {

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dpm = view.context.getSystemService(DevicePolicyManager::class.java)
        val setupProfileButton = view.findViewById<Button>(R.id.set_up_profile)
        val setupProfileHintText = view.findViewById<TextView>(R.id.set_up_profile_hint)

        activity?.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == Intent.ACTION_MANAGED_PROFILE_REMOVED &&
                        parentFragment != null
                    ) {
                        parentFragmentManager.beginTransaction()
                            .detach(this@SetupProfileFragment)
                            .commitAllowingStateLoss()
                        parentFragmentManager.beginTransaction()
                            .attach(this@SetupProfileFragment)
                            .commitAllowingStateLoss()
                    }
                }
            },
            IntentFilter(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        )

        if (dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            setupProfileButton.setOnClickListener { provisionManagedProfile(view.context) }
        } else {
            setupProfileButton.apply {
                val crossProfileApps = view.context.getSystemService(CrossProfileApps::class.java)
                val targetUser = crossProfileApps.targetUserProfiles.first()

                text = crossProfileApps.getProfileSwitchingLabel(targetUser)
                setOnClickListener {
                    crossProfileApps.startMainActivity(
                        ComponentName(
                            view.context.packageName,
                            "${view.context.packageName}.MainActivity"
                        ),
                        targetUser
                    )
                }
            }
            setupProfileHintText.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.workProfileStatus).text =
                getString(R.string.existing_work_profile)
            view.findViewById<TextView>(R.id.workProfileHelp).text =
                Html.fromHtml(
                    getString(R.string.existing_work_profile_help),
                    Html.FROM_HTML_MODE_COMPACT
                )
        }
    }

    private fun provisionManagedProfile(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            val component = BasicDeviceAdminReceiver.getComponentName(context)
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, component)
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE
            )
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS, true)
        }
        try {
            startForResult.launch(intent)
            activity?.finish()
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.managed_provisioning_not_enabled),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
