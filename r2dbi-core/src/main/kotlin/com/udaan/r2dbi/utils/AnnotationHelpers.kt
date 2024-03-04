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

import java.lang.reflect.AnnotatedElement
import kotlin.reflect.full.findAnnotation

inline fun <reified T : Annotation> Annotation.findAnnotatedWith(): T? {
    return annotationClass.findAnnotation<T>()
}

inline fun <reified T : Annotation> AnnotatedElement.findAnnotation(): T? {
    return this.getAnnotation(T::class.java)
}


inline fun <reified T : Annotation> AnnotatedElement.findAnnotationWhichIsAnnotatedWith(): Pair<Annotation, T>? {
    return findAllAnnotationsWhichIsAnnotatedWith<T>()
        .firstOrNull()
}

inline fun <reified T : Annotation> AnnotatedElement.findAllAnnotationsWhichIsAnnotatedWith(): List<Pair<Annotation, T>> {
    return annotations.mapNotNull { annotation ->
        val annotatedWith = annotation.findAnnotatedWith<T>()
        annotatedWith?.let {
            annotation to it
        }
    }
}
