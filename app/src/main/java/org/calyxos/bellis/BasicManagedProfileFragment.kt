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

import android.app.Dialog
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.view.View
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BasicManagedProfileFragment : Fragment(R.layout.basic_managed_profile_fragment) {

    private val userSettings = "android.settings.USER_SETTINGS"

    class RemoveProfileDialogFragment : DialogFragment() {

        companion object {
            fun show(parentFragment: Fragment) {
                return RemoveProfileDialogFragment().show(
                    parentFragment.childFragmentManager,
                    RemoveProfileDialogFragment::class.java.simpleName
                )
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.remove_profile_confirmation))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    context?.getSystemService(DevicePolicyManager::class.java)?.wipeData(0)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.remove_profile)?.setOnClickListener {
            RemoveProfileDialogFragment.show(this)
        }
        view.findViewById<View>(R.id.app_and_content_access)?.setOnClickListener {
            val intent = Intent(userSettings).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                putExtra(Intent.EXTRA_USER, Process.myUserHandle())
            }
            it.context.startActivity(intent)
        }
    }
}
