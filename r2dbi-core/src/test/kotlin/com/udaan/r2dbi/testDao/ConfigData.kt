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

import com.udaan.r2dbi.mappers.ColumnName
import com.udaan.r2dbi.mappers.PojoMapperFactory

data class ConfigData(val name: String, val value: String)
data class ConfigDataWithNullableField(val name: String, val value: String?)

data class ConfigDataWithOptionalField(val name: String, val value: String = "hello")

data class ConfigWithEnum(val name: TestConfigName, val value: String)

class ConfigDataMapperFactory : PojoMapperFactory() {
    override fun isSupportedType(type: Class<*>): Boolean {
        return type.kotlin.isData
    }
}

data class ConfigDataWithDiffFieldNames(
    @ColumnName("name")
    val configName: String,
) {
    @ColumnName("value")
    var configValue: String = ""

}

data class ConfigDataWithDiffColumnNames(
    @ColumnName("configName")
    val name: String,

    @ColumnName("configValue")
    val value: String
)

data class ConfigWithBooleanValue(
    val name: String,
    val value: Boolean
)

internal const val CONFIG_NAME1 = "Key1"
internal const val CONFIG_NAME2 = "Key2"
internal const val CONFIG_NAME3 = "Key3"
internal const val CONFIG_NAME4 = "Key4"
internal const val CONFIG_NAME5 = "Key5"
internal const val CONFIG_NAME6 = "Key6"
