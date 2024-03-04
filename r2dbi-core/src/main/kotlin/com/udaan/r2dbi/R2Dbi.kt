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

package com.udaan.r2dbi

import com.udaan.r2dbi.mappers.EnumMapperFactory
import com.udaan.r2dbi.mappers.PojoMapperFactory
import com.udaan.r2dbi.mappers.R2DbcNativeTypeMapperFactory
import com.udaan.r2dbi.sql.interfaces.ScopedExecutor
import com.udaan.r2dbi.sql.interfaces.SqlExecutionCallback
import com.udaan.r2dbi.utils.isKotlinClass
import io.r2dbc.pool.ConnectionPool
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.Supplier

class R2Dbi private constructor(private val sqlConnectionPool: ConnectionPool) {
    private val rowMappers: RowMappers = RowMappers()
    private val columnMappers: ColumnMappers = ColumnMappers()
    private val dynamicProxyFactory by lazy { DynamicProxyFactory(this) }

    private val parameterCustomizer: SqlParameterCustomizer by lazy {
        findParameterCustomizer()
    }

    fun getRowMappers() = rowMappers

    fun getColumnMappers() = columnMappers

    fun <T> execute(fn: SqlExecutionCallback<T>): Flux<T> {
        return Flux.usingWhen(
            openExecutionContext(),
            fn::withContext,
            SqlExecutionContext::close
        )
    }

    fun <T> onDemand(sqlInterface: Class<T>): T {
        return getFactory().create(sqlInterface)
    }

    internal fun parameterCustomizer() = parameterCustomizer

    @ExperimentalAPI
    internal fun <T> attachTo(sqlInterface: Class<T>, scopedExecutor: ScopedExecutor): T {
        return getFactory().createWithScopeSupplier(sqlInterface, scopedExecutor.thisSupplier())
    }

    @ExperimentalAPI
    internal fun createScopedExecutor(): ScopedExecutor = OnDemandScopedExecutor()

    @ExperimentalAPI
    internal fun open(): FluentR2Dbi = FluentR2Dbi(openExecutionContext())

    internal fun onDemandScopedExecutorSupplier(): Supplier<ScopedExecutor> {
        return Supplier {
            OnDemandScopedExecutor()
        }
    }

    private fun openExecutionContext(): Mono<SqlExecutionContext> = sqlConnectionPool
        .create()
        .map { conn ->
            SqlExecutionContext(conn, this)
        }

    private fun getFactory(): DynamicProxyFactory {
        return dynamicProxyFactory
    }

    private fun findParameterCustomizer(): SqlParameterCustomizer {
        return when(sqlConnectionPool.metadata.name) {
            "PostgreSQL" -> PostgreSQLParamCustomizer
            "MySQL" -> SqlParameterCustomizer { "?${it.name}" }
            "Microsoft SQL Server" -> SqlParameterCustomizer { "@${it.name}" }
            else -> {
                SqlParameterCustomizer { "@${it.name}" }
            }
        }
    }

    companion object {
        fun withPool(sqlConnectionPool: ConnectionPool): R2Dbi {
            return R2Dbi(sqlConnectionPool).also {
                it.getColumnMappers()
                    .registerColumnMapperFactory(R2DbcNativeTypeMapperFactory())
                    .registerColumnMapperFactory(EnumMapperFactory())
                it.getRowMappers()
                    .registerRowMapperFactory(DefaultKotlinClassRowMapperFactory())
            }
        }
    }

    private inner class OnDemandScopedExecutor : ScopedExecutor {
        override fun <T> use(fn: SqlExecutionCallback<T>): Publisher<T> {
            return execute(CatchingSqlExecutionCallback(fn))
        }
    }
}

private class CatchingSqlExecutionCallback<T>(private val fn: SqlExecutionCallback<T>) : SqlExecutionCallback<T> {
    override fun withContext(sqlExecutionContext: SqlExecutionContext): Publisher<T> {
        return try {
            fn.withContext(sqlExecutionContext)
        } catch (e: Exception) {
            Mono.error(e)
        }
    }
}

internal object PostgreSQLParamCustomizer : SqlParameterCustomizer {
    override fun getParameterName(parameter: SqlParameter): String {
        return "$${parameter.index + 1}"
    }

    override fun getArgumentName(parameter: SqlParameter): String {
        return "$${parameter.index + 1}"
    }

}

private class DefaultKotlinClassRowMapperFactory : PojoMapperFactory() {
    override fun isSupportedType(type: Class<*>): Boolean {
        return type.isKotlinClass()
    }
}