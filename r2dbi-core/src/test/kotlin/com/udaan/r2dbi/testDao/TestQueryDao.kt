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

import com.udaan.r2dbi.SqlBatch
import com.udaan.r2dbi.SqlQuery
import com.udaan.r2dbi.binders.Bind
import com.udaan.r2dbi.binders.BindPojo
import com.udaan.r2dbi.sql.annotations.UseRowMapperFactory
import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

@UseRowMapperFactory(ConfigDataMapperFactory::class)
interface TestQueryDao {

    @SqlQuery("SELECT 1")
    fun getOne(): Flow<Long>

    @SqlQuery("SELECT * FROM $TableName")
    fun getConfig(): Flow<ConfigData>

    @SqlQuery("SELECT * FROM $TableName where name = :name")
    fun getConfigByName(@Bind("name") name: String): Flow<ConfigData>

    @SqlQuery("SELECT * FROM $TableName where name = :name")
    fun getConfigByNameWithDifferentFieldNames(@Bind("name") name: String): Flow<ConfigDataWithDiffFieldNames>

    @SqlQuery("SELECT name as configName, value as configValue FROM $TableName where name = :name")
    fun getConfigByNameWithDifferentColumnNames(@Bind("name") name: String): Flow<ConfigDataWithDiffColumnNames>

    @SqlQuery("SELECT * FROM $TableName where name = :name")
    fun getConfigByNameAsPublisher(@Bind("name") name: String): Publisher<ConfigData>

    @SqlQuery("SELECT * FROM $TableName where name = :name")
    fun getConfigByPojo(@BindPojo config: ConfigData): Flow<ConfigData>

    @SqlBatch("SELECT * FROM $TableName where name = :name and value = :value")
    fun getBatch(@Bind("name") list: List<String>, @Bind("value") value: String): Flow<ConfigData>

    @SqlBatch("SELECT * FROM $TableName where name = :name and value = :value")
    fun getBatchAsPublisher(@Bind("name") list: List<String>, @Bind("value") value: String): Flux<ConfigData>

    @SqlBatch("SELECT * FROM $TableName where name = :name and value = :value", returnUpdatedRows = true)
    fun getBatchAsPublisherAndRowsUpdated(
        @Bind("name") list: List<String>,
        @Bind("value") value: String
    ): Publisher<Long>

    @SqlQuery("SELECT value FROM $TableName where name = :name")
    fun getSingleString(@Bind("name") name: String): Flow<String>

    @SqlQuery("SELECT count(*) FROM $TableName where name = :name")
    fun getCountOfValues(@Bind("name") name: String): Publisher<Long>

    @SqlQuery("SELECT count(*) FROM $TableName where name = :name")
    fun getCountOfValues(@Bind("name") name: TestConfigName): Publisher<Long>

    @SqlQuery("SELECT * FROM $TableName where name = :name")
    fun getConfigWithEnumValue(@Bind("name") name: String): Flow<ConfigWithEnum>

    @SqlQuery("SELECT name FROM $TableName where name = :name")
    fun getConfigNameOnlyAssumingValueIsOptional(@Bind("name") name: String): Flow<ConfigDataWithOptionalField>

    @SqlQuery("SELECT name FROM $TableName where name = :name")
    fun getConfigNameOnly(@Bind("name") name: String): Flow<ConfigData>

    @SqlQuery("SELECT name, null as value FROM $TableName where name = :name")
    fun getConfigAndForceValueAsNull(@Bind("name") name: String): Flow<ConfigDataWithNullableField>

    @SqlQuery("SELECT name, null as value FROM $TableName where name = :name")
    fun getConfigAndValueAsNull(@Bind("name") name: String): Flow<ConfigData>

    @SqlQuery("SELECT name FROM $TableName where name = :name")
    fun getNoValueColForNonOptionalButNullableField(@Bind("name") name: String): Flow<ConfigDataWithNullableField>

    @SqlQuery("Select name as colName from $TableName where name = '$CONFIG_NAME1'")
    fun getNullReturn(): Flow<ConfigData>

    @SqlQuery("SELECT name, value FROM $TableName where name = :name and value = :name")
    fun getConfigWithSameNameAndValue(@Bind("name") name: String): Flow<ConfigData>
}

internal const val TableName = "test"
enum class TestConfigName {
    Key1,
    Key2
}

