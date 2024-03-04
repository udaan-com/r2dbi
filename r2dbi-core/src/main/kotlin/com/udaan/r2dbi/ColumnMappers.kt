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

import com.udaan.r2dbi.sql.interfaces.ColumnMapper
import com.udaan.r2dbi.sql.interfaces.ColumnMapperFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClass

class ColumnMappers {
    private val cache = ConcurrentHashMap<Class<*>, ColumnMapper<*>?>()

    //NOTE:shashwat - ColumnMapperFactory should be polled according to the order they were added
    //Usually, when a column mapper factory is registered, there is an order in which the user
    //imagines them to be looked. First the internal factories should be used
    // and then the external registered factories
    private val factories = ConcurrentHashMap<KClass<out ColumnMapperFactory>, ColumnMapperFactory>()
    private val factoriesList = ConcurrentLinkedQueue<ColumnMapperFactory>()

    fun registerColumnMapperFactory(factory: ColumnMapperFactory): ColumnMappers {
        factories.computeIfAbsent(factory::class) {
            factoriesList.add(factory)
            factory
        }
        return this
    }

    fun getColumnMapperFor(clazz: Class<*>): ColumnMapper<*>? {
        return cache.computeIfAbsent(clazz, this::createRowMapperForClass)
    }

    private fun createRowMapperForClass(t: Class<*>): ColumnMapper<*>? {
        return factoriesList.firstNotNullOfOrNull {
            it.build(t)
        }
    }
}
