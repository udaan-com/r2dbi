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

package com.udaan.r2dbi.mappers


/* TODO:shashwat
    need to implement ColumnNameMatcher that can detect if the
    ColumnName is compatible with the Class's field name basis established
    variable / column naming conventions (eg: snake_case or CamelCase)
    This will reduce the need for annotating field names with @ColumnName annotation
*/
private interface ColumnNameMatcher {
    /**
     * Returns whether the column name fits the given Java identifier name.
     *
     * @param columnName the SQL column name
     * @param fieldName   the Java property, field, or parameter name
     * @return whether the given names are logically equivalent
     */
    fun columnNameMatches(columnName: String?, fieldName: String?): Boolean

//    /**
//     * Return whether the column name starts with the given prefix, according to the matching strategy of this
//     * `ColumnNameMatcher`. This method is used by reflective mappers to short-circuit nested mapping when no
//     * column names begin with the nested prefix.
//     *
//     * By default, this method returns `columnName.startWith(prefix)`. Third party implementations should override
//     * this method to match prefixes by the same criteria as [.columnNameMatches].
//     *
//     * @param columnName the column name to test
//     * @param prefix the prefix to test for
//     * @return whether the column name begins with the prefix.
//     * @since 3.5.0
//     */
//    fun columnNameStartsWith(columnName: String, prefix: String?): Boolean {
//        return columnName.startsWith(prefix!!)
//    }
}

