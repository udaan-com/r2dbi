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
import io.r2dbc.spi.R2dbcType
import java.lang.reflect.Method

class BindFactory : ArgumentBinderFactory<Bind> {
    override fun buildForParameter(
        annotation: Bind,
        sqlInterface: Class<*>,
        method: Method,
        methodArg: MethodArg
    ): ArgumentBinder<Any?> {
        return BindImpl(annotation, methodArg)
    }
}

class BindImpl(private val annotation: Bind, private val methodArg: MethodArg) : ArgumentBinder<Any?> {
    override fun bindArg(statementContext: StatementContext, value: Any?) {
        val argName = annotation.value
        value?.let { argValue: Any ->
            //TODO: This is a bad way of writing code; need to fix it by breaking it into an abstraction that
            // supports binding to different types extensible. See example from JDBI -> Arguments.java
            /**
             *     public Arguments(final ConfigRegistry registry) {
             *         factories = new CopyOnWriteArrayList<>();
             *         this.registry = registry;
             *
             *         // register built-in factories, priority of factories is by reverse registration order
             *
             *         // the null factory must be interrogated last to preserve types!
             *         register(new UntypedNullArgumentFactory());
             *
             *         register(new PrimitivesArgumentFactory());
             *         register(new BoxedArgumentFactory());
             *         register(new SqlArgumentFactory());
             *         register(new InternetArgumentFactory());
             *         register(new SqlTimeArgumentFactory());
             *         register(new JavaTimeArgumentFactory());
             *         register(new SqlArrayArgumentFactory());
             *         register(new CharSequenceArgumentFactory()); // register before EssentialsArgumentFactory which handles String
             *         register(new EssentialsArgumentFactory());
             *         register(new JavaTimeZoneIdArgumentFactory());
             *         register(new NVarcharArgumentFactory());
             *         register(new EnumArgumentFactory());
             *         register(new OptionalArgumentFactory());
             *         register(new DirectArgumentFactory());
             *     }
             */

            R2dbcType.values().find { r2DbcType ->
                r2DbcType.javaType.isAssignableFrom(argValue.javaClass)
            }?.let {
                statementContext.bind(argName, argValue)
            } ?: let {
                if (argValue is Enum<*>) {
                    statementContext.bind(argName, argValue.name)
                } else {
                    null
                }
            } ?: throw IllegalArgumentException("Cannot bind value type: ${value.javaClass.name} as it is not supported by ${annotation.javaClass.name} annotation")

        } ?: statementContext.bindNull(argName, methodArg.type)
    }
}



