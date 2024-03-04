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

package com.udaan.r2dbi.sql.interfaces

import com.udaan.r2dbi.SqlExecutionContext
import io.r2dbc.spi.Row

interface ColumnMapper<T> {
    fun map(row: Row, columnIndex: Int, sqlExecutionContext: SqlExecutionContext): T

    fun map(row: Row, columnName: String, sqlExecutionContext: SqlExecutionContext): T
}

interface ColumnMapperFactory {
    /**
     * Supplies a column mapper which will map a column in a row to a type if the factory supports it; null otherwise.
     *
     * @param type   the target type to map to
     * @return a column mapper for the given type if this factory supports it; <code>null</code> otherwise.
     */
    fun build(type: Class<*>): ColumnMapper<*>?
}


fun interface ColumnGetter<T> {
    fun get(row: Row): T?
}
