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

import com.udaan.r2dbi.R2DbiTestExtensionBase

class SqlDatabaseExtension(provider: SqlDatabaseContainerProvider) : R2DbiTestExtensionBase() {
    private val container = provider.newInstance()

    override fun beforeAll() {
        container.start()
        super.beforeAll()
    }

    override fun afterAll() {
        super.afterAll()
        container.stop()
    }

    override fun getHost(): String = container.getHost()
    override fun getPassword(): String = container.getPassword()
    override fun getPort(): Int = container.getPort() //getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT)
    override fun getUsername(): String = container.getUserName()

    override fun getDriver(): String = container.getDriverName()
    override fun getDatabaseName(): String = container.getDatabaseName()
}
