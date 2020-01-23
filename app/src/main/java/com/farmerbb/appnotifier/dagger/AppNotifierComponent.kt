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

import com.farmerbb.appnotifier.AppNotifierService
import com.farmerbb.appnotifier.receivers.NotificationClickedReceiver
import com.farmerbb.appnotifier.receivers.NotificationDismissedReceiver
import com.farmerbb.appnotifier.receivers.PackageUpgradeReceiver
import com.farmerbb.appnotifier.ui.SettingsFragment
import dagger.Component
import javax.inject.Singleton

@Component(modules = [AppNotifierModule::class])
@Singleton interface AppNotifierComponent {
    fun inject(fragment: SettingsFragment)
    fun inject(service: AppNotifierService)
    fun inject(receiver: NotificationClickedReceiver)
    fun inject(receiver: NotificationDismissedReceiver)
    fun inject(receiver: PackageUpgradeReceiver)
}