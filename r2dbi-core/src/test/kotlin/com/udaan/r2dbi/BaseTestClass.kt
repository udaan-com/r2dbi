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

import com.udaan.r2dbi.testDao.TestUpdateDao
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import reactor.test.StepVerifier
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseTestClass(private val extension: R2DbiTestExtensionBase) {
    @BeforeAll
    fun setUp() {
        extension.beforeAll()

        doInsert()
    }

    @AfterAll
    fun tearDown() {
        extension.afterAll()
    }

    private fun doInsert() = runBlocking {
        val updateDao = extension.getR2dbi().onDemand(TestUpdateDao::class.java)

        val flow = updateDao.insertSingleValue(config1.name, config1.value)
        assertEquals(1, flow.count(), "Expected 1 row to be updated")

        val flow2 = updateDao.insertSingle(config2)
        StepVerifier.create(flow2)
            .expectNext(1)
            .verifyComplete()


        val flow3 = updateDao.insertMultiple(listOf(config3, config4, config5))
        assertEquals(3, flow3.reduce { acc, value -> acc + value }, "Expected 3 inserts")

        val flow4 = updateDao.insertSingleInTransaction(config6)
        assertEquals(1, flow4.count(), "Expected 1 row to be updated")
    }
}
