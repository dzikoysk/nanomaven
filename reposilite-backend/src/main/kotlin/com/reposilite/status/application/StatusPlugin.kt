/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.status.application

import com.reposilite.VERSION
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.api.ReposiliteStartedEvent
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.parameters
import com.reposilite.plugin.reposilite
import com.reposilite.shared.extensions.TimeUtils
import com.reposilite.status.FailureFacade
import com.reposilite.status.StatusFacade
import com.reposilite.status.StatusSnapshotScheduler
import com.reposilite.status.infrastructure.StatusEndpoints
import com.reposilite.web.HttpServer
import com.reposilite.web.api.HttpServerStoppedEvent
import com.reposilite.web.api.RoutingSetupEvent
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import panda.std.reactive.Completable
import panda.std.reactive.Reference.Dependencies.dependencies
import panda.std.reactive.Reference.computed
import panda.utilities.console.Effect

@Plugin(name = "status", dependencies = ["failure", "local-configuration"])
internal class StatusPlugin : ReposilitePlugin() {

    private val remoteVersionEndpoint = "https://maven.reposilite.com/api/maven/latest/version/releases/com/reposilite/reposilite?type=raw"

    override fun initialize(): StatusFacade {
        val webServer = Completable<HttpServer>()
        val failureFacade = facade<FailureFacade>()
        val localConfiguration = facade<LocalConfiguration>()

        val statusFacade = StatusComponents(
            testEnv = parameters().testEnv,
            failureFacade = failureFacade,
            remoteVersionEndpoint = remoteVersionEndpoint,
            statusSupplier = { if (webServer.isReady) webServer.get().isAlive() else false },
            maxThreads = with (localConfiguration) {
                computed(dependencies(webThreadPool, ioThreadPool, databaseThreadPool)) {
                    webThreadPool.get() + ioThreadPool.get() + databaseThreadPool.get()
                }
            }
        ).statusFacade()

        val statusSnapshotScheduler = StatusSnapshotScheduler(reposilite().scheduler, statusFacade)
        statusSnapshotScheduler.start()

        event { _: ReposiliteDisposeEvent ->
            statusSnapshotScheduler.stop()
        }

        event { _: ReposiliteInitializeEvent ->
            webServer.complete(reposilite().webServer)
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(StatusEndpoints(statusFacade, failureFacade))
        }

        event { _: ReposiliteStartedEvent ->
            logger.info("")
            logger.info("${Effect.GREEN}Done (${TimeUtils.getPrettyUptimeInSeconds(statusFacade.getUptime())})!${Effect.RESET}")
            logger.info("")
            logger.info("${Effect.YELLOW_BOLD}$VERSION version has been sponsored by:${Effect.RESET}")
            logger.info("  ${Effect.BOLD}talismanplatform,${Effect.RESET}")
            logger.info("  ${Effect.BOLD}joshuasing, andrm, rdehuyss, amp7368, insertt, GotoFinal,${Effect.RESET}")
            logger.info("  ${Effect.BOLD}SirEndii, Koressi, tipsy, Kamilkime, that-apex,${Effect.RESET}")
            logger.info("  ${Effect.BOLD}crejk, Rollczi, Jan Bojarczuk, Petersoj${Effect.RESET}")
            logger.info("")
            logger.info("${Effect.RESET}")
        }

        event { _: ReposiliteStartedEvent ->
            val localTmpDataDirectory = parameters().workingDirectory.resolve(".local")
            Files.createDirectories(localTmpDataDirectory)

            Files.writeString(
                localTmpDataDirectory.resolve("reposilite.address"),
                "${parameters().hostname}:${parameters().port}",
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC
            )
        }

        event { _: HttpServerStoppedEvent ->
            logger.info("Bye! Uptime: " + TimeUtils.getPrettyUptime(statusFacade.getUptime()))
        }

        return statusFacade
    }



}
