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

package com.udaan.r2dbi.internal

import org.testcontainers.containers.JdbcDatabaseContainer

interface SqlDatabaseContainerProvider {
    fun newInstance(): SqlDatabaseContainer
}

interface SqlDatabaseContainer {

    fun start()

    fun stop()

    fun getHost(): String
    fun getUserName(): String
    fun getPassword(): String

    fun getPort(): Int
    fun getDatabaseName(): String

    fun getDriverName(): String
}

abstract class SqlDatabaseContainerAdapter(private val jdbcDatabaseContainer: JdbcDatabaseContainer<*>): SqlDatabaseContainer {

    init {
        jdbcDatabaseContainer.withStartupTimeoutSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS)
        jdbcDatabaseContainer.withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS)
    }

    override fun start() {
        jdbcDatabaseContainer.start()
    }

    override fun stop() {
        jdbcDatabaseContainer.stop()
    }

    override fun getHost(): String {
        return jdbcDatabaseContainer.getHost()
    }

    override fun getPort(): Int {
        return jdbcDatabaseContainer.getFirstMappedPort()
    }

    override fun getDatabaseName(): String {
        return jdbcDatabaseContainer.getDatabaseName()
    }

    override fun getUserName(): String {
        return jdbcDatabaseContainer.getUsername()
    }

    override fun getPassword(): String {
        return jdbcDatabaseContainer.getPassword()
    }

}

const val DEFAULT_STARTUP_TIMEOUT_SECONDS = 240
const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 240
