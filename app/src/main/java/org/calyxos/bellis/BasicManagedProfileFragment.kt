/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 2022 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.bellis

import android.app.Dialog
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BasicManagedProfileFragment : Fragment(R.layout.basic_managed_profile_fragment) {

    private val TAG = BasicManagedProfileFragment::class.java.simpleName
    private val userSettings = "android.settings.USER_SETTINGS"

    inner class RemoveProfileDialogFragment : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.remove_profile))
                .setMessage(getString(R.string.remove_profile_confirmation))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    removeProfile()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }
    }

    fun showRemoveProfileDialogFragment() {
        return RemoveProfileDialogFragment().show(
            this.childFragmentManager,
            RemoveProfileDialogFragment::class.java.simpleName
        )
    }

    fun shouldAllowRemoveProfile(): Boolean {
        val context = requireContext()
        context.getSystemService(DevicePolicyManager::class.java)?.apply {
            val sharedPreferences = context.getSharedPreferences(context.packageName,
                Context.MODE_PRIVATE)
            val garlicLevel = sharedPreferences.getInt("garlicLevel", 0)
            return garlicLevel < PostProvisioningHelper.GarlicLevel.SAFEST.ordinal
        }
        return false
    }

    fun removeProfile() {
        if (shouldAllowRemoveProfile()) {
            context?.getSystemService(DevicePolicyManager::class.java)?.wipeData(0)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.removeProfile)?.visibility =
            if (shouldAllowRemoveProfile()) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }

        view.findViewById<Toolbar>(R.id.toolbar)?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.removeProfile -> showRemoveProfileDialogFragment()
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
}
