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

package com.farmerbb.appnotifier.receivers

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.farmerbb.appnotifier.AppNotifierApplication
import com.farmerbb.appnotifier.PLAY_STORE_PACKAGE
import com.farmerbb.appnotifier.isPlayStoreInstalled
import com.farmerbb.appnotifier.room.AppUpdateDAO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotificationClickedReceiver: BroadcastReceiver() {

    @Inject lateinit var dao: AppUpdateDAO

    init {
        AppNotifierApplication.component.inject(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        context.apply {
            if(!isPlayStoreInstalled()) return@apply

            try {
                startActivity(Intent("com.google.android.finsky.VIEW_MY_DOWNLOADS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: ActivityNotFoundException) {
                startActivity(packageManager.getLaunchIntentForPackage(PLAY_STORE_PACKAGE))
            }
        }

        GlobalScope.launch {
            dao.deleteAll()
        }
    }
}
