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

package com.reposilite.web

import com.reposilite.Reposilite
import com.reposilite.web.application.ReposiliteRoutes
import io.javalin.Javalin

interface WebConfiguration {

    fun initialize(reposilite: Reposilite) { }

    fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> = setOf()

    // TOFIX: Remove dependency on infrastructure details
    fun javalin(reposilite: Reposilite, javalin: Javalin) { }

    fun dispose(reposilite: Reposilite) { }

}

fun <WEB : WebConfiguration, R> web(configurations: MutableCollection<WebConfiguration>, configuration: WEB, block: WEB.() -> R): R {
    configurations.add(configuration)
    return block(configuration)
}