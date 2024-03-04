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

import com.udaan.r2dbi.binders.BindPojoImpl
import com.udaan.r2dbi.utils.wrapPrimitiveType
import io.r2dbc.spi.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import java.util.function.Consumer

@ExperimentalAPI
class FluentR2Dbi(private val sqlExecutionContextMono: Mono<SqlExecutionContext>) {

    fun sql(sql: String): SqlStatement {
        val statementContextMono = sqlExecutionContextMono.map {
            StatementState(it.createStatementContext(sql), it)
        }

        return SqlStatement(statementContextMono)
    }
}

class SqlStatement internal constructor(statementStateMono: Mono<StatementState>) {
    private var thisStatementStateMono: Mono<StatementState> = statementStateMono

    fun bindName(name: String, value: Any): SqlStatement {
        thisStatementStateMono = thisStatementStateMono.map {
            it.statementContext.bind(name, value)
            it
        }
        return this
    }

    fun bindPojo(pojo: Any): SqlStatement {
        thisStatementStateMono = thisStatementStateMono.map {
            BindPojoImpl().bindArg(it.statementContext, pojo)
            it
        }

        return this
    }

    fun execute(): ResultBearing {
        val resultFlux = thisStatementStateMono.flatMapMany {state ->
            val result = state.statementContext.execute()
            Flux.from(result).map {
                ResultState(it, state.sqlExecutionContext)
            }
        }

        return ResultBearing(resultFlux)
    }
}

class ResultBearing internal constructor(private val resultSateFlux: Flux<ResultState>) {
    fun subscribeRowsUpdated(onLongValue: Consumer<Long>) {
        mapToRowsUpdated()
            .subscribe(onLongValue)
    }

    fun rowsUpdated(): Flow<Long> {
        return mapToRowsUpdated()
            .asFlow()
    }

    fun <T : Any> subscribeNotNull(clazz: Class<T>, onValue: Consumer<T>) {
        mapRowsInternal(clazz)
            .subscribe {
                when (it) {
                    is Optional<*> -> {}
                    else -> onValue.accept(it as T)
                }
            }
    }

    fun <T : Any> subscribeRows(clazz: Class<T>, onValue: Consumer<T?>) {
        mapRowsInternal(clazz)
            .subscribe {
                when (it) {
                    is Optional<*> -> onValue.accept(null)
                    else -> onValue.accept(it as T)
                }
            }
    }

    fun <T : Any> mapToNotNull(clazz: Class<T>): Flow<T> {
        @Suppress("UNCHECKED_CAST")
        return (mapRowsInternal(clazz).filter {
            it !is Optional<*>
        } as Flux<T>)
            .asFlow()
    }

    private fun mapToRowsUpdated(): Flux<Long> {
        return scopedExecution { (result, _) ->
            result.rowsUpdated
        }
    }

    private fun <T> mapRowsInternal(clazz: Class<T>): Flux<Any> {
        if (clazz.isPrimitive) {
            return mapRowsInternal(wrapPrimitiveType(clazz))
        }

        return scopedExecution { (result, sqlExecutionContext) ->
            val rowMapper = sqlExecutionContext.findRowMapper(clazz)
                ?: return@scopedExecution Flux.error(IllegalArgumentException("No row mapper found for ${clazz.name}"))

            result.map { row, _ ->
                val value = rowMapper.map(row, sqlExecutionContext)
                value?.let {
                    value
                } ?: Optional.empty<T>()
            }
        }
    }

    private fun <T> scopedExecution(fn: (ResultState) -> Publisher<T>): Flux<T> {
        return Flux.usingWhen(
            resultSateFlux,
            {
                withTryCatch(fn, it)
            },

            ) {
            it.sqlExecutionContext.close()
        }
    }

    private fun <T> withTryCatch(
        fn: (ResultState) -> Publisher<T>,
        it: ResultState
    ): Publisher<T> {
        return try {
            fn(it)
        } catch (e: Exception) {
            Flux.error(e)
        }
    }
}


internal data class ResultState(
    val result: Result,
    val sqlExecutionContext: SqlExecutionContext
)

internal data class StatementState(
    val statementContext: StatementContext,
    val sqlExecutionContext: SqlExecutionContext
)

