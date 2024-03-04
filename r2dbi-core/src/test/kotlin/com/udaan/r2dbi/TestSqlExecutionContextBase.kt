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

import com.udaan.r2dbi.testDao.TestQueryDao
import io.r2dbc.pool.ConnectionPool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalAPI::class)
abstract class TestSqlExecutionContextBase(private val r2dbi: R2Dbi, private val pool: ConnectionPool) {
    @Test
    fun `test single dao attached to SqlExecutionContext`() = runBlocking {
        val scopedExecutor = r2dbi.createScopedExecutor()
        val dao = r2dbi.attachTo(TestQueryDao::class.java, scopedExecutor)

        val configData = dao.getConfig()
        assertEquals(6, configData.count(), "Expected 6 entries")

        val configByName = dao.getConfigByName(config1.name)
        val singleVal = configByName.single()
        assertEquals(config1, singleVal)
    }

    @Test
    fun `test multiple dao attached to SqlExecutionContext`() = runBlocking {
        val scopedExecutor = r2dbi.createScopedExecutor()
        val dao = r2dbi.attachTo(TestQueryDao::class.java, scopedExecutor)

        val configData = dao.getConfig()
        assertEquals(6, configData.count(), "Expected 6 entries")

        val configByName = dao.getConfigByName(config1.name)
        val singleVal = configByName.single()
        assertEquals(config1, singleVal)
    }
}
