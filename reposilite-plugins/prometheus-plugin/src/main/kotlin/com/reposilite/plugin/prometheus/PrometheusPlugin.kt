package com.reposilite.plugin.prometheus

import com.reposilite.maven.api.DeployEvent
import com.reposilite.maven.api.ResolvedFileEvent
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.prometheus.metrics.JettyMetrics
import com.reposilite.plugin.prometheus.metrics.QueuedThreadPoolMetrics
import com.reposilite.plugin.prometheus.metrics.ReposiliteMetrics
import com.reposilite.status.FailureFacade
import com.reposilite.status.StatusFacade
import com.reposilite.storage.api.toLocation
import com.reposilite.web.api.HttpServerConfigurationEvent
import com.reposilite.web.api.HttpServerStartedEvent
import com.reposilite.web.api.RoutingSetupEvent
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit


@Plugin(name = "prometheus", dependencies = ["failure", "statistics", "status"])
class PrometheusPlugin : ReposilitePlugin() {

    override fun initialize(): PrometheusFacade {
        logger.info("")
        logger.info("--- Prometheus")

        val prometheusPath = (System.getProperty("reposilite.prometheus.path")
            ?: System.getenv("REPOSILITE_PROMETHEUS_PATH")
            ?: "/metrics").toLocation()

        val prometheusUser = System.getProperty("reposilite.prometheus.user")
            ?: System.getenv("REPOSILITE_PROMETHEUS_USER")
            ?: throw IllegalStateException("Prometheus user is not defined")

        val prometheusPassword = System.getProperty("reposilite.prometheus.password")
            ?: System.getenv("REPOSILITE_PROMETHEUS_PASSWORD")
            ?: throw IllegalStateException("Prometheus password is not defined")

        val prometheusFacade = PrometheusFacade(
            failureFacade = facade<FailureFacade>(),
            prometheusUser = prometheusUser,
            prometheusPassword = prometheusPassword
        )

        JvmMetrics.builder().register()
        logger.info("Prometheus | JVM metrics has been initialized.")

        ReposiliteMetrics.register(facade<StatusFacade>(), facade<FailureFacade>())

        event { event: ResolvedFileEvent ->
            event.result.map { (info, _) ->
                ReposiliteMetrics.responseFileSizeSummary.observe(info.contentLength.toDouble())
                ReposiliteMetrics.resolvedFileCounter.inc()
            }
        }

        event { _: DeployEvent ->
            ReposiliteMetrics.mavenDeployCounter.inc()
        }

        logger.info("Prometheus | Reposilite metrics has been initialized")


        event { event: HttpServerConfigurationEvent ->
            event.config.router.mount {
                it.before { ctx ->
                    ctx.attribute("timestamp", System.currentTimeMillis())
                }

                it.after { ctx ->
                    if (ctx.path().toLocation() == prometheusPath)
                        return@after

                    ReposiliteMetrics.responseCounter.labelValues(ctx.statusCode().toString()).inc()

                    val currentTime = System.currentTimeMillis()
                    val timestamp = ctx.attribute<Long>("timestamp")

                    if (timestamp != null)
                        JettyMetrics.responseTimeSummary.labelValues(ctx.statusCode().toString())
                            .observe((currentTime - timestamp).milliseconds.toDouble(DurationUnit.SECONDS))
                }
            }

            event.config.jetty.modifyServer { server ->
                val handler = StatisticsHandler()
                server.insertHandler(handler)
                JettyMetrics.register(handler)

                when (val threadPool = server.threadPool) {
                    is QueuedThreadPool -> {
                        QueuedThreadPoolMetrics.register(threadPool)
                        logger.info("Prometheus | Queued Thread Pool metrics has been initialized.")
                    }

                    else -> logger.warn("Prometheus | Unsupported thread pool for metrics: ${threadPool.javaClass.name}, ignoring")
                }

                // yes, I need to nest events (I need access to the server and I can't get that from HttpServerStartedEvent)
                event<HttpServerStartedEvent> { _ ->
                    for (connector in server.connectors) {
                        connector.addBean(JettyMetrics)
                    }

                    logger.info("Prometheus | Jetty metrics has been initialized.")
                }
            }
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(
                PrometheusEndpoints(
                    prometheusFacade = prometheusFacade,
                    prometheusPath = prometheusPath.toString()
                )
            )
        }

        return prometheusFacade
    }
}
