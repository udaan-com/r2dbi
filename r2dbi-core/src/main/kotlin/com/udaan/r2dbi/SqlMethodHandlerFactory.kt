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

import com.udaan.r2dbi.sql.annotations.UseSqlMethodHandler
import com.udaan.r2dbi.utils.findAnnotationWhichIsAnnotatedWith
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.full.primaryConstructor

internal class SqlMethodHandlerFactory {
    private val cache: ConcurrentMap<Method, SqlMethodHandler> = ConcurrentHashMap()
    fun createHandler(sqlInterface: Class<*>, method: Method): SqlMethodHandler {
        val (annotation, annotatedWith) = method.findAnnotationWhichIsAnnotatedWith<UseSqlMethodHandler>()
            ?: throw IllegalStateException("No annotation found using UseSqlMethodHandler for interface: ${sqlInterface.simpleName}, method: ${method.name}")

        return cache.computeIfAbsent(method) {
            val sqlHandlerConstructor = annotatedWith.value.primaryConstructor
                ?: throw IllegalStateException("No primary constructor found for ${annotatedWith.value.simpleName}; in interface: ${sqlInterface.simpleName}, method: ${method.name}, annotation: $annotation")

            sqlHandlerConstructor.call(method)
        }
    }
}
