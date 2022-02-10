/*
 * Copyright (c) 2021 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.settings.application

import com.reposilite.settings.api.LocalConfiguration.StandardSQLDatabaseSettings
import com.reposilite.settings.api.LocalConfiguration.EmbeddedSQLDatabaseSettings
import com.reposilite.shared.extensions.loadCommandBasedConfiguration
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File
import java.nio.file.Path
import java.sql.Connection.TRANSACTION_SERIALIZABLE
import javax.sql.DataSource
import kotlin.io.path.absolutePathString

internal object DatabaseSourceFactory {

    fun createConnection(workingDirectory: Path, databaseConfiguration: String): Database =
        when {
            databaseConfiguration.startsWith("mysql") -> connectWithStandardDatabase(databaseConfiguration, "jdbc:mysql", "com.mysql.cj.jdbc.Driver")
            databaseConfiguration.startsWith("sqlite") -> connectWithEmbeddedDatabase(workingDirectory, databaseConfiguration, "org.sqlite.JDBC", "jdbc:sqlite:%file%")
            /* Experimental implementations (not covered with integration tests) */
            databaseConfiguration.startsWith("postgresql") -> connectWithStandardDatabase(databaseConfiguration, "jdbc:postgresql", "org.postgresql.Driver")
            databaseConfiguration.startsWith("h2") -> connectWithEmbeddedDatabase(workingDirectory, databaseConfiguration, "org.h2.Driver", "jdbc:h2:%file%")
            else -> throw RuntimeException("Unknown database: $databaseConfiguration")
        }

    private fun connectWithStandardDatabase(databaseConfiguration: String, dialect: String, driver: String): Database =
        with(loadCommandBasedConfiguration(StandardSQLDatabaseSettings(), databaseConfiguration).configuration) {
            Database.connect(createDataSource(driver, "$dialect://${host}/${database}", 2, user, password))
        }

    private fun connectWithEmbeddedDatabase(workingDirectory: Path, databaseConfiguration: String, driver: String, dialect: String): Database =
        with(loadCommandBasedConfiguration(EmbeddedSQLDatabaseSettings(), databaseConfiguration).configuration) {
            val databaseFile =
                if (temporary)
                    File.createTempFile("reposilite-database", ".db")
                        .also { it.deleteOnExit() }
                        .toPath()
                else
                    workingDirectory.resolve(fileName)

            Database.connect(createDataSource(driver, dialect.replace("%file%", databaseFile.absolutePathString()), 1)).also {
                TransactionManager.manager.defaultIsolationLevel = TRANSACTION_SERIALIZABLE
            }
        }

    private fun createDataSource(driver: String, url: String, threadPool: Int, username: String? = null, password: String? = null): DataSource =
        HikariDataSource(
            HikariConfig().apply {
                this.jdbcUrl = url
                this.driverClassName = driver
                this.maximumPoolSize = threadPool
                username?.also { this.username = it }
                password?.also { this.password = it }
            }
        )

}