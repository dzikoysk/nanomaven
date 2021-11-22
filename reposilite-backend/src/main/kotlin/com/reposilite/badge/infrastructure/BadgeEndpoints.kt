package com.reposilite.badge.infrastructure

import com.reposilite.badge.BadgeFacade
import com.reposilite.badge.api.LatestBadgeRequest
import com.reposilite.web.application.ReposiliteRoute
import com.reposilite.web.application.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiParam

internal class BadgeEndpoints(badgeFacade: BadgeFacade) : ReposiliteRoutes() {

    @OpenApi(
        path = "/api/badge/latest/{repository}/{gav}",
        tags = ["badge"],
        pathParams = [
            OpenApiParam(name = "repository", description = "Artifact's repository", required = true),
            OpenApiParam(name = "gav", description = "Artifacts' GAV", required = true)
        ],
        methods = [HttpMethod.GET]
    )
    val latestBadge = ReposiliteRoute("/api/badge/latest/{repository}/<gav>", GET) {
        response = LatestBadgeRequest(
                repository = requiredParameter("repository"),
                gav = requiredParameter("gav"),
                name = ctx.queryParam("name"),
                color = ctx.queryParam("color"),
                prefix = ctx.queryParam("prefix")
            )
            .let { badgeFacade.findLatestBadge(it) }
            .peek { ctx.contentType("image/svg+xml") }
    }

    override val routes = setOf(latestBadge)

}