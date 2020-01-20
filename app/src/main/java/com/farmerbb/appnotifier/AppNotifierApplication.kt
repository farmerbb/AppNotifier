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

import android.app.Application
import com.farmerbb.appnotifier.dagger.AppNotifierComponent
import com.farmerbb.appnotifier.dagger.AppNotifierModule
import com.farmerbb.appnotifier.dagger.DaggerAppNotifierComponent

class AppNotifierApplication: Application() {

    init {
        component = DaggerAppNotifierComponent.builder()
            .appNotifierModule(AppNotifierModule(this))
            .build()
    }

    companion object {
        lateinit var component: AppNotifierComponent
    }
}