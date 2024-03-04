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

import com.udaan.r2dbi.sql.annotations.UseArgumentBinder

/**
 * Binds the annotated argument as a named parameter, and as a positional parameter.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@UseArgumentBinder(value = BindFactory::class)
annotation class Bind(

    /**
     * The name to bind the argument to. It is an error to omit the name
     * when there is no parameter naming information in your class files.
     *
     * @return the name to which the argument will be bound.
     */
    val value: String
)
