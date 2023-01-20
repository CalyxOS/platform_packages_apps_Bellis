# Bellis

Bellis is a [Work Profile](https://support.google.com/work/android/answer/6191949?hl=en) manager app,
based upon the [Android BasicManagedProfile Sample](https://github.com/android/enterprise-samples/tree/989baf811a43127ef55e5021f1bcabbe229d148b/BasicManagedProfile).
It leverages the Managed Profile APIs that are part of Android MDM to offer users an easy way to
create and manage work profiles for personal usage.

## Features & Stack

While initially based upon the enterprise sample app, Bellis has been wholly rewritten and adapted to
seamlessly integrate within [CalyxOS](https://calyxos.org), offering a great experience to the users.
Among the various features, the most notable ones are:

- Simple and easy UI/UX (only two fragments for all tasks, i.e., Setup and Management)
- SDK versions based on the AOSP branching strategy.
- Kotlin and Material3.
- Compatible with both AOSP and Gradle build systems.
- Single Activity Architecture.
- Integration with the [SetupWizard](https://gitlab.com/CalyxOS/platform_packages_apps_SetupWizard).
- Enables connecting work and personal apps (requires `INTERACT_ACROSS_PROFILES` permission to be used by apps).
- Handles tasks to be run on app upgrades.

## Screenshots

[<img src="https://gitlab.com/CalyxOS/platform_packages_apps_Bellis/-/raw/tmp/assets/screenshot-01.png" width=250>](https://gitlab.com/CalyxOS/platform_packages_apps_Bellis/-/raw/tmp/assets/screenshot-01.png)
[<img src="https://gitlab.com/CalyxOS/platform_packages_apps_Bellis/-/raw/tmp/assets/screenshot-02.png" width=250>](https://gitlab.com/CalyxOS/platform_packages_apps_Bellis/-/raw/tmp/assets/screenshot-02.png)
[<img src="https://gitlab.com/CalyxOS/platform_packages_apps_Bellis/-/raw/tmp/assets/screenshot-03.png" width=250>](https://gitlab.com/CalyxOS/platform_packages_apps_Bellis/-/raw/tmp/assets/screenshot-03.png)

## Development

As mentioned before, Bellis is compatible with both AOSP and Gradle build systems and seamlessly integrates
with CalyxOS.

To build in AOSP, add the following lines to an included Makefile:

```makefile
# Apps
PRODUCT_PACKAGES += \
    Bellis \
```

To build in Android Studio, clone this repo to get started. Ensure that the testing device targets
a supported API level. The `debug` build type is additionally signed with AOSP signing keys (test keys) to
allow installation over the existing app in the system.

Various patches might be required across the OS to support all the features Bellis offers, depending
upon the use case. Please check [our Gerrit instance](https://review.calyxos.org/) for a complete list of patches.

## Copyright and License

Bellis is licensed and distributed under the [Apache 2.0 License](LICENSE). See files for individual
copyright holder's information.

## Reference Links

These links could help in learning more about various topics mentioned or used in Bellis's development.

- [Work profiles](https://developer.android.com/work/managed-profiles)
- [Connected work & personal apps](https://developers.google.com/android/work/connected-apps)
- [DevicePolicyManager](https://developer.android.com/reference/android/app/admin/DevicePolicyManager)
- [android/enterprise-samples](https://github.com/android/enterprise-samples/)