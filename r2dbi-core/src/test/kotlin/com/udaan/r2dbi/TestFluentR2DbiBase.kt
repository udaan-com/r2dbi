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

import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalAPI::class)
abstract class TestFluentR2DbiBase(private val r2dbi: R2Dbi) {

    @Test
    fun testFluent() = runBlocking {
        r2dbi.open()
            .sql("select 1")
            .execute()
            .subscribeNotNull(Int::class.java) {
                println(it)
                assertEquals(1, it)
            }
    }

    @Test
    fun testFluentNull() = runBlocking {
        r2dbi.open()
            .sql("select null")
            .execute()
            .subscribeRows(Int::class.java) {
                println(it)
                assertNull(it)
            }
    }

    @Test
    fun testFluentWithFlow() = runBlocking {
        val long = r2dbi.open()
            .sql("select 1")
            .execute()
            .mapToNotNull(Long::class.java)
            .single()

        assertEquals(1, long)
    }
}
