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

import com.udaan.r2dbi.utils.unwrapGenericArg
import reactor.core.publisher.Flux
import java.lang.reflect.ParameterizedType

fun main() {
    val a: List<Int> = listOf(1)
    println(a.javaClass.typeParameters.first())

    val aParam = A::class.java.methods.first().parameters.first().parameterizedType
    println((aParam as ParameterizedType).actualTypeArguments.first())
    println(aParam.javaClass.name)

    val bParam = B::class.java.methods.first().parameters.first().parameterizedType
    println(bParam)
    println(bParam.javaClass.name)


    val cReturnType = C::class.java.methods.first().genericReturnType
    println(cReturnType)
    println(cReturnType.unwrapGenericArg())

    val dReturnType = D::class.java.methods.first().genericReturnType
    println(dReturnType)
    println(dReturnType.unwrapGenericArg())
}


interface A {
    fun list(list: List<Int>)
}
interface B {
    fun list(list: Int)
}

interface C {
    fun list(): Flux<List<Int>>
}

interface D {
    fun list(): Flux<String>
}
