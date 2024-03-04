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

import com.udaan.r2dbi.sql.annotations.UseArgumentBinder
import com.udaan.r2dbi.sql.interfaces.ArgumentBinder
import com.udaan.r2dbi.sql.interfaces.ArgumentBinderFactory
import com.udaan.r2dbi.sql.interfaces.MethodArg
import com.udaan.r2dbi.utils.findAnnotatedWith
import com.udaan.r2dbi.utils.findAnnotationWhichIsAnnotatedWith
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.full.primaryConstructor

class ArgumentBinders {
    private val cache: ConcurrentMap<Annotation, ArgumentBinderFactory<out Annotation>> = ConcurrentHashMap()

    fun getArgBindersFor(sqlInterface: Class<*>, method: Method): List<BoundedArgumentBinder> {
        val params = method.parameters

        return params.mapIndexedNotNull { index, param: Parameter ->
            val (argAnnotation, _) = param.findAnnotationWhichIsAnnotatedWith<UseArgumentBinder>()
                ?: return@mapIndexedNotNull null

            val arg = MethodArg(param.name, param.type, param.parameterizedType, index)

            @Suppress("UNCHECKED_CAST")
            val factory = getBinderFactoryFor(argAnnotation)
                    as ArgumentBinderFactory<Annotation>

            val argBinder = factory.buildForParameter(argAnnotation, sqlInterface, method, arg)
            BoundedArgumentBinder(arg, argBinder)
        }
    }

    private fun getBinderFactoryFor(annotation: Annotation): ArgumentBinderFactory<out Annotation> {
        return cache.computeIfAbsent(annotation) {
            val factoryClass = annotation.findAnnotatedWith<UseArgumentBinder>()
                ?.value
                ?: throw IllegalArgumentException("Not a valid UseArgumentBinder annotation. It is not annotated with ${UseArgumentBinder::class.simpleName}")

            factoryClass.primaryConstructor?.call()
                ?: throw IllegalArgumentException("Cannot create an ArgumentBinderFactory instance. No primary constructor available.")
        }
    }
}

class BoundedArgumentBinder(
    private val methodArg: MethodArg,
    private val argumentBinder: ArgumentBinder<Any?>
) : ArgumentBinder<Any?> by argumentBinder {
    fun getIndex() = methodArg.index

    fun getArgType() = methodArg.type
    fun getParameterisedType() = methodArg.parameterizedType
}
