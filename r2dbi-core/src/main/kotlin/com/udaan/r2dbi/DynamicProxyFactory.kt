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

import com.udaan.r2dbi.sql.interfaces.ScopedExecutor
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.function.Supplier

internal class DynamicProxyFactory internal constructor(private val r2dbi: R2Dbi) {
    private val cache: ConcurrentMap<Class<*>, Any> = ConcurrentHashMap()
    private val sqlMethodHandlerFactory = SqlMethodHandlerFactory()

    fun <T> create(sqlInterface: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return cache.computeIfAbsent(sqlInterface) {
            createWithScopeSupplier(sqlInterface, r2dbi.onDemandScopedExecutorSupplier())
        } as T
    }

    internal fun <T> createWithScopeSupplier(sqlInterface: Class<T>, scopedExecutorSupplier: Supplier<ScopedExecutor>): T {
        if (!sqlInterface.isInterface) {
            throw IllegalArgumentException("${sqlInterface.simpleName} is not an interface class")
        }

        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            sqlInterface.classLoader, arrayOf(sqlInterface),
            DynamicProxyImpl(
                sqlInterface,
                scopedExecutorSupplier,
                sqlMethodHandlerFactory
            )
        ) as T
    }

    inline fun <reified T> create(): T {
        return create(T::class.java)
    }
}
