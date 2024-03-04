/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.udaan.r2dbi.testDao

import com.udaan.r2dbi.*
import com.udaan.r2dbi.binders.Bind
import com.udaan.r2dbi.binders.BindPojo
import kotlinx.coroutines.flow.Flow
import reactor.core.publisher.Flux

interface TestUpdateDao {
    @SqlUpdate("insert into $TableName values (:name, :value)")
    fun insertSingleValue(@Bind("name") name: String, @Bind("value") value: String): Flow<Long>
    @SqlUpdate("insert into $TableName values (:name, :value)")
    fun insertSingle(@BindPojo config: ConfigData): Flux<Long>

    @SqlBatch("insert into $TableName values (:name, :value)", returnUpdatedRows = true)
    fun insertMultiple(@BindPojo configs: List<ConfigData>): Flow<Long>

    @Transaction
    @SqlBatch("insert into $TableName values (:name, :value)", returnUpdatedRows = true)
    fun insertSingleInTransaction(@BindPojo config: ConfigData): Flow<Long>
}
