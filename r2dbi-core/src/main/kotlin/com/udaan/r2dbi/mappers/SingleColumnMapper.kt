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

import com.udaan.r2dbi.SqlExecutionContext
import com.udaan.r2dbi.sql.interfaces.ColumnMapper
import com.udaan.r2dbi.sql.interfaces.RowMapper
import io.r2dbc.spi.Row

class SingleColumnMapperByIndex<T> internal constructor(private val columnIndex: Int,
                                                 private val delegate: ColumnMapper<T>) :
    RowMapper<T> {
    override fun map(row: Row, sqlExecutionContext: SqlExecutionContext): T {
        return delegate.map(row, columnIndex, sqlExecutionContext)
    }
}

class SingleColumnMapperByName<T> internal constructor(private val name: String,
                                                        private val delegate: ColumnMapper<T>) :
    RowMapper<T> {
    override fun map(row: Row, sqlExecutionContext: SqlExecutionContext): T {
        return delegate.map(row, name, sqlExecutionContext)
    }
}

class SingleColumnMapper private constructor() {
    companion object {
        fun <T> mapperOf(delegate: ColumnMapper<T>): RowMapper<T> {
            return SingleColumnMapperByIndex(0, delegate)
        }

        fun <T> mapperOf(delegate: ColumnMapper<T>, atIndex: Int): RowMapper<T> {
            return SingleColumnMapperByIndex(atIndex, delegate)
        }

        fun <T> mapperOf(delegate: ColumnMapper<T>, name: String): RowMapper<T> {
            return SingleColumnMapperByName(name, delegate)
        }
    }
}
