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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class SetupProfileFragment : Fragment(R.layout.setup_profile_fragment) {

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dpm = view.context.getSystemService(DevicePolicyManager::class.java)
        val setupProfileButton = view.findViewById<Button>(R.id.set_up_profile)
        val setupProfileHintText = view.findViewById<TextView>(R.id.set_up_profile_hint)

        if (dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            setupProfileButton.setOnClickListener { provisionManagedProfile(view.context) }
        } else {
            setupProfileButton.isEnabled = false
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
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE)
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
