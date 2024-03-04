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

package com.udaan.r2dbi.utils

import com.udaan.r2dbi.mappers.ColumnName
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

internal fun toClass(javaType: Type?): Class<*>? = when (javaType) {
    is ParameterizedType -> javaType.rawType as Class<*>
    is Class<*> -> javaType
    else -> null
}

internal fun toClass(kType: KType): Class<*>? = toClass(kType.javaType)

internal fun isIterableOrArray(clazz: Class<*>) = Iterable::class.java.isAssignableFrom(clazz)
        || java.lang.Iterable::class.java.isAssignableFrom(clazz)
        || clazz.isArray

internal fun iteratorFrom(iterable: Any): Iterator<Any?>? {
    val javaClass = iterable.javaClass
    return if (Iterable::class.java.isAssignableFrom(javaClass)) {
        (iterable as Iterable<*>).iterator()
    } else if (java.lang.Iterable::class.java.isAssignableFrom(javaClass)) {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        (iterable as java.lang.Iterable<*>).iterator()
    } else if (javaClass.isArray) {
        (iterable as Array<*>).iterator()
    } else {
        null
    }
}

internal fun Type.unwrapGenericArg(): Class<*>? {
    val firstType = (this as? ParameterizedType)?.actualTypeArguments?.first()
    return if (firstType != null) {
        toClass(firstType)
    } else {
        null
    }
}


internal fun KClass<*>.simpleName(): String = this.simpleName ?: this::java.javaClass.simpleName
internal fun KParameter.name(): String? = name //findAnnotation<ColumnName>()?.value ?: name
internal fun KParameter.getName(): String {
    val annotation = this.findAnnotation<ColumnName>()
    return annotation?.value
        ?: name
        ?: throw IllegalArgumentException("No name found for parameter: $this")
}

internal fun KParameter.type(): Class<*>? = toClass(this.type.javaType)
internal fun KParameter.getType(): Class<*> = toClass(this.type.javaType)
    ?: throw IllegalArgumentException("Cannot handle paramType: ${this.type.javaType.typeName} for parameter: $this")

internal fun KMutableProperty1<*, *>.name(): String {
    val annotation = this.findAnnotation<ColumnName>()
    return annotation?.value
        ?: this.javaField?.findAnnotation<ColumnName>()
            ?.value
        ?: name
}

internal fun KMutableProperty1<*, *>.type(): Class<*>? =
    toClass(this.returnType.javaType)

internal fun KMutableProperty1<*, *>.getType(): Class<*> =
    toClass(this.returnType.javaType)
        ?: throw IllegalArgumentException("Cannot handle paramType: ${this.returnType.javaType.typeName} for parameter: $this")

internal fun <C : Any> findConstructor(kClass: KClass<C>): KFunction<C> {
    return kClass.primaryConstructor
        ?: if (kClass.constructors.size == 1) {
            kClass.constructors.first()
        } else {
            throw IllegalArgumentException("Bean ${kClass.simpleName()} is not instantiable: more than one constructor found")
        }
}


internal fun findGenericParameter(type: Type, parameterizedSupertype: Class<*>, n: Int): Type? {
    if (type is Class<*>) {
        val genericSuper = type.getGenericSuperclass()
        if (genericSuper is ParameterizedType) {
            if (genericSuper.rawType === parameterizedSupertype) {
                return genericSuper.actualTypeArguments[n]
            }
        }
    }

    return null
}

fun wrapPrimitiveType(clazz: Class<*>): Class<*> {
    return when (clazz.name) {
        "boolean" -> java.lang.Boolean::class.java
        "char" -> java.lang.Character::class.java
        "byte" -> java.lang.Byte::class.java
        "short" -> java.lang.Short::class.java
        "int" -> java.lang.Integer::class.java
        "float" -> java.lang.Float::class.java
        "long" -> java.lang.Long::class.java
        "double" -> java.lang.Double::class.java
        "void" -> Void::class.java
        else -> clazz
    }
}


private const val METADATA_FQ_NAME = "kotlin.Metadata"

/**
 * Returns true if the [Class][java.lang.Class] object represents a Kotlin class.
 *
 * @return True if this is a Kotlin class.
 */
fun Class<*>.isKotlinClass() = this.annotations.singleOrNull { it.annotationClass.java.name == METADATA_FQ_NAME } != null
