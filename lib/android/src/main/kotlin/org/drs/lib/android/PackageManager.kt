/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drs.lib.android

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Component class of the DRS settings launcher alias whose icon gets toggled. */
private const val SETTINGS_ACTIVITY_NAME = "com.drs.smartkeyboard.SettingsLauncherAlias"

/** Disables the settings launcher component, hiding the app icon from the launcher. */
fun Context.hideAppIcon(): Unit {
    setLauncherComponentEnabled(targetState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
}

/** Enables the settings launcher component, showing the app icon in the launcher. */
fun Context.showAppIcon(): Unit {
    setLauncherComponentEnabled(targetState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
}

private fun Context.setLauncherComponentEnabled(targetState: Int) {
    packageManager.setComponentEnabledSetting(
        ComponentName(
            this,
            SETTINGS_ACTIVITY_NAME,
        ),
        targetState,
        PackageManager.DONT_KILL_APP,
    )
}
