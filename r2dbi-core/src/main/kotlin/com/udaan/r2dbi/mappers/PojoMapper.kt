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
import com.udaan.r2dbi.sql.interfaces.RowMapper
import com.udaan.r2dbi.sql.interfaces.RowMapperFactory
import com.udaan.r2dbi.utils.*
import io.r2dbc.spi.ColumnMetadata
import io.r2dbc.spi.Row
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * A very basic Pojo mapper that can construct a class with a default constructor or mutable members
 * NOTE: At present, it does not support nested objects
 */
class PojoMapper(private val clazz: Class<*>) : RowMapper<Any?> {
    private val kClass = clazz.kotlin
    private val constructor = findConstructor(kClass)
    private val constructorMapper = getBoundedConstructorParameterMapper()
    private val memberMapper = getBoundedFieldMapper()

    override fun map(row: Row, sqlExecutionContext: SqlExecutionContext): Any? {
        val instance = constructorMapper.map(row, sqlExecutionContext)
            ?: return null

        memberMapper.map(instance, row, sqlExecutionContext)
        return instance
    }

    private inner class BoundedConstructorMapper(columnProperties: Map<KParameter, ColumnProperty>) :
        BoundedMapperBase<KParameter>(clazz, columnProperties) {

        fun map(row: Row, sqlExecutionContext: SqlExecutionContext): Any? {
            val (paramValues, mandatoryMissingParams) = mapPropToValue(row, sqlExecutionContext)
            val args = paramValues.toMap()

            if (args.isNotEmpty() && mandatoryMissingParams.isNotEmpty()) {
                // It seems that we found values for only a few of the constructor parameters.
                // Given that we cannot construct the object, it is better to throw an exception here
                val missingParams = mandatoryMissingParams.joinToString { it.second.resolvedName }
                throw IllegalStateException("No value found for mandatory param: $missingParams in mappedType: ${clazz.name}")
            }

            if (args.isEmpty() && mandatoryMissingParams.isNotEmpty()) {
                // It seems that we did not find any value for constructor parameters
                // that will enable us to construct the object.
                // But the constructor seems to have mandatory parameters.
                // So we can only return `null` from here.
                // The other alternative is to throw exception, which does not sound wise.
                return null
            }

            return constructor.callBy(args)
        }
    }

    private inner class BoundedFieldMapper(columnProperties: Map<KMutableProperty1<*, *>, ColumnProperty>) :
        BoundedMapperBase<KMutableProperty1<*, *>>(clazz, columnProperties) {

        fun map(instance: Any, row: Row, sqlExecutionContext: SqlExecutionContext) {
            val (paramValues, _) = mapPropToValue(row, sqlExecutionContext)

            paramValues.forEach { (prop, value) ->
                prop.setter.call(instance, value)
            }
        }
    }

    private fun getBoundedConstructorParameterMapper(): BoundedConstructorMapper {
        val columnProperties: Map<KParameter, ColumnProperty> = constructor.parameters.associateWith { parameter ->
            ColumnProperty.of(parameter)
        }

        return BoundedConstructorMapper(columnProperties)
    }

    private fun getBoundedFieldMapper(): BoundedFieldMapper {
        val memberProperties: List<KMutableProperty1<*, *>> = kClass.memberProperties
            .mapNotNull { it as? KMutableProperty1<*, *> }
            .filter { property ->
                !constructor.parameters.any { parameter -> parameter.name() == property.name() }
            }

        val columnProperties: Map<KMutableProperty1<*, *>, ColumnProperty> = memberProperties.associateWith {
            ColumnProperty.of(it)
        }

        return BoundedFieldMapper(columnProperties)
    }
}

private interface ColumnProperty {
    val resolvedName: String
    val type: Class<*>
    val isOptional: Boolean
    val isNullable: Boolean
    val isNested: Boolean

    companion object {
        fun of(param: KParameter): ColumnProperty {
            return object : ColumnProperty {
                override val resolvedName = param.getName()

                override val type: Class<*> = param.getType()

                override val isOptional: Boolean = param.isOptional

                override val isNullable: Boolean = param.type.isMarkedNullable

                override val isNested: Boolean = param.findAnnotation<Nested>() != null
            }
        }

        fun of(param: KMutableProperty1<*, *>): ColumnProperty {
            return object : ColumnProperty {
                override val resolvedName: String = param.name()

                override val type: Class<*> = param.getType()

                override val isOptional: Boolean = !param.isLateinit

                override val isNullable: Boolean = param.returnType.isMarkedNullable

                override val isNested: Boolean = (
                        param.findAnnotation<Nested>()
                            ?: param.javaField?.findAnnotation<Nested>()
                            ?: param.setter.findAnnotation<Nested>()
                        ) != null
            }
        }

    }
}

abstract class PojoMapperFactory : RowMapperFactory {
    override fun build(type: Class<*>): RowMapper<*>? {
        return if (isSupportedType(type)) {
            PojoMapper(type)
        } else {
            null
        }
    }

    abstract fun isSupportedType(type: Class<*>): Boolean
}

private abstract class BoundedMapperBase<T>(
    private val mappedType: Class<*>,
    private val columnProperties: Map<T, ColumnProperty>
) {
    private val columnPropertiesSeq = columnProperties.asSequence()
    private val memberCache = ConcurrentHashMap<T, NullCheckingRowMapper<*>>()
    private val hasMandatoryParams = columnProperties.values.any { !it.isNullable && !it.isOptional }
    protected fun hasMandatoryParams() = hasMandatoryParams
    protected fun hasParams() = columnProperties.isNotEmpty()

    protected fun mapPropToValue(row: Row, sqlExecutionContext: SqlExecutionContext): BoundedMapResponse<T> {
        val columnMetadataList = row.metadata.columnMetadatas
        val mandatoryMissingParams = mutableListOf<Pair<T, ColumnProperty>>()

        val values = columnPropertiesSeq
            //NOTE: shashwat, the same mapper may be used for different kinds of queries,
            // and hence it is required to check if the column is present in the row in every map-attempt
            .mapNotNull { (prop, columnProperty) ->
                if (columnProperty.isNested) {
                    val mapper = memberCache.computeIfAbsent(prop) {
                        getRowMapperFor(sqlExecutionContext, columnProperty)
                    }
                    val value = mapper.map(row, sqlExecutionContext)
                    prop to value
                } else if (columnMetadataList.isColumPropertyPresentInMetaData(columnProperty)) {
                    val mapper = memberCache.computeIfAbsent(prop) {
                        getColumnMapperFor(sqlExecutionContext, columnProperty)
                    }
                    val value = mapper.map(row, sqlExecutionContext)
                    prop to value
                } else if (columnProperty.isNullable) {
                    prop to null
                } else {
                    if (!columnProperty.isOptional) {
                        mandatoryMissingParams.add(prop to columnProperty)
                    }
                    null
                }
            }

        return BoundedMapResponse(values, mandatoryMissingParams)
    }

    private fun List<ColumnMetadata>.isColumPropertyPresentInMetaData(
        columnProperty: ColumnProperty
    ): Boolean {
        val resolvedName = columnProperty.resolvedName
        val columnMetadata = this.find {
            //NOTE: PostgreSQL driver returns column names in lower case and hence we need to
            // ignore case when comparing columnName in columnMetadata with property's resolvedName
            it.name.equals(resolvedName, ignoreCase = true)
        }
        return columnMetadata != null
    }

    private fun getColumnMapperFor(
        sqlExecutionContext: SqlExecutionContext,
        columnProperty: ColumnProperty,
    ): NullCheckingRowMapper<*> {
        val name = columnProperty.resolvedName
        val type = columnProperty.type
        val columnMapper = sqlExecutionContext.findColumnMapper(type)
            ?: throw IllegalArgumentException("No column mapper found for param: $name, type: ${type.name}, mappedType: ${mappedType.name}")
        val mapper = SingleColumnMapper.mapperOf(columnMapper, name)
        return NullCheckingRowMapper(mappedType, columnProperty, mapper)
    }

    private fun getRowMapperFor(
        sqlExecutionContext: SqlExecutionContext,
        columnProperty: ColumnProperty,
    ): NullCheckingRowMapper<*> {
        val name = columnProperty.resolvedName
        val type = columnProperty.type
        val mapper = sqlExecutionContext.findRowMapper(type)
            ?: throw IllegalArgumentException("No row mapper found for param: $name, type: ${type.name}, mappedType: ${mappedType.name}")
        return NullCheckingRowMapper(mappedType, columnProperty, mapper)
    }
}

private data class BoundedMapResponse<T>(
    val paramValues: Sequence<Pair<T, Any?>>,
    val mandatoryMissingParams: List<Pair<T, ColumnProperty>>
)

private class NullCheckingRowMapper<T>(
    private val mappedType: Class<*>,
    private val columnProperty: ColumnProperty,
    private val rowMapper: RowMapper<T>
) : RowMapper<T> {
    override fun map(row: Row, sqlExecutionContext: SqlExecutionContext): T {
        val value = rowMapper.map(row, sqlExecutionContext)
        if (value == null && !columnProperty.isNullable) {
            throw IllegalStateException("Null value found for non-nullable param: ${columnProperty.resolvedName} in mappedType: ${mappedType.name}")
        }

        return value
    }
}
