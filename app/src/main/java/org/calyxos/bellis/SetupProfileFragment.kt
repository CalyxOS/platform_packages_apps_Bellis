/*
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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment

class SetupProfileFragment : Fragment(R.layout.setup_profile_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.set_up_profile)
            .setOnClickListener { provisionManagedProfile(view.context) }
    }

    private fun provisionManagedProfile(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            val component = BasicDeviceAdminReceiver.getComponentName(context)
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, component)
        }
        try {
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.managed_provisioning_not_enabled),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}