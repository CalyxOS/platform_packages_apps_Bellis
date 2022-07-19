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

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Bundle

// TODO: Make this a broadcast receiver instead
class SetupActivity : Activity() {

    private val setupWizard = "org.lineageos.setupwizard"
    private val setupWizardActivity = ".SetupWizardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PostProvisioningHelper.completeProvisioning(this)
        navigateBackToFTSW(this)
    }

    private fun navigateBackToFTSW(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(setupWizard, setupWizard + setupWizardActivity)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

}