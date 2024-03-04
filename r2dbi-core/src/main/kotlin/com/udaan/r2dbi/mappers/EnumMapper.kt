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

package com.udaan.r2dbi.mappers

import com.udaan.r2dbi.SqlExecutionContext
import com.udaan.r2dbi.sql.interfaces.ColumnGetter
import com.udaan.r2dbi.sql.interfaces.ColumnMapper
import com.udaan.r2dbi.sql.interfaces.ColumnMapperFactory
import io.r2dbc.spi.Row
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class EnumMapper(private val enumClass: Class<*>) : ColumnMapper<Enum<*>?> {
    private val enumsCache: ConcurrentMap<String, Enum<*>> = ConcurrentHashMap()
    override fun map(row: Row, columnIndex: Int, sqlExecutionContext: SqlExecutionContext): Enum<*>? {
        return map(row, sqlExecutionContext) {
            row.get(columnIndex, String::class.java)
        }
    }

    override fun map(row: Row, columnName: String, sqlExecutionContext: SqlExecutionContext): Enum<*>? {
        return map(row, sqlExecutionContext) {
            row.get(columnName, String::class.java)
        }
    }

    private fun map(row: Row, sqlExecutionContext: SqlExecutionContext, columnGetter: ColumnGetter<String>): Enum<*>? {
        val value = columnGetter.get(row)
            ?: return null

        @Suppress("UNCHECKED_CAST")
        val enumConstants = enumClass.enumConstants as Array<out Enum<*>>
        return enumsCache.computeIfAbsent(value) { enumValue ->
            enumConstants.firstOrNull { it.name == enumValue }
        }
    }
}


class EnumMapperFactory : ColumnMapperFactory {
    override fun build(type: Class<*>): ColumnMapper<*>? {
        return if (type.isEnum) {
            EnumMapper(type)
        } else {
            null
        }

    }
}

