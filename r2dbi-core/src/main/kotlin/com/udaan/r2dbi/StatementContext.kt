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

import io.r2dbc.spi.Result
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import io.r2dbc.spi.Statement
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

class StatementContext(
    private val parameterisedSql: ParameterisedSql,
    private val statement: Statement
) {

    fun bind(argName: String, value: Any): StatementContext {
        statement.bind(argName(argName), value)
        return this
    }

    fun bindNull(argName: String, clazz: Class<*>): StatementContext {
        statement.bindNull(argName(argName), clazz)
        return this
    }

    fun bindSqlParam(binder: (sqlParam: String) -> Unit): StatementContext {
        parameterisedSql.parameters.forEach { (_, param) ->
            binder(param.name)
        }
        return this
    }

    fun addNewBindings(): StatementContext {
        statement.add()
        return this
    }

    private fun argName(name: String): String {
        return parameterisedSql.arguments[name]
            ?: throw IllegalArgumentException("No bind-parameter found for $name")
    }

    internal fun execute(): Publisher<out Result> = statement.execute()

    fun executeUpdate(): Flux<Long> {
        val resultPublisher = statement.execute()
        return Flux.from(resultPublisher)
            .flatMap {
                it.rowsUpdated
            }
    }

    fun <T> executeQueryAndMap(mapper: (Row, RowMetadata) -> T): Flux<T> {
        val resultPublisher = statement.execute()
        return Flux.from(resultPublisher)
            .flatMap {
                it.map { row, rowMetadata ->
                    mapper(row, rowMetadata)
                }
            }
    }
}
