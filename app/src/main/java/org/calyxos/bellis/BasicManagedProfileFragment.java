/*
 * Copyright (C) 2014 The Android Open Source Project
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

package org.calyxos.bellis;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;

import androidx.fragment.app.Fragment;

/**
 * Provides several functions that are available in a managed profile
 */
public class BasicManagedProfileFragment extends Fragment implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    /**
     * Tag for logging.
     */
    private static final String TAG = "ManagedProfileFragment";

    // This must match Settings.ACTION_USER_SETTINGS
    private static final String USER_SETTINGS = "android.settings.USER_SETTINGS";

    /**
     * {@link Button} to remove this managed profile.
     */
    private Button mButtonRemoveProfile;

    public BasicManagedProfileFragment() {
    }

    public static BasicManagedProfileFragment newInstance() {
        return new BasicManagedProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.basic_managed_profile_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Bind event listeners and initial states
        mButtonRemoveProfile = view.findViewById(R.id.remove_profile);
        mButtonRemoveProfile.setOnClickListener(this);
        view.findViewById(R.id.app_and_content_access).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.app_and_content_access) {
            setupAppAndContentAccess();
        } else if (view.getId() == R.id.remove_profile) {
            mButtonRemoveProfile.setEnabled(false);
            removeProfile();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    /**
     * Manages apps and content in this managed profile.
     */
    private void setupAppAndContentAccess() {
        Intent intent = new Intent(USER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Wipes out all the data related to this managed profile.
     */
    private void removeProfile() {
        Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        DevicePolicyManager manager =
                (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        manager.wipeData(0);
        // The screen turns off here
    }
}
