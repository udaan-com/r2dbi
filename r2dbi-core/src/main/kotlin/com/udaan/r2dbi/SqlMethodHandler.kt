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

import com.udaan.r2dbi.sql.annotations.UseRowMapperFactory
import com.udaan.r2dbi.sql.interfaces.RowMapper
import com.udaan.r2dbi.sql.interfaces.ScopedExecutor
import com.udaan.r2dbi.utils.unwrapGenericArg
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference

internal abstract class SqlMethodHandler(private val method: Method) : SqlInvocationHandler {
    private val declaringClass = method.declaringClass
    private val flowOrPublisherReturnType = method.returnType
    private val returnType: Class<*> = unwrapReturnType(method)

    private val methodRowMapperAnnotation: UseRowMapperFactory? = method.getAnnotation(UseRowMapperFactory::class.java)
    private val classRowMapperAnnotation: Array<UseRowMapperFactory> =
        declaringClass.getAnnotationsByType(UseRowMapperFactory::class.java)

    private val methodArgBinders = argBinders.getArgBindersFor(declaringClass, method)

    private val resolvedRowMapper = AtomicReference<RowMapper<*>>()

    private val parameterisedSqlRef = AtomicReference<ParameterisedSql>()

    override fun invoke(scopedExecutor: ScopedExecutor, target: Any, args: Array<out Any>): Any? {
        val publisher = scopedExecutor.use { executionContext ->
            val statementContext = prepareSqlStatement(executionContext, args)

            val publisher: Publisher<*> = if (returnUpdatedCount()) {
                statementContext.executeUpdate()
            } else {
                val rowMapper = resolvedRowMapper.get()
                    ?: resolvedRowMapper.updateAndGet { r ->
                        r ?: executionContext.getRowMapper()
                    }

                statementContext.executeQueryAndMap { it, _ ->
                    rowMapper.map(it, executionContext)
                        ?: throw IllegalStateException("Null value found for returnType: ${returnType.name} when mapping result in method: ${method.name} in clazz: ${declaringClass.name}")
                }
            }

            publisher
        }
        return castToReturnType(publisher)
    }

    private fun prepareSqlStatement(sqlExecutionContext: SqlExecutionContext, args: Array<out Any?>): StatementContext {
        val parameterisedSql = parameterisedSqlRef.computeIfAbsent {
            sqlExecutionContext.getParameterisedSqlFactory().create(
                getSqlString()
            )
        }
        val statementContext = sqlExecutionContext.createStatementContext(parameterisedSql)
        bindArgs(statementContext, args)

        return statementContext
    }

    private fun castToReturnType(publisher: Publisher<out Any?>): Any? {
        return if (Flow::class.java.isAssignableFrom(flowOrPublisherReturnType)) {
            publisher.asFlow()
        } else if (Flux::class.java.isAssignableFrom(flowOrPublisherReturnType)) {
            Flux.from(publisher)
        } else {
            publisher
        }
    }

    protected abstract fun getSqlString(): String

    protected abstract fun returnUpdatedCount(): Boolean

    protected open fun bindArgs(
        statementContext: StatementContext,
        args: Array<out Any?>
    ) {
        args.forEachIndexed { index, value ->
            val argBinder = getArgBinderAtIndex(index)
            argBinder.bindArg(statementContext, value)
        }
    }

    protected fun filterArgBinders(predicate: (BoundedArgumentBinder) -> Boolean): List<BoundedArgumentBinder> =
        methodArgBinders.filter(predicate)

    protected fun getArgBinderAtIndex(index: Int): BoundedArgumentBinder {
        return methodArgBinders.find { it.getIndex() == index }
            ?: throw IllegalStateException("No method arg found at index $index, sqlInterface: ${declaringClass.simpleName}, method: ${method.name}")
    }

    private fun SqlExecutionContext.getRowMapper(): RowMapper<out Any?> {
        return methodRowMapperAnnotation
            ?.let { resolveRowMapper(returnType, it) }
            ?: resolveRowMapperFromClassAnnotation()
            ?: findRowMapper(returnType)
            ?: throw IllegalStateException("No rowMapper found for type: ${returnType.typeName}, in interface: ${declaringClass.simpleName}, mthod: ${method.name}")
    }

    private fun SqlExecutionContext.resolveRowMapperFromClassAnnotation(): RowMapper<*>? {
        return classRowMapperAnnotation.firstNotNullOfOrNull {
            resolveRowMapper(returnType, it)
        }
    }

    companion object {
        internal val argBinders = ArgumentBinders()
    }
}

private fun unwrapReturnType(method: Method): Class<*> {
    if (!Flow::class.java.isAssignableFrom(method.returnType) && !Publisher::class.java.isAssignableFrom(method.returnType)) {
        throw IllegalArgumentException("Expected Flow or instance of Publisher as return type")
    }

    return method.genericReturnType.unwrapGenericArg()
        ?: throw IllegalStateException("Unable to unwrap return type ${method.genericReturnType} for class ${method.declaringClass.simpleName}, method: ${method.name}")
}

private fun <T> AtomicReference<T>.computeIfAbsent(block: () -> T): T {
    return this.get()
        ?: synchronized(this) {
            this.get()
                ?: this.updateAndGet {
                    block()
                }
        }
}
