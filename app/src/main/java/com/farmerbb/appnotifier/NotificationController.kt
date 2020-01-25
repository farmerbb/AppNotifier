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

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.farmerbb.appnotifier.models.AppUpdateInfo
import com.farmerbb.appnotifier.receivers.InstallNotificationClickedReceiver
import com.farmerbb.appnotifier.receivers.InstallNotificationDismissedReceiver
import com.farmerbb.appnotifier.receivers.UpdateNotificationClickedReceiver
import com.farmerbb.appnotifier.receivers.UpdateNotificationDismissedReceiver
import com.farmerbb.appnotifier.room.AppUpdateDAO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton class NotificationController @Inject constructor(
    private val context: Context,
    private val pref: SharedPreferences,
    private val dao: AppUpdateDAO,
    private val manager: NotificationManager
) {

    fun handleAppUpdateNotification(appInfo: ApplicationInfo) {
        if(!pref.getBoolean("notify_updates", true) || !verifyPrefs(appInfo.packageName)) return

        val info = AppUpdateInfo(
            packageName = appInfo.packageName,
            label = appInfo.loadLabel(context.packageManager).toString(),
            updatedAt = Date(),
            isInstall = false
        )

        GlobalScope.launch {
            val list: List<AppUpdateInfo>

            dao.apply {
                deleteUpdate(info.packageName)
                list = getAllUpdates().plus(info).reversed()
                insert(info)
            }

            buildAppUpdateNotification(list.map { it.label }, lazy { getIcon(appInfo) })
        }
    }

    private fun buildAppUpdateNotification(list: List<String>, icon: Lazy<Bitmap>) {
        if(list.isEmpty()) return

        val header: String
        val content: String

        context.resources.apply {
            header = getQuantityString(R.plurals.apps_updated_header, list.size, list.size)

            val subList = list.subList(0, min(4, list.size)).toMutableList().apply {
                if(list.size >= 5) {
                    val newSize = list.size - 4
                    add(getQuantityString(R.plurals.apps_updated_footer, newSize, newSize))
                }
            }

            val name = "apps_updated_${if(list.size < 5) list.size.toString() else "other"}"
            val id = getIdentifier(name, "string", BuildConfig.APPLICATION_ID)

            content = getString(id, *subList.toTypedArray())
        }

        val channelId = "app_updates"
        context.createNotificationChannel(channelId)

        val contentIntent = Intent(context, UpdateNotificationClickedReceiver::class.java).apply {
            setPackage(BuildConfig.APPLICATION_ID)
        }

        val deleteIntent = Intent(context, UpdateNotificationDismissedReceiver::class.java).apply {
            setPackage(BuildConfig.APPLICATION_ID)
        }

        val pendingContentIntent = PendingIntent.getBroadcast(context, 0, contentIntent, FLAGS)
        val pendingDeleteIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, FLAGS)

        val isEnhanced = pref.getString("notification_text_style", "original") == "enhanced"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_updated)
            .setContentTitle(header)
            .setContentText(content)
            .setContentIntent(pendingContentIntent)
            .setStyle(
                    if(isEnhanced)
                        NotificationCompat.BigTextStyle().bigText(list.joinToString(separator = "\n"))
                    else
                        NotificationCompat.BigTextStyle()
            )
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setAutoCancel(true)
            .setDeleteIntent(pendingDeleteIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if(isEnhanced)
            builder.setLargeIcon(icon.value)

        manager.notify(APP_UPDATE_ID, builder.build())
    }

    fun handleAppInstallNotification(appInfo: ApplicationInfo) {
        if(!pref.getBoolean("notify_installs", true) || !verifyPrefs(appInfo.packageName)) return

        val info = AppUpdateInfo(
                packageName = appInfo.packageName,
                label = appInfo.loadLabel(context.packageManager).toString(),
                updatedAt = Date(),
                isInstall = true
        )

        GlobalScope.launch {
            dao.apply {
                deleteInstall(info.packageName)
                insert(info)
            }

            buildAppInstallNotification(info.packageName, info.label, getIcon(appInfo))
        }
    }

    private fun buildAppInstallNotification(packageName: String, label: String, icon: Bitmap) {
        val channelId = "app_installs"
        context.createNotificationChannel(channelId)

        val code = packageName.hashCode()

        val contentIntent = Intent(context, InstallNotificationClickedReceiver::class.java).apply {
            setPackage(BuildConfig.APPLICATION_ID)
            putExtra(PACKAGE_NAME, packageName)
        }

        val deleteIntent = Intent(context, InstallNotificationDismissedReceiver::class.java).apply {
            setPackage(BuildConfig.APPLICATION_ID)
            putExtra(PACKAGE_NAME, packageName)
        }

        val pendingContentIntent = PendingIntent.getBroadcast(context, code, contentIntent, FLAGS)
        val pendingDeleteIntent = PendingIntent.getBroadcast(context, code, deleteIntent, FLAGS)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_updated)
            .setContentTitle(label)
            .setContentText(context.getString(R.string.successfully_installed))
            .setContentIntent(pendingContentIntent)
            .setStyle(NotificationCompat.BigTextStyle())
            .setLargeIcon(icon)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setAutoCancel(true)
            .setDeleteIntent(pendingDeleteIntent)
            .setGroup(APP_INSTALL_GROUP)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val groupBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_updated)
            .setGroup(APP_INSTALL_GROUP)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_LOW)

        manager.apply {
            notify(APP_INSTALL_ID, groupBuilder.build())
            notify(code, builder.build())
        }
    }

    fun cancelAppInstallNotification(packageName: String) = manager.cancel(packageName.hashCode())

    private fun verifyPrefs(packageName: String): Boolean {
        val fromPlayStore = context.packageManager.getInstallerPackageName(packageName) == PLAY_STORE_PACKAGE
        if(pref.getBoolean("notify_play_store", true) && fromPlayStore) return true
        if(pref.getBoolean("notify_other_sources", false) && !fromPlayStore) return true

        return false
    }

    private fun getIcon(appInfo: ApplicationInfo): Bitmap {
        val icon = context.packageManager.getApplicationIcon(appInfo)
        return if(icon !is BitmapDrawable) {
            val width = max(icon.intrinsicWidth, 1)
            val height = max(icon.intrinsicHeight, 1)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            icon.setBounds(0, 0, canvas.width, canvas.height)
            icon.draw(canvas)

            BitmapDrawable(context.resources, bitmap).bitmap
        } else
            icon.bitmap
    }

    fun replayAppUpdates() {
        if(!pref.getBoolean("notify_updates", true)) return

        GlobalScope.launch {
            val updates = dao.getAllUpdates().reversed()
            context.getApplicationInfoSafely(updates.first().packageName, dao)?.let {
                buildAppUpdateNotification(updates.map { it.label }, lazy { getIcon(it) })
            }
        }
    }

    fun replayAppInstalls() {
        if(!pref.getBoolean("notify_installs", true)) return

        GlobalScope.launch {
            for(install in dao.getAllInstalls()) {
                context.getApplicationInfoSafely(install.packageName, dao)?.let {
                    buildAppInstallNotification(install.packageName, install.label, getIcon(it))
                }
            }
        }
    }
}