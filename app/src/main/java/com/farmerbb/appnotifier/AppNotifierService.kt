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

import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.farmerbb.appnotifier.room.AppUpdateDAO
import javax.inject.Inject

class AppNotifierService: Service() {

    @Inject lateinit var controller: NotificationController
    @Inject lateinit var dao: AppUpdateDAO
    @Inject lateinit var pref: SharedPreferences

    init {
        AppNotifierApplication.component.inject(this)
    }

    private val packages = mutableMapOf<String, Long>()

    private val packageAddedReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val packageName = intent.dataString?.removePrefix("package:").orEmpty()

            getPackageInfoSafely(packageName, dao)?.let {
                val versionCode = PackageInfoCompat.getLongVersionCode(it)
                if(versionCode <= packages[packageName] ?: 0) return@let

                packages[packageName] = versionCode

                if(intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                    controller.handleAppUpdateNotification(it)
                else
                    controller.handleAppInstallNotification(it)
            }
        }
    }

    private val packageRemovedReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

            val packageName = intent.dataString?.removePrefix("package:").orEmpty()
            packages.remove(packageName)
            controller.cancelAppInstallNotification(packageName)
        }
    }

    override fun onCreate() {
        val packageInfo = packageManager.getInstalledPackages(0)
        for(info in packageInfo) {
            val versionCode = PackageInfoCompat.getLongVersionCode(info)
            packages[info.packageName] = versionCode

            if(info.packageName == BuildConfig.APPLICATION_ID)
                pref.edit().putLong("version_code", versionCode).apply()
        }

        registerReceiver(packageAddedReceiver, IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        })

        registerReceiver(packageRemovedReceiver, IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        })

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channelId = "foreground_service"
        createNotificationChannel(channelId, true)

        val contentIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        }

        val pendingContentIntent = PendingIntent.getActivity(this, 0, contentIntent, FLAGS)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_info_outline_black_24dp)
            .setContentTitle(getString(R.string.listening_for_app_updates_header))
            .setContentText(getString(R.string.listening_for_app_updates_content))
            .setContentIntent(pendingContentIntent)
            .setStyle(NotificationCompat.BigTextStyle())
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_MIN)

        startForeground(FOREGROUND_SERVICE_ID, builder.build())
    }

    override fun onDestroy() {
        unregisterReceiver(packageAddedReceiver)
        unregisterReceiver(packageRemovedReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent) = null
}