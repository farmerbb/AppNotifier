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

package com.farmerbb.appnotifier.dagger

import android.app.NotificationManager
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.farmerbb.appnotifier.room.AppUpdateDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module class AppNotifierModule(private val context: Context) {
    @Provides @Singleton fun provideContext() = context

    @Provides @Singleton fun provideSharedPreferences(context: Context)
            = PreferenceManager.getDefaultSharedPreferences(context)

    @Provides @Singleton fun provideNotificationManager(context: Context)
            = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides @Singleton fun provideDatabase(context: Context)
            = Room.databaseBuilder(context, AppUpdateDatabase::class.java, "app_updates").build()

    @Provides @Singleton fun provideDAO(db: AppUpdateDatabase) = db.getDAO()
}