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

package com.udaan.r2dbi.binders

import com.udaan.r2dbi.StatementContext
import com.udaan.r2dbi.sql.interfaces.ArgumentBinder
import com.udaan.r2dbi.sql.interfaces.ArgumentBinderFactory
import com.udaan.r2dbi.sql.interfaces.MethodArg
import com.udaan.r2dbi.utils.toClass
import java.lang.reflect.Method
import kotlin.reflect.full.memberProperties

class BindPojoFactory : ArgumentBinderFactory<BindPojo> {
    override fun buildForParameter(
        annotation: BindPojo,
        sqlInterface: Class<*>,
        method: Method,
        methodArg: MethodArg
    ): ArgumentBinder<Any?> {
        return BindPojoImpl()
    }
}

class BindPojoImpl : ArgumentBinder<Any?> {
    override fun bindArg(statementContext: StatementContext, value: Any?) {
        value ?: throw IllegalArgumentException("value cannot be null for BindPojo")

        val javaClass = value.javaClass
        val properties = javaClass.kotlin.memberProperties

        statementContext.bindSqlParam { sqlParam ->
            val property = properties.find { it.name == sqlParam } ?: return@bindSqlParam

            val propValue = property.getter.call(value)
            val propType = toClass(property.returnType)
                ?: throw IllegalArgumentException("Unable to determine class of parameter ${property.name} of pojo: ${javaClass.name}")
            propValue?.let {
                statementContext.bind(sqlParam, it)
            } ?: statementContext.bindNull(sqlParam, propType)
        }
    }
}
