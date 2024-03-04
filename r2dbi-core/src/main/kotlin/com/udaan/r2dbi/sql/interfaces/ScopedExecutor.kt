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

package com.udaan.r2dbi.sql.interfaces

import com.udaan.r2dbi.SqlExecutionContext
import com.udaan.r2dbi.TransactionIsolationLevel
import org.reactivestreams.Publisher
import java.util.function.Supplier

internal interface ScopedExecutor {
    fun <T> use(fn: SqlExecutionCallback<T>): Publisher<T>

    fun inTransaction(isolationLevel: TransactionIsolationLevel): ScopedExecutor {
        return compose {
            it.inTransaction(isolationLevel)
        }
    }

    fun compose(innerScopeProvider: (SqlExecutionContext) -> ScopedExecutor): ScopedExecutor {
        return ComposedScopedExecutor(this, innerScopeProvider)
    }

    fun thisSupplier(): Supplier<ScopedExecutor> = ConstantScopedExecutorSupplier.of(this)
}

private class ComposedScopedExecutor(
    private val outerScopeExecutor: ScopedExecutor,
    private val innerScopeProvider: (SqlExecutionContext) -> ScopedExecutor
) : ScopedExecutor {
    override fun <T> use(fn: SqlExecutionCallback<T>): Publisher<T> {
        return outerScopeExecutor.use {
            innerScopeProvider(it).use(fn)
        }
    }
}

private class ConstantScopedExecutorSupplier private constructor(private val scopedExecutor: ScopedExecutor) :
    Supplier<ScopedExecutor> {
    override fun get(): ScopedExecutor {
        return scopedExecutor
    }

    companion object {
        fun of(scopedExecutor: ScopedExecutor): ConstantScopedExecutorSupplier {
            return ConstantScopedExecutorSupplier(scopedExecutor)
        }
    }
}

//private class SuspendableScopedExecutorAdapter private constructor(private val scopedExecutor: ScopedExecutor) :
//    SuspendableScopedExecutor {
//    override fun <T : Any> use(fn: suspend (SqlExecutionContext) -> T): Flow<T> {
//        return scopedExecutor.use {
//            flow<T> {
//                fn(it)
//            }.asPublisher()
//        }.asFlow()
//    }
//
//    companion object {
//        fun of(scopedExecutor: ScopedExecutor): SuspendableScopedExecutor {
//            return SuspendableScopedExecutorAdapter(scopedExecutor)
//        }
//    }
//}

