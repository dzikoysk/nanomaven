package com.reposilite.plugin.swagger

import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.settings.SettingsFacade
import com.reposilite.web.api.HttpServerConfigurationEvent
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin as SwaggerPluginForJavalin

@Plugin(name = "swagger", dependencies = ["settings"])
class SwaggerPlugin : ReposilitePlugin() {

    override fun initialize(): Facade? {
        event { event: HttpServerConfigurationEvent ->
            val swaggerConfiguration = SwaggerConfiguration()
            swaggerConfiguration.title = facade<SettingsFacade>().sharedConfiguration.title.get()
            event.javalinConfig.registerPlugin(SwaggerPluginForJavalin(swaggerConfiguration))
        }

        return null
    }

}