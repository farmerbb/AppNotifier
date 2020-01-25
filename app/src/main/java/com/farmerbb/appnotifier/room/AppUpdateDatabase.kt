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

package com.farmerbb.appnotifier.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.farmerbb.appnotifier.models.AppUpdateInfo
import java.util.*

@Database(
    entities = [AppUpdateInfo::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppUpdateDatabase: RoomDatabase() {
    abstract fun getDAO(): AppUpdateDAO
}

class DateConverter {
    companion object {
        @TypeConverter @JvmStatic fun fromDate(src: Date) = src.time
        @TypeConverter @JvmStatic fun toDate(src: Long) = Date(src)
    }
}