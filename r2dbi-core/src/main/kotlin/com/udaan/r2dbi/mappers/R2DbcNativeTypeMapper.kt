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
import com.udaan.r2dbi.sql.interfaces.ColumnMapper
import com.udaan.r2dbi.sql.interfaces.ColumnMapperFactory
import com.udaan.r2dbi.utils.wrapPrimitiveType
import io.r2dbc.spi.R2dbcType
import io.r2dbc.spi.Row
import java.util.concurrent.ConcurrentHashMap

internal class R2DbcNativeTypeMapper(private val clazz: Class<*>) : ColumnMapper<Any?> {
    override fun map(row: Row, columnIndex: Int, sqlExecutionContext: SqlExecutionContext): Any? {
        val value = row.get(columnIndex)
        return value?.let {
            castType(it, clazz)
        }
    }

    override fun map(row: Row, columnName: String, sqlExecutionContext: SqlExecutionContext): Any? {
        val value = row.get(columnName)
        return value?.let {
            castType(it, clazz)
        }
    }
}

internal class R2DbcNativeTypeMapperFactory : ColumnMapperFactory {
    private val cache = ConcurrentHashMap<Class<*>, R2DbcNativeTypeMapper>()
    override fun build(type: Class<*>): ColumnMapper<*>? {
        val boxedType = wrapPrimitiveType(type)
        val isR2DbcNativeType = R2dbcType.values().any {
            it.javaType.isAssignableFrom(boxedType)
        }
        return if (isR2DbcNativeType) {
            cache.computeIfAbsent(boxedType) {
                R2DbcNativeTypeMapper(it)
            }
        } else {
            null
        }
    }
}

private fun castType(value: Any, clazz: Class<*>): Any {
    return if (value is Number) {
        when(clazz) {
            java.lang.Long::class.java -> value.toLong()
            java.lang.Short::class.java -> value.toShort()
            java.lang.Integer::class.java -> value.toInt()
            java.lang.Byte::class.java -> value.toByte()
            java.lang.Double::class.java -> value.toDouble()
            java.lang.Float::class.java -> value.toFloat()
            else -> clazz.cast(value)
        }
    } else {
        clazz.cast(value)
    }
}
