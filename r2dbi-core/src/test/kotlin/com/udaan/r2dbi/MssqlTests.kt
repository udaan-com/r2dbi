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

import com.udaan.r2dbi.binders.Bind
import com.udaan.r2dbi.internal.AzureSqlEdgeContainerProvider
import com.udaan.r2dbi.internal.SqlDatabaseExtension
import com.udaan.r2dbi.testDao.ConfigWithBooleanValue
import com.udaan.r2dbi.testDao.TableName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MssqlTests : BaseTestClass(extension) {
    companion object {
        private val extension = SqlDatabaseExtension(AzureSqlEdgeContainerProvider())
    }

    @Nested
    inner class TestDynamicInterfaces : TestDynamicInterfaceBase(extension.getR2dbi())

    @Nested
    inner class TestMssqlTests: TestMssqlTestCases(extension.getR2dbi())

    @Nested
    inner class TestSqlExecutionContext : TestSqlExecutionContextBase(extension.getR2dbi(), extension.getPool())

    @Nested
    inner class TestFluentR2Dbi : TestFluentR2DbiBase(extension.getR2dbi())

    @Nested
    inner class TestMappers : TestMappersBase(extension.getR2dbi())

}

abstract class TestMssqlTestCases(private val r2dbi: R2Dbi) {
    private val dao by lazy { r2dbi.onDemand(MssqlQueryDao::class.java) }

    @Test
    fun `test with boolean value`() = runBlocking {
        val data = dao.getConfigWithBooleanValue(config1.name)
            .single()
        assertTrue(data.value)
    }
}

interface MssqlQueryDao {
    @SqlQuery("SELECT name, Cast(1 as bit) as value FROM $TableName where name = :name")
    fun getConfigWithBooleanValue(@Bind("name") name: String): Flow<ConfigWithBooleanValue>
}