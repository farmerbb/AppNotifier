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
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.farmerbb.appnotifier.models.AppUpdateInfo
import com.farmerbb.appnotifier.receivers.InstallNotificationClickedReceiver
import com.farmerbb.appnotifier.receivers.InstallNotificationDismissedReceiver
import com.farmerbb.appnotifier.receivers.UpdateNotificationClickedReceiver
import com.farmerbb.appnotifier.receivers.UpdateNotificationDismissedReceiver
import com.farmerbb.appnotifier.room.AppUpdateDAO
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class NotificationController @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val pref: SharedPreferences,
    private val dao: AppUpdateDAO,
    private val manager: NotificationManager
) {

    fun handleAppUpdateNotification(pkgInfo: PackageInfo) {
        if(!pref.getBoolean("notify_updates", true) || !verifyPrefs(pkgInfo.packageName)) return

        val info = AppUpdateInfo(
            packageName = pkgInfo.packageName,
            label = pkgInfo.applicationInfo.loadLabel(context.packageManager).toString(),
            updatedAt = Date(),
            version = pkgInfo.versionName,
            isInstall = false
        )

        CoroutineScope(Dispatchers.IO).launch {
            val list: List<AppUpdateInfo>

            dao.apply {
                deleteUpdate(info.packageName)
                list = getAllUpdates().plus(info).reversed()
                insert(info)
            }

            buildAppUpdateNotification(list, lazy { getIcon(pkgInfo) })
        }
    }

    private fun buildAppUpdateNotification(list: List<AppUpdateInfo>, icon: Lazy<Bitmap>) {
        if(list.isEmpty()) return

        val isEnhanced = pref.getString("notification_text_style", "original") == "enhanced"

        val header: String
        val content: String

        context.resources.apply {
            header = getQuantityString(R.plurals.apps_updated_header, list.size, list.size)

            if(isEnhanced && list.size == 1) {
                val info = list.first()
                info.version?.let {
                    content = getString(R.string.enhanced_template, info.label, it)
                    return@apply
                }
            }

            val subList = list.map { it.label }
                    .subList(0, min(4, list.size))
                    .toMutableList()
                    .apply {
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

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_updated)
            .setContentTitle(header)
            .setContentText(content)
            .setContentIntent(pendingContentIntent)
            .setStyle(NotificationCompat.BigTextStyle().also {
                if(isEnhanced) {
                    val text = list.joinToString("\n") { info ->
                        info.version?.let { version ->
                            context.getString(R.string.enhanced_template, info.label, version)
                        } ?: info.label
                    }

                    it.bigText(text)
                }
            })
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setAutoCancel(true)
            .setDeleteIntent(pendingDeleteIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if(isEnhanced)
            builder.setLargeIcon(icon.value)

        manager.notify(APP_UPDATE_ID, builder.build())
    }

    fun handleAppInstallNotification(pkgInfo: PackageInfo) {
        if(!pref.getBoolean("notify_installs", true) || !verifyPrefs(pkgInfo.packageName)) return

        val info = AppUpdateInfo(
                packageName = pkgInfo.packageName,
                label = pkgInfo.applicationInfo.loadLabel(context.packageManager).toString(),
                updatedAt = Date(),
                isInstall = true
        )

        CoroutineScope(Dispatchers.IO).launch {
            dao.apply {
                deleteInstall(info.packageName)
                insert(info)
            }

            buildAppInstallNotification(info.packageName, info.label, getIcon(pkgInfo))
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

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val groupBuilder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.app_updated)
                    .setGroup(APP_INSTALL_GROUP)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setStyle(NotificationCompat.BigTextStyle())
                    .setPriority(NotificationCompat.PRIORITY_LOW)

            manager.notify(APP_INSTALL_ID, groupBuilder.build())
        }

        manager.notify(code, builder.build())
    }

    fun cancelAppInstallNotification(packageName: String) {
        manager.cancel(packageName.hashCode())

        CoroutineScope(Dispatchers.IO).launch {
            dao.deleteInstall(packageName)

            if(dao.getAllInstalls().isEmpty())
                manager.cancel(APP_INSTALL_ID)
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyPrefs(packageName: String): Boolean {
        val fromPlayStore = context.packageManager.getInstallerPackageName(packageName) == PLAY_STORE_PACKAGE
        if(pref.getBoolean("notify_play_store", true) && fromPlayStore) return true
        if(pref.getBoolean("notify_other_sources", false) && !fromPlayStore) return true

        return false
    }

    private fun getIcon(pkgInfo: PackageInfo): Bitmap {
        val icon = context.packageManager.getApplicationIcon(pkgInfo.applicationInfo)
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

        CoroutineScope(Dispatchers.IO).launch {
            val updates = dao.getAllUpdates().reversed()
            if(updates.isEmpty()) return@launch

            context.getPackageInfoSafely(updates.first().packageName, dao)?.let {
                buildAppUpdateNotification(updates, lazy { getIcon(it) })
            }
        }
    }

    fun replayAppInstalls() {
        if(!pref.getBoolean("notify_installs", true)) return

        CoroutineScope(Dispatchers.IO).launch {
            for(install in dao.getAllInstalls()) {
                context.getPackageInfoSafely(install.packageName, dao)?.let {
                    buildAppInstallNotification(install.packageName, install.label, getIcon(it))
                }
            }
        }
    }
}