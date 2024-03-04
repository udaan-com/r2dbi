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

import com.udaan.r2dbi.mappers.Nested
import com.udaan.r2dbi.testDao.CONFIG_NAME1
import com.udaan.r2dbi.testDao.TableName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

abstract class TestMappersBase(private val r2dbi: R2Dbi) {

    private val dao by lazy { r2dbi.onDemand(TestNestedObjectDao::class.java) }

//    @Test
//    fun `test column mapper ordering`() {
//        TODO("Not yet implemented")
//    }

    @Test
    fun `test pojo mapper nesting`() = runBlocking {
        val config = dao.getNestedValueConfig().single()
        assertEquals(config1.name, config.name)
        assertEquals(config1.value, config.v.value)
    }

    @Test
    fun `test pojo mapper nesting all fields`() = runBlocking {
        val config = dao.getNestedConfig().single()
        assertEquals(config1.name, config.n.name)
        assertEquals(config1.value, config.v.value)
    }

    @Test
    fun `test nullable nested config with null value`() = runBlocking {
        val config = dao.getNullValueInNullableNestedConfig().single()
        assertEquals(config1.name, config.n.name)
        assertEquals(config1.name, config.name)
        assertNull(config.v)
    }

    @Test
    fun `test nested config with null value - expect exception`() {
        val exception = assertThrows<IllegalStateException> {
            runBlocking {
                dao.getNullValueInNestedConfig().single()
            }
        }
        assertEquals("Null value found for non-nullable param: v in mappedType: com.udaan.r2dbi.NestedConfig", exception.message)
    }
}


interface TestNestedObjectDao {
    @SqlQuery("select * from $TableName where name = '$CONFIG_NAME1'")
    fun getNestedValueConfig(): Flow<NestedValueConfig>

    @SqlQuery("select * from $TableName where name = '$CONFIG_NAME1'")
    fun getNestedConfig(): Flow<NestedConfig>

    @SqlQuery("select name from $TableName where name = '$CONFIG_NAME1'")
    fun getNullValueInNullableNestedConfig(): Flow<NullableNestedConfig>

    @SqlQuery("select name from $TableName where name = '$CONFIG_NAME1'")
    fun getNullValueInNestedConfig(): Flow<NestedConfig>
}

data class NestedValueConfig(
    val name: String
) {
    @Nested
    var v: NestedValue = NestedValue("")
}

data class NestedConfig(
    @Nested val n: NestedName,

    @Nested var v: NestedValue = NestedValue("")
)

data class NullableNestedConfig(
    val name: String,

    @Nested val n: NestedName,

    @Nested
    val v: NestedValue?
)

class NestedName {
    var name: String = ""
    override fun toString(): String {
        return "NestedName(name='$name')"
    }
}

class NestedValue(
    val value: String
) {
    override fun toString(): String {
        return "NestedValue(value='$value')"
    }
}
