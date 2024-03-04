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

import com.udaan.r2dbi.mappers.SingleColumnMapper
import com.udaan.r2dbi.sql.annotations.UseRowMapperFactory
import com.udaan.r2dbi.sql.interfaces.ColumnMapper
import com.udaan.r2dbi.sql.interfaces.RowMapper
import com.udaan.r2dbi.sql.interfaces.ScopedExecutor
import com.udaan.r2dbi.sql.interfaces.SqlExecutionCallback
import io.r2dbc.spi.Closeable
import io.r2dbc.spi.Connection
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicBoolean

class SqlExecutionContext(
    private val connection: Connection,
    private val r2dbi: R2Dbi
) : Closeable {
    private val parameterisedSqlFactory = ParameterisedSqlFactory(r2dbi.parameterCustomizer())

    private val isInTransaction = AtomicBoolean(false)

    internal fun inTransaction(isolationLevel: TransactionIsolationLevel): ScopedExecutor {
        val didStartTransaction = isInTransaction.compareAndSet(false, true)

        val transactionMono = if (didStartTransaction) {
            val isolationL = isolationLevel.r2dbcLevel ?: connection.transactionIsolationLevel
            Mono.from(
                connection.beginTransaction(isolationL)
            ).thenReturn(true)
        } else {
            Mono.just(false)
        }

        return TransactionScope(transactionMono)
    }

    internal fun getParameterisedSqlFactory(): ParameterisedSqlFactory {
        return parameterisedSqlFactory
    }
    fun createStatementContext(sql: String): StatementContext {
        return createStatementContext(parameterisedSqlFactory.create(sql))
    }

    fun createStatementContext(sql: ParameterisedSql): StatementContext {
        val statement = connection.createStatement(sql.finalSql)
        return StatementContext(sql, statement)
    }

    fun resolveRowMapper(type: Class<*>, rowMapperAnnotation: UseRowMapperFactory): RowMapper<*>? {
        return r2dbi.getRowMappers().getRowMapperFor(type, rowMapperAnnotation.value)
    }

    fun findRowMapper(type: Class<*>): RowMapper<*>? {
        return r2dbi.getRowMappers().getRowMapperFor(type)
            ?: findColumnMapper(type)?.let {
                val rowMapper = SingleColumnMapper.mapperOf(it)
                r2dbi.getRowMappers().registerRowMapper(type, rowMapper)
                rowMapper
            }
    }

    fun findColumnMapper(type: Class<*>): ColumnMapper<*>? {
        return r2dbi.getColumnMappers().getColumnMapperFor(type)
    }

    override fun close(): Publisher<Void> {
        return connection.close()
    }

    private inner class TransactionScope(
        private val transactionPublisher: Publisher<Boolean>,
    ) : ScopedExecutor {
        override fun <T> use(fn: SqlExecutionCallback<T>): Flux<T> {
            return Flux.usingWhen(
                transactionPublisher,
                {
                    fn.withContext(this@SqlExecutionContext)
                },
                { didStartTransaction ->
                    doFinally(didStartTransaction) {
                        connection.commitTransaction()
                    }
                },
                { didStartTransaction, _ ->
                    doFinally(didStartTransaction) {
                        connection.rollbackTransaction()
                    }
                },
                { didStartTransaction ->
                    doFinally(didStartTransaction) {
                        connection.rollbackTransaction()
                    }
                }
            )
        }

        private fun doFinally(didStartTransaction: Boolean, callback: () -> Publisher<*>): Mono<*> {
            return if (didStartTransaction) {
                Mono.from(
                    callback()
                ).doFinally {
                    isInTransaction.compareAndSet(true, false)
                }
            } else {
                Mono.empty<Void>()
            }
        }
    }
}

