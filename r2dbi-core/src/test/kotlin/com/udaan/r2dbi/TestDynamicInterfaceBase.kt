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

import com.udaan.r2dbi.testDao.*
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.test.StepVerifierOptions
import kotlin.test.assertNotNull
import kotlin.test.assertNull

abstract class TestDynamicInterfaceBase(private val r2dbi: R2Dbi) {

    private val dao: TestQueryDao by lazy { r2dbi.onDemand(TestQueryDao::class.java) }

    @Test
    fun `test simple query`() = runBlocking {
        val singleValue = dao.getOne().single()
        assertEquals(1, singleValue)
    }

    @Test
    fun `test querying empty results`() = runBlocking {
        dao.getConfigByName("some random key name").collect {
            Assertions.assertNull(it, "Expected empty response")
        }
    }

    @Test
    fun `test transaction failure - with txn failure - scenario 1`() = runBlocking {
        val (beforeTx, inTx, afterTx) = transactionScenario().blockLast()!!

        // Note: DO NOT Change the order; inTx emits error() and will stop further publishes and prevent
        // afterTx from publishing any result, causing the insert to never happen
        val orderedExecution = beforeTx.concatWith(inTx).concatWith(afterTx)

        StepVerifier.create(orderedExecution)
            .expectNext(1)
            .expectError(IllegalStateException::class.java)
            .verify()

        dao.getConfigByNameAsPublisher(failureKey).let {
            StepVerifier.create(it, StepVerifierOptions.create().scenarioName("Transaction that failed"))
        }.verifyComplete()

        dao.getConfigByNameAsPublisher(successKeyBeforeTx).let {
            StepVerifier.create(it, StepVerifierOptions.create().scenarioName("Before Transaction"))
        }.expectNextCount(1)
            .verifyComplete()

        dao.getConfigByNameAsPublisher(successKeyAfterTx).let {
            StepVerifier.create(it, StepVerifierOptions.create().scenarioName("After Transaction"))
        }
            .verifyComplete()

        r2dbi.execute {
            it.createStatementContext("delete from $TableName where name in ('$successKeyBeforeTx', '$successKeyAfterTx')")
                .executeUpdate()
        }.blockLast()

        Unit
    }

    @Test
    fun `test transaction failure - with txn failure - scenario 2`() = runBlocking {
        val (beforeTx, inTx, afterTx) = transactionScenario().blockLast()!!

        // Note: DO NOT Change the order; inTx emits error() and will stop further publishes.
        // In this scenario, we are testing if we wait for afterTx first and then execute inTx
        // The error from inTx MUST NOT prevent beforeTx and afterTx to execute
        val orderedExecution = beforeTx
            .concatWith(afterTx)
            .concatWith(inTx)

        // NOTE that we are expecting beforeTx and afterTx - both to succeed
        StepVerifier.create(orderedExecution)
            .expectNext(1)
            .expectNext(1)
            .expectError(IllegalStateException::class.java)
            .verify()

        dao.getConfigByNameAsPublisher(failureKey).let {
            StepVerifier.create(it, StepVerifierOptions.create().scenarioName("Transaction that failed"))
        }.verifyComplete()

        dao.getConfigByNameAsPublisher(successKeyBeforeTx).let {
            StepVerifier.create(it, StepVerifierOptions.create().scenarioName("Before Transaction"))
        }.expectNextCount(1)
            .verifyComplete()

        dao.getConfigByNameAsPublisher(successKeyAfterTx).let {
            StepVerifier.create(it, StepVerifierOptions.create().scenarioName("After Transaction"))
        }.expectNextCount(1)
            .verifyComplete()

        r2dbi.execute {
            it.createStatementContext("delete from $TableName where name in ('$successKeyBeforeTx', '$successKeyAfterTx')")
                .executeUpdate()
        }.blockLast()

        Unit
    }

    @Test
    fun `test dao`() = runBlocking {
        val configData = dao.getConfig()
        assertEquals(6, configData.count(), "Expected 6 entries")

        val configByName = dao.getConfigByName(CONFIG_NAME1)
        val singleVal = configByName.single()
        assertEquals(config1, singleVal)
    }

    @Test
    fun `test bind pojo`() = runBlocking {
        val configData = dao.getConfigByPojo(config1)
        val config = configData.single()
        assertEquals(config1, config)
    }

    @Test
    fun `test batch`() = runBlocking {
        val configData = dao.getBatch(listOf(CONFIG_NAME1, CONFIG_NAME2, CONFIG_NAME3), config2.value)
        val list = configData.toList()
        assertEquals(1, list.size)
        assertEquals(config2, list.first())
    }

    @Test
    fun `test batch as publisher`() = runBlocking {
        val listOfNames = listOf(config1, config2, config3).map { it.name }
        val configData = dao.getBatchAsPublisher(listOfNames, config2.value)
        StepVerifier.create(configData)
            .expectNext(config2)
            .verifyComplete()

        val long: Publisher<Long> =
            dao.getBatchAsPublisherAndRowsUpdated(listOfNames, config2.value)

        StepVerifier.create(long)
            .expectNext(0)
            .expectNext(1)
            .expectNext(0)
            .verifyComplete()

        Unit
    }

    @Test
    fun `test single`() = runBlocking {
        val configData = dao.getSingleString(CONFIG_NAME1)
        val list = configData.toList()
        assertEquals(1, list.size)
        assertEquals(config1.value, list.first())
    }

    @Test
    fun `test enum bindings`() {
        val configData = Flux.from(dao.getCountOfValues(TestConfigName.Key1))
            .reduce { a, v -> a + v }
            .block()

        assertNotNull(configData)
        assertEquals(1, configData)
    }

    @Test
    fun `test enum row mapping`() = runBlocking {
        val configWithEnumData = dao.getConfigWithEnumValue(CONFIG_NAME1)
            .single()
        assertEquals(ConfigWithEnum(TestConfigName.Key1, config1.value), configWithEnumData)
    }

    @Test
    fun `test nullable value`() = runBlocking {
        val value = dao.getConfigAndForceValueAsNull(config1.name)
            .single()
        assertEquals(config1.name, value.name)
        assertNull(value.value)
    }

    @Test
    fun `test nullable value but no column`() = runBlocking {
        val value = dao.getNoValueColForNonOptionalButNullableField(config1.name)
            .single()
        assertEquals(config1.name, value.name)
        assertNull(value.value)
    }

    @Test
    fun `test nullable value and expect exception`() {
        val exception = assertThrows<IllegalStateException> {
            runBlocking {
                dao.getConfigAndValueAsNull(config1.name)
                    .single()
            }
        }

        assertEquals(
            "Null value found for non-nullable param: value in mappedType: com.udaan.r2dbi.testDao.ConfigData",
            exception.message
        )
    }

    @Test
    fun `test optional value`() = runBlocking {
        val value = dao.getConfigNameOnlyAssumingValueIsOptional(config1.name)
            .single()
        assertEquals(config1.name, value.name)
        assertEquals("hello", value.value)
    }

    @Test
    fun `test optional value and expect exception`() {
        val exception = assertThrows<IllegalStateException> {
            runBlocking {
                dao.getConfigNameOnly(config1.name)
                    .single()
            }
        }
        assertEquals(
            "No value found for mandatory param: value in mappedType: com.udaan.r2dbi.testDao.ConfigData",
            exception.message
        )
    }

    @Test
    fun `test with diff property name or column name`() = runBlocking {
        val data1 = dao.getConfigByNameWithDifferentFieldNames(config1.name)
            .single()
        assertEquals(config1.name, data1.configName)
        assertEquals(config1.value, data1.configValue)

        val data2 = dao.getConfigByNameWithDifferentColumnNames(config1.name)
            .single()

        assertEquals(config1.name, data2.name)
        assertEquals(config1.value, data2.value)
    }

    @Test
    fun `test null return`() {
        val e = assertThrows<IllegalStateException> {
            runBlocking {
                dao.getNullReturn().single()
            }
        }

        assertEquals(
            "Null value found for returnType: com.udaan.r2dbi.testDao.ConfigData when mapping result in method: getNullReturn in clazz: com.udaan.r2dbi.testDao.TestQueryDao",
            e.message
        )
    }

    @Test
    fun `test with multiple placeholders with same name`() = runBlocking {
        val config = dao.getConfigWithSameNameAndValue(config6.name).single()
        assertEquals(config6.name, config.name)
        assertEquals(config6.name, config.value)
    }

    private fun transactionScenario() = r2dbi.execute { context ->
        val updateCountBeforeTxMono: Mono<Long> =
            context.createStatementContext("insert into $TableName values ('$successKeyBeforeTx', 'some value')")
                .executeUpdate()
                .reduce { acc, value -> acc + value }

        val txPublisher: Publisher<Long> = context.inTransaction(TransactionIsolationLevel.DEFAULT).use {
            // Inside Transaction
            val stmt = context.createStatementContext("insert into $TableName values ('$failureKey', 'some value')")
            val publisher = stmt.executeUpdate()
                .then<Long>(
                    Mono.error(IllegalStateException("Terminating Transaction"))
                )
            publisher
        }

        val updateCountAfterTxnMono: Mono<Long> =
            context.createStatementContext("insert into $TableName values ('$successKeyAfterTx', 'some value')")
                .executeUpdate()
                .reduce { acc, value -> acc + value }

        Mono.just(
            TransactionScenarioResult(
                updateCountBeforeTxMono,
                Mono.from(txPublisher),
                updateCountAfterTxnMono
            )
        )
    }
}

internal val config1 = ConfigData(CONFIG_NAME1, "some value1")
internal val config2 = ConfigData(CONFIG_NAME2, "some value2")
internal val config3 = ConfigData(CONFIG_NAME3, "some value3")
internal val config4 = ConfigData(CONFIG_NAME4, "some value4")
internal val config5 = ConfigData(CONFIG_NAME5, "some value5")
internal val config6 = ConfigData(CONFIG_NAME6, CONFIG_NAME6)

private const val successKeyBeforeTx = "success before Tx Key"
private const val successKeyAfterTx = "success after Tx Key"
private const val failureKey = "failure Key"

private data class TransactionScenarioResult(
    val beforeTx: Mono<Long>,
    val inTx: Mono<Long>,
    val afterTx: Mono<Long>
)

