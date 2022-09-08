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

import android.app.Activity
import android.content.Intent
import android.os.Bundle

// TODO: Make this a broadcast receiver instead
class SetupActivity : Activity() {

    private val setupWizard = "org.lineageos.setupwizard"
    private val setupWizardActivity = ".SetupWizardActivity"
    private val requestCodeFTSW = 110

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PostProvisioningHelper.completeProvisioning(this)
        PostProvisioningHelper.enableRequiredPackages(this)
        navigateBackToFTSW()
    }

    private fun navigateBackToFTSW() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(setupWizard, setupWizard + setupWizardActivity)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityForResult(intent, requestCodeFTSW)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeFTSW) {
            PostProvisioningHelper.enableRequiredPackages(this, true)
            finish()
        }
    }
}
