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

internal fun interface SqlParameterCustomizer {
    /**
     * parameterName is the placeholder string that are added to the sql to be substituted later
     * usually, parameterName is prefixed with some prefix
     * (ex: <code>@</code>, <code>$</code>, <code>?<code> etc)
     */
    fun getParameterName(parameter: SqlParameter): String

    /**
     * argumentName is the string that are used to identify parameters that are added as placeholder in the sql
     * The argumentName is usually the <code>string</code> after removing the prefix (as mentioned in <code>getParameterName</code>
     * In case of Postgres R2DBC Driver, the argumentName retains the prefix and hence the need for specialisation
     */
    fun getArgumentName(parameter: SqlParameter): String {
        return parameter.name
    }
}
