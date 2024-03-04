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

import com.udaan.r2dbi.sql.annotations.UseInvocationDecorator
import com.udaan.r2dbi.sql.interfaces.ScopedExecutor
import com.udaan.r2dbi.utils.findAllAnnotationsWhichIsAnnotatedWith
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.function.Supplier
import kotlin.reflect.full.primaryConstructor

class DynamicProxyImpl<T> internal constructor(
    private val sqlInterface: Class<T>,
    private val scopedExecutorSupplier: Supplier<ScopedExecutor>,
    private val sqlMethodHandlerFactory: SqlMethodHandlerFactory
) : InvocationHandler {
    private val allHandlers: Map<Method, SqlInvocationHandler> = getMethodHandlers()

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (EQUALS_METHOD.equals(method)) {
            return proxy === args?.get(0)
        }

        if (HASHCODE_METHOD.equals(method)) {
            return System.identityHashCode(proxy)
        }

        if (TOSTRING_METHOD.equals(method)) {
            return "R2dbi on demand proxy for ${sqlInterface.getName()}@${
                Integer.toHexString(System.identityHashCode(proxy))
            }"
        }

        val sqlInvocationHandler = findMethodHandler(method)
        val scopedExecutor = scopedExecutorSupplier.get()

        return sqlInvocationHandler.invoke(scopedExecutor, proxy, args ?: arrayOf())
    }

    private fun getMethodHandlers(): Map<Method, SqlInvocationHandler> {
        return sqlInterface.methods.associateWith { method ->
            val handler = sqlMethodHandlerFactory.createHandler(sqlInterface, method)
            decorateInvocationHandler(method, handler)
        }
    }

    private fun decorateInvocationHandler(
        method: Method,
        handler: SqlInvocationHandler
    ): SqlInvocationHandler {
        val decorators = method.findAllAnnotationsWhichIsAnnotatedWith<UseInvocationDecorator>()
            .mapNotNull { (annotation, useAnnotation) ->
                useAnnotation.value.primaryConstructor?.call(annotation)
            }

        val finalHandler = decorators.fold(handler) { invocationHandler, sqlInvocationDecorator ->
            sqlInvocationDecorator.decorate(invocationHandler)
        }

        return finalHandler
    }

    private fun findMethodHandler(method: Method): SqlInvocationHandler {
        return allHandlers.get(method)
            ?: throw IllegalStateException("No handler found for interface: ${sqlInterface.simpleName}, method: ${method.name} ")
    }
}

private val objectClass = Object::class.java
private val TOSTRING_METHOD = objectClass.getMethod("toString")
private val HASHCODE_METHOD = objectClass.getMethod("hashCode")
private val EQUALS_METHOD = objectClass.getMethod("equals", objectClass)
