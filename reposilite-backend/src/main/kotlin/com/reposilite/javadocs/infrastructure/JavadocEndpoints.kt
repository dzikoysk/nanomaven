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

package com.reposilite.javadocs.infrastructure

import com.reposilite.javadocs.JavadocFacade
import com.reposilite.javadocs.api.JavadocPageRequest
import com.reposilite.maven.infrastructure.MavenRoutes
import com.reposilite.shared.extensions.encoding
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.routing.RouteMethod

internal class JavadocEndpoints(javadoc: JavadocFacade) : MavenRoutes(javadoc.mavenFacade) {

    private val javadocRoute = ReposiliteRoute<Any>("/javadoc/{repository}/<gav>", RouteMethod.GET) {
        accessed {
            requireGav { gav ->
                requireRepository { repository ->
                    response = JavadocPageRequest(this?.identifier, repository, gav)
                        .let { javadoc.findJavadocPage(it) }
                        .peek { ctx.encoding(Charsets.UTF_8).contentType(it.contentType) }
                        .map { it.content }
                }
            }
        }
    }

    override val routes = routes(javadocRoute)

}