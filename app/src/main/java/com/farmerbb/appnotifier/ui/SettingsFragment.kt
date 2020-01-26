/* Copyright 2020 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.appnotifier.ui

import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.farmerbb.appnotifier.*
import java.util.*
import javax.inject.Inject

class SettingsFragment: PreferenceFragmentCompat() {

    @Inject lateinit var pref: SharedPreferences
    @Inject lateinit var manager: NotificationManager
    @Inject lateinit var controller: NotificationController

    init {
        AppNotifierApplication.component.inject(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs)
        setListeners("notify_installs", "notify_updates", "notify_play_store", "notify_other_sources")

        findPreference<Preference>("about_content")?.apply {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Denver")).apply {
                timeInMillis = BuildConfig.TIMESTAMP
            }

            summary = getString(R.string.about_content, calendar.get(Calendar.YEAR))

            setOnPreferenceClickListener {
                val intent = getPlayStoreLaunchIntent(BuildConfig.APPLICATION_ID).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                startActivitySafely(intent)
                true
            }
        }

        findPreference<Preference>("battery_optimization")?.apply {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setOnPreferenceClickListener {
                    startActivitySafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    true
                }
            } else
                preferenceScreen.removePreference(this)
        }

        findPreference<ListPreference>("notification_text_style")?.apply {
            var recreateNotification = false

            val listener = Preference.OnPreferenceChangeListener { _, newValue ->
                val index = findIndexOfValue(newValue.toString())
                summary = if(index >= 0) entries[index] else null

                if(recreateNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    for(notification in manager.activeNotifications) {
                        if(notification.id == APP_UPDATE_ID)
                            controller.replayAppUpdates()
                    }
                }

                true
            }

            onPreferenceChangeListener = listener
            listener.onPreferenceChange(this, pref.getString(key, "original"))

            recreateNotification = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setDivider(ColorDrawable(Color.TRANSPARENT))
        setDividerHeight(0)
    }

    private fun setListeners(vararg keys: String) {
        for(key in keys) {
            findPreference<CheckBoxPreference>(key)?.setOnPreferenceChangeListener { _, _ ->
                Handler().post {
                    requireContext().initAppNotifierService()
                }

                true
            }
        }
    }
}
