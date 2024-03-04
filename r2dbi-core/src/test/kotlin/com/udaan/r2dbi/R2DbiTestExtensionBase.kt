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

import com.udaan.r2dbi.testDao.TableName
import io.r2dbc.mssql.MssqlConnectionFactoryProvider
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import java.time.Duration

abstract class R2DbiTestExtensionBase {
    private lateinit var pool: ConnectionPool
    private lateinit var r2Dbi: R2Dbi

    open fun beforeAll() {
        pool = createPool()
        r2Dbi = R2Dbi.withPool(pool)

        createTable()
    }

    open fun afterAll() {
        dropTable()

        pool.close().block()
    }

    private fun createTable() {
        println("Creating table - $TableName")
        getR2dbi().execute {
            val stmtContext =
                it.createStatementContext("CREATE TABLE $TableName ( name VARCHAR(128), value VARCHAR(256))")
            stmtContext.executeUpdate()
                .then()
        }.blockLast()
    }

    private fun dropTable() {
        println("Dropping table - $TableName")
        getR2dbi().execute {
            val stmtContext = it.createStatementContext("DROP TABLE $TableName")
            stmtContext.executeUpdate()
                .then()
        }.blockLast()
    }

    private fun createPool(): ConnectionPool {
        val options: ConnectionFactoryOptions = ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, getDriver())
            .option(ConnectionFactoryOptions.HOST, getHost())
            .option(ConnectionFactoryOptions.PORT, getPort())
            .option(ConnectionFactoryOptions.USER, getUsername())
            .option(ConnectionFactoryOptions.PASSWORD, getPassword())
            .option(ConnectionFactoryOptions.DATABASE, getDatabaseName()) // optional
            .option(ConnectionFactoryOptions.SSL, false) // optional, defaults to false
            .option(MssqlConnectionFactoryProvider.TRUST_SERVER_CERTIFICATE, true)

            .build()

        val connectionFactory: ConnectionFactory = ConnectionFactories.get(options)
        // Create a ConnectionPool for connectionFactory
        val configuration: ConnectionPoolConfiguration = ConnectionPoolConfiguration.builder(connectionFactory)
            .maxIdleTime(Duration.ofMillis(1000))
            .maxSize(1)
            .build()

        return ConnectionPool(configuration)
    }

    fun getR2dbi(): R2Dbi {
        return r2Dbi
    }

    fun getPool(): ConnectionPool {
        return pool
    }

    protected abstract fun getHost(): String
    protected abstract fun getPassword(): String
    protected abstract fun getPort(): Int
    protected abstract fun getUsername(): String

    protected abstract fun getDatabaseName(): String

    protected abstract fun getDriver(): String

}
