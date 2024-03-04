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

internal data class SqlParameter(val index: Int, val name: String)

class ParameterisedSql internal constructor(sql: String, private val customizer: SqlParameterCustomizer) {
    private val sqlWithoutComments = singleLineCommentsRegex.replace(sql) { "" }.trim()

    internal val parameters: Map<String, SqlParameter> = mutableMapOf<String, SqlParameter>().also { mutableMap ->
        var indexToUse = 0
        val stringWithNoQuotedStrings = quotedStringRegex.replace(sqlWithoutComments) { "" }

        paramRegex
            .findAll(stringWithNoQuotedStrings)
            .forEach { match ->
                val group = match.groups[1]
                if (group != null) {
                    //Handling a special case where '::' is used - such as in postgres queries to cast a field to a type
                    if (group.range.first > 2 && stringWithNoQuotedStrings[group.range.first - 2] == ':') {
                        // do nothing; ignore
                    } else {
                        val name = group.value
                        mutableMap.computeIfAbsent(name) {
                            val indexForParam = indexToUse++
                            SqlParameter(indexForParam, it)
                        }
                    }
                }
            }
    }

    internal val finalSql = if (paramRegex.containsMatchIn(sql)) {
        paramRegex.replace(sql) { r ->
            r.groups[1]?.let {
                parameters[it.value]
            }?.let {
                customizer.getParameterName(it)
            } ?: r.value
        }
    } else {
        sql
    }

    internal val arguments = parameters.mapValues { customizer.getArgumentName(it.value) }
}

internal class ParameterisedSqlFactory(
    private val customizer: SqlParameterCustomizer
) {
    fun create(sql: String): ParameterisedSql = ParameterisedSql(sql, customizer)
}

private const val paramIdentifier = ":"
private val paramRegex = "$paramIdentifier(\\p{Alpha}[\\w_]{0,127})".toRegex()
private val singleLineCommentsRegex = "(/\\*.*?\\*/)".toRegex()
private val quotedStringRegex = "((['\"]).*?\\2)".toRegex()

