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

/**
 * Used to indicate that a method should execute SQL batch.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@UseSqlMethodHandler(value = SqlBatchMethodHandler::class)
annotation class SqlBatch(
    /**
     * The sql to be executed.
     *
     * @return the SQL string (or name)
     */
    val value: String,

    /**
     * Whether to return updated rows or not
     * If set to <code>true</code>, then, the method's return type should be <code>Long</code>
     */
    val returnUpdatedRows: Boolean = false
)
