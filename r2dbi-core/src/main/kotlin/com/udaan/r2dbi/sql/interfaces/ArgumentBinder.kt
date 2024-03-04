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

import com.udaan.r2dbi.StatementContext
import java.lang.reflect.Method
import java.lang.reflect.Type

fun interface ArgumentBinder<ArgType : Any?> {
    fun bindArg(statementContext: StatementContext, value: ArgType)
}

interface ArgumentBinderFactory<T: Annotation> {
    /**
     * Supplies a argumentbinder mapper which will map method arguments to the statement
     *
     * @param annotation   the target Annotation to bind to
     * @return an argument mapper instance.
     */
    fun buildForParameter(
        annotation: T,
        sqlInterface: Class<*>,
        method: Method,
        methodArg: MethodArg
    ): ArgumentBinder<Any?>
}

data class MethodArg(
    val name: String,
    val type: Class<*>,
    val parameterizedType: Type,
    val index: Int
)
