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

package com.farmerbb.appnotifier

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import androidx.preference.PreferenceManager

fun Context.initAppNotifierService() {
    val pref = PreferenceManager.getDefaultSharedPreferences(this)
    val intent = Intent(this, AppNotifierService::class.java)

    if((pref.getBoolean("notify_installs", true) || pref.getBoolean("notify_updates", true))
        && (pref.getBoolean("notify_play_store", true) || pref.getBoolean("notify_other_sources", false))) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent)
        else
            startService(intent)
    } else
        stopService(intent)
}

fun Context.createNotificationChannel(channelId: String, isService: Boolean = false) {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val manager = getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
    val name = resources.getIdentifier(channelId, "string", BuildConfig.APPLICATION_ID)
    val priority = if(isService) NotificationManager.IMPORTANCE_MIN else NotificationManager.IMPORTANCE_LOW

    manager.createNotificationChannel(NotificationChannel(channelId, getString(name), priority))
}

fun Context.isPlayStoreInstalled() = try {
    packageManager.getPackageInfo(PLAY_STORE_PACKAGE, 0)
    true
} catch (e: PackageManager.NameNotFoundException) {
    false
}

fun getPlayStoreLaunchIntent(packageName: String) = Intent(Intent.ACTION_VIEW).apply {
    data = "https://play.google.com/store/apps/details?id=$packageName".toUri()
}

const val FOREGROUND_SERVICE_ID = Integer.MAX_VALUE
const val APP_UPDATE_ID = Integer.MAX_VALUE - 1
const val APP_INSTALL_ID = Integer.MAX_VALUE - 2

const val APP_INSTALL_GROUP = "app_install_group"
const val PLAY_STORE_PACKAGE = "com.android.vending"

const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT