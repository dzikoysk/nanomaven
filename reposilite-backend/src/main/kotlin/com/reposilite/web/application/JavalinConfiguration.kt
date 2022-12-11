/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.web.application

import com.reposilite.Reposilite
import com.reposilite.ReposiliteObjectMapper
import com.reposilite.VERSION
import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.api.Credentials
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.journalist.Journalist
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.extensions.response
import com.reposilite.shared.extensions.uri
import com.reposilite.shared.extractFromHeaders
import com.reposilite.status.FailureFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.web.api.HttpServerConfigurationEvent
import com.reposilite.web.api.HttpServerStarted
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.RoutingSetupEvent
import com.reposilite.web.infrastructure.CacheBypassHandler
import com.reposilite.web.infrastructure.EndpointAccessLoggingHandler
import com.reposilite.web.routing.RoutingPlugin
import io.javalin.community.ssl.SSLPlugin
import io.javalin.config.JavalinConfig
import io.javalin.json.JavalinJackson
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.plugin.OpenApiConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.thread.ThreadPool
import panda.std.reactive.Reference
import java.util.function.Consumer

internal object JavalinConfiguration {

    internal fun configure(reposilite: Reposilite, webThreadPool: ThreadPool, config: JavalinConfig) {
        val server = Server(webThreadPool)
        config.jetty.server { server }
        reposilite.extensions.emitEvent(HttpServerConfigurationEvent(reposilite, config))

        val localConfiguration = reposilite.extensions.facade<LocalConfiguration>()
        val sharedConfigurationFacade = reposilite.extensions.facade<SharedConfigurationFacade>()
        val webSettings = sharedConfigurationFacade.getDomainSettings<WebSettings>()
        val frontendSettings = sharedConfigurationFacade.getDomainSettings<FrontendSettings>()

        reposilite.extensions.registerEvent { _: HttpServerStarted ->
            server.connectors
                .filterIsInstance<ServerConnector>()
                .forEach { it.idleTimeout = localConfiguration.idleTimeout.get() }
        }

        if (localConfiguration.bypassExternalCache.get()) {
            reposilite.extensions.registerEvent { event: RoutingSetupEvent ->
                event.registerRoutes(EndpointAccessLoggingHandler())
                event.registerRoutes(CacheBypassHandler())
                reposilite.logger.debug("CacheBypassHandler has been registered")
            }
        }

        configureJavalin(config, localConfiguration, webSettings)
        configureJsonSerialization(config)
        configureSSL(reposilite, localConfiguration, config, server)
        configureCors(config)
        configureOpenApi(config, frontendSettings.get())
        configureDebug(reposilite, localConfiguration, config)
        configureReactiveRoutingPlugin(config, reposilite)
    }

    private fun configureJavalin(config: JavalinConfig, localConfiguration: LocalConfiguration, webSettings: Reference<WebSettings>) {
        config.showJavalinBanner = false
        config.http.asyncTimeout = 1000L * 60 * 60 * 10 // 10min
        config.contextResolver.ip = { ctx -> ctx.header(webSettings.get().forwardedIp) ?: ctx.req().remoteAddr }

        when (localConfiguration.compressionStrategy.get().lowercase()) {
            "none" -> config.compression.none()
            "gzip" -> config.compression.gzipOnly()
            "brotli" -> config.compression.brotliOnly()
            else -> error("Unknown compression strategy ${localConfiguration.compressionStrategy.get()}")
        }
    }

    private fun configureReactiveRoutingPlugin(config: JavalinConfig, reposilite: Reposilite) {
        val extensionManager = reposilite.extensions
        val failureFacade = extensionManager.facade<FailureFacade>()
        val accessTokenFacade = extensionManager.facade<AccessTokenFacade>()
        val authenticationFacade = extensionManager.facade<AuthenticationFacade>()

        val plugin = RoutingPlugin<ReposiliteRoute<Any>>(
            handler = { ctx, route ->
                try {
                    val dsl = ContextDsl<Any>(
                        reposilite.logger,
                        ctx,
                        accessTokenFacade,
                        lazy {
                            extractFromHeaders(ctx.headerMap())
                                .map { (name, secret) -> Credentials(name, secret) }
                                .flatMap { authenticationFacade.authenticateByCredentials(it) }
                        }
                    )
                    route.handler(dsl)
                    dsl.response?.also { result ->
                        result.onError { reposilite.logger.debug("ERR RESULT | ${ctx.method()} ${ctx.uri()} errored with $it") }
                        ctx.response(result)
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                    failureFacade.throwException(ctx.uri(), throwable)
                }
            }
        )

        extensionManager.emitEvent(RoutingSetupEvent(reposilite))
            .getRoutes()
            .asSequence()
            .flatMap { it.routes }
            .distinctBy { it.methods.joinToString(";") + ":" + it.path }
            .toSet()
            .let { plugin.registerRoutes(it) }

        config.plugins.register(plugin)
    }

    private fun configureJsonSerialization(config: JavalinConfig) {
        val objectMapper = ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
        config.jsonMapper(JavalinJackson(objectMapper))
    }

    private fun configureSSL(reposilite: Reposilite, localConfiguration: LocalConfiguration, config: JavalinConfig, server: Server) {
        if (localConfiguration.sslEnabled.get()) {
            reposilite.logger.info("Enabling SSL connector at ::" + localConfiguration.sslPort.get())

            val sslPlugin = SSLPlugin {
                it.insecure = true
                it.insecurePort = localConfiguration.port.get()

                it.secure = true
                it.securePort = localConfiguration.sslPort.get()

                val keyPath = localConfiguration.keyPath.get().replace("\${WORKING_DIRECTORY}", reposilite.parameters.workingDirectory.toAbsolutePath().toString())
                val keyPassword = localConfiguration.keyPassword.get()

                when {
                    keyPath.endsWith(".pem") -> {
                        val (pemKeyPath, certPath) = keyPath.split(" ")
                        it.pemFromPath(pemKeyPath, certPath, keyPassword)
                    }
                    keyPath.endsWith(".jks") -> it.keystoreFromPath(keyPath, keyPassword)
                    else -> throw IllegalArgumentException("Provided key extension is not supported.")
                }

                it.configConnectors = Consumer { connector -> connector.idleTimeout = localConfiguration.idleTimeout.get() }
                it.sniHostCheck = false
            }

            config.plugins.register(sslPlugin)
        }

        if (localConfiguration.enforceSsl.get()) {
            config.plugins.enableSslRedirects()
        }
    }

    private fun configureCors(config: JavalinConfig) {
        config.plugins.enableCors {
            it.add { cfg ->
                cfg.anyHost()
            }
        }
    }

    private fun configureOpenApi(config: JavalinConfig, frontendSettings: FrontendSettings) {
        val openApiConfiguration = OpenApiConfiguration()

        openApiConfiguration.info = OpenApiInfo().also {
            it.title = frontendSettings.title
            it.description = frontendSettings.description
            it.version = VERSION
        }

        config.plugins.register(OpenApiPlugin(openApiConfiguration))
    }

    private fun configureDebug(journalist: Journalist, localConfiguration: LocalConfiguration, config: JavalinConfig) {
        if (localConfiguration.debugEnabled.get()) {
            // config.requestCacheSize = FilesUtils.displaySizeToBytesCount(System.getProperty("reposilite.requestCacheSize", "8MB"));
            // Reposilite.getLogger().debug("requestCacheSize set to " + config.requestCacheSize + " bytes");
            journalist.logger.info("Debug enabled")
            config.plugins.enableDevLogging()
        }
    }

}
