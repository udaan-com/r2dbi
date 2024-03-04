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

package com.udaan.r2dbi.sql.annotations

import com.udaan.r2dbi.sql.interfaces.SqlInvocationDecorator
import kotlin.reflect.KClass

/**
 * Meta-Annotation used to decorate a SqlInvocationDecorator. Use this to annotate an annotation.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS)
internal annotation class UseInvocationDecorator(
    /**
     * [SqlInvocationDecorator] annotation that decorates a <code>SqlInvocationHandler</code>.
     * <br></br>
     * The SqlInvocationDecorator must have  a `(annotation: Annotation)` constructor.
     *
     * @return the [SqlInvocationDecorator] class
     */
    val value: KClass<out SqlInvocationDecorator>
)
