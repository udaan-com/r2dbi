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

import com.udaan.r2dbi.sql.interfaces.RowMapper
import com.udaan.r2dbi.sql.interfaces.RowMapperFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class RowMappers {
    private val cache = ConcurrentHashMap<Class<*>, RowMapper<*>?>()

    //NOTE:shashwat - RowMapperFactories should be polled according to the order they were added
    //Usually, when a row mapper factory is registered, there is an order in which the user
    //imagines them to be looked. First the internal factories should be used
    // and then the external registered factories
    private val rowMapperFactories = ConcurrentHashMap<KClass<out RowMapperFactory>, RowMapperFactory>()
    private val rowMapperFactoriesList = ConcurrentLinkedQueue<RowMapperFactory>()


    fun registerRowMapperFactory(rowMapperFactory: RowMapperFactory): RowMappers {
        rowMapperFactories.computeIfAbsent(rowMapperFactory::class) {
            rowMapperFactoriesList.add(rowMapperFactory)
            rowMapperFactory
        }
        return this
    }

    fun registerRowMapperFactory(rowMapperFactoryClass: KClass<out RowMapperFactory>): RowMappers {
        rowMapperFactories.computeIfAbsent(rowMapperFactoryClass) {
            //TODO -> !! operator used
            val mapperFactory = it.primaryConstructor!!.call()
            rowMapperFactoriesList.add(mapperFactory)
            mapperFactory
        }
        return this
    }

    fun registerRowMapper(klass: Class<*>, rowMapper: RowMapper<*>): RowMappers {
        cache.putIfAbsent(klass, rowMapper)
        return this
    }

    fun getRowMapperFor(klass: Class<*>): RowMapper<*>? {
        return cache.computeIfAbsent(klass, this::createRowMapperForClass)
    }

    fun getRowMapperFor(klass: Class<*>, factoryClass: KClass<out RowMapperFactory>): RowMapper<*>? {
        registerRowMapperFactory(factoryClass)
        return cache.computeIfAbsent(klass) {
            rowMapperFactories[factoryClass]
                ?.build(klass)
        }
    }

    private fun createRowMapperForClass(t: Class<*>): RowMapper<*>? {
        return rowMapperFactoriesList.firstNotNullOfOrNull {
            it.build(t)
        }
    }
}
