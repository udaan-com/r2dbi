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

fun main() {
    val sql =
        ParameterisedSql("/* this is a comment with @param, :param2*/ select a, b, c::string, d * from abcd where name = ':hi' and value = :dede and check = $1 and another_comment = /* this is a comment : it :name */ : it :value ", DefCustomiser)

    println(sql.finalSql)
    println(sql.parameters)
}

private object DefCustomiser : SqlParameterCustomizer {
    override fun getParameterName(parameter: SqlParameter): String {
        return "@${parameter.name}"
    }

    override fun getArgumentName(parameter: SqlParameter): String {
        return parameter.name
    }

}