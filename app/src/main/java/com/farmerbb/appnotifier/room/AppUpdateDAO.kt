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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farmerbb.appnotifier.models.AppUpdateInfo

@Dao interface AppUpdateDAO {

    // Create or Update

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: AppUpdateInfo)

    // Read

    @Query("SELECT * FROM AppUpdateInfo ORDER BY updatedAt")
    suspend fun getAll(): List<AppUpdateInfo>

    // Delete

    @Query("DELETE FROM AppUpdateInfo WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM AppUpdateInfo")
    suspend fun deleteAll()
}
