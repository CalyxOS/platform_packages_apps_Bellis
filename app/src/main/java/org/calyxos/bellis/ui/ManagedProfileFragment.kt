/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis.ui

import android.app.Dialog
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.calyxos.bellis.R
import org.calyxos.bellis.utils.PostProvisioningHelper

class ManagedProfileFragment : Fragment(R.layout.managed_profile_fragment) {

    private val TAG = ManagedProfileFragment::class.java.simpleName
    private val userSettings = "android.settings.USER_SETTINGS"

    companion object {
        fun shouldAllowRemoveProfile(context: Context): Boolean {
            return PostProvisioningHelper.getGarlicLevel(context) !=
                PostProvisioningHelper.GarlicLevel.SAFEST
        }
    }

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
                .setTitle(getString(R.string.remove_profile))
                .setMessage(getString(R.string.remove_profile_confirmation))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (shouldAllowRemoveProfile(requireContext())) {
                        context?.getSystemService(DevicePolicyManager::class.java)?.wipeData(0)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.removeProfile)?.visibility =
            if (shouldAllowRemoveProfile(view.context)) {
                View.VISIBLE
            } else {
                View.GONE
            }

        view.findViewById<Toolbar>(R.id.toolbar)?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.removeProfile -> RemoveProfileDialogFragment.show(this)
                else -> Log.d(TAG, "Unexpected itemId: ${it.itemId}")
            }
            true
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

    override fun onResume() {
        super.onResume()

        view?.apply {
            // Show options for manging profile if provisioning is completed
            if (PostProvisioningHelper.provisioningComplete(this.context)) {
                findViewById<ConstraintLayout>(R.id.managedProfileLayout)?.visibility = View.VISIBLE
                findViewById<ConstraintLayout>(R.id.pleaseWaitLayout)?.visibility = View.GONE
            }
        }
    }
}
