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

import com.udaan.r2dbi.internal.AzureSQLEdgeServerContainer.Companion.MS_SQL_SERVER_PORT
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.LicenseAcceptance
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Stream

/**
 * Uses a [MSSQLServerContainer] with a `mcr.microsoft.com/azure-sql-edge` image (default: "latest") in place of
 * the standard ` mcr.microsoft.com/mssql/server` image
 */
class AzureSqlEdgeContainerProvider : SqlDatabaseContainerProvider {
//    override fun supports(databaseType: String): Boolean {
//        return databaseType == NAME
//    }

    override fun newInstance(): SqlDatabaseContainer {
        return newInstance("latest")
    }

    private fun newInstance(tag: String): SqlDatabaseContainer {
        val taggedImageName: DockerImageName = DockerImageName.parse("mcr.microsoft.com/azure-sql-edge")
            .withTag(tag)
            .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server")
        val containerInstance = AzureSQLEdgeServerContainer(taggedImageName)
            .withUrlParam("trustServerCertificate", "true")

        return object: SqlDatabaseContainerAdapter(containerInstance) {
            override fun getDriverName(): String {
                return containerInstance.getDriverName()
            }

            override fun getPort(): Int {
                return containerInstance.getMappedPort(MS_SQL_SERVER_PORT)
            }
        }
    }

    companion object {
        private const val NAME = "azuresqledge"
    }
}

/**
 * Testcontainers implementation for Microsoft SQL Server.
 *
 *
 * Supported image: `mcr.microsoft.com/mssql/server`
 *
 *
 * Exposed ports: 1433
 */
open class AzureSQLEdgeServerContainer(dockerImageName: DockerImageName) : JdbcDatabaseContainer<AzureSQLEdgeServerContainer>(dockerImageName) {
    private var password: String = DEFAULT_PASSWORD

    init {
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME)
        addExposedPort(MS_SQL_SERVER_PORT)
    }

    override fun configure() {
        // If license was not accepted programatically, check if it was accepted via resource file
        if (!envMap.containsKey("ACCEPT_EULA")) {
            LicenseAcceptance.assertLicenseAccepted(this.dockerImageName)
            acceptLicense()
        }
        addEnv("MSSQL_SA_PASSWORD", password)
        addEnv("MSSQL_PID", "Developer")
        addEnv("MSSQL_USER", username)
    }

    /**
     * Accepts the license for the SQLServer container by setting the ACCEPT_EULA=Y
     * variable as described at [https://hub.docker.com/_/microsoft-mssql-server](https://hub.docker.com/_/microsoft-mssql-server)
     */
    open fun acceptLicense(): AzureSQLEdgeServerContainer? {
        addEnv("ACCEPT_EULA", "1")
        return self()
    }

    override fun getDriverClassName(): String {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    }

    override fun constructUrlForConnection(queryString: String): String {
        // The JDBC driver of MS SQL Server enables encryption by default for versions > 10.1.0.
        // We need to disable it by default to be able to use the container without having to pass extra params.
        // See https://github.com/microsoft/mssql-jdbc/releases/tag/v10.1.0
        if (urlParameters.keys.stream().map { obj: String ->
                obj.lowercase(
                    Locale.getDefault()
                )
            }.noneMatch { anObject: String? -> "encrypt".equals(anObject) }) {
            urlParameters["encrypt"] = "false"
        }
        return super.constructUrlForConnection(queryString)
    }

    override fun getJdbcUrl(): String {
        val additionalUrlParams = constructUrlParameters(";", ";")
        return "jdbc:sqlserver://" + host + ":" + getMappedPort(MS_SQL_SERVER_PORT) + additionalUrlParams
    }

    override fun getUsername(): String {
        return DEFAULT_USER
    }

    override fun getPassword(): String {
        return password
    }

    override fun getTestQueryString(): String {
        return "SELECT 1"
    }

    override fun getDatabaseName(): String {
        return "master"
    }

    fun getDriverName(): String {
        return "sqlserver"
    }

    override fun withPassword(password: String): AzureSQLEdgeServerContainer? {
        checkPasswordStrength(password)
        this.password = password
        return self()
    }

    private fun checkPasswordStrength(password: String?) {
        requireNotNull(password) { "Null password is not allowed" }
        require(password.length >= 8) { "Password should be at least 8 characters long" }
        require(password.length <= 128) { "Password can be up to 128 characters long" }
        val satisfiedCategories = Stream
            .of<Pattern>(*PASSWORD_CATEGORY_VALIDATION_PATTERNS)
            .filter { p: Pattern ->
                p.matcher(password).find()
            }
            .count()
        require(satisfiedCategories >= 3) {
            """Password must contain characters from three of the following four categories:
 - Latin uppercase letters (A through Z)
 - Latin lowercase letters (a through z)
 - Base 10 digits (0 through 9)
 - Non-alphanumeric characters such as: exclamation point (!), dollar sign ($), number sign (#), or percent (%)."""
        }
    }

    companion object {
        private val DEFAULT_IMAGE_NAME = DockerImageName.parse("mcr.microsoft.com/azure-sql-edge")
        const val NAME = "sqlserver"
        val IMAGE: String = DEFAULT_IMAGE_NAME.getUnversionedPart()
        const val MS_SQL_SERVER_PORT = 1433
        const val DEFAULT_USER = "sa"
        const val DEFAULT_PASSWORD = "A_Str0ng_Required_Password"

        private val PASSWORD_CATEGORY_VALIDATION_PATTERNS = arrayOf(
            Pattern.compile("[A-Z]+"),
            Pattern.compile("[a-z]+"),
            Pattern.compile("[0-9]+"),
            Pattern.compile("[^a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE)
        )
    }
}
