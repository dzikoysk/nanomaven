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

package com.reposilite.plugin

import com.reposilite.plugin.api.ReposilitePlugin
import panda.std.Result.supplyThrowing
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.ServiceLoader
import java.util.jar.JarFile
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
import kotlin.io.path.useDirectoryEntries

class PluginLoader(
    val pluginsDirectory: Path,
    val extensions: Extensions
) {

    fun initialize() {
        extensions.getPlugins()
            .values
            .forEach { (_, plugin) -> plugin.load(this) }

        val plugins = sortPlugins()

        plugins.chunked(5).forEach {
            extensions.logger.info(it.joinToString(", ", transform = { (metadata, _) -> metadata.name }))
        }

        plugins.forEach { (_, plugin) ->
            plugin.initialize()?.apply { extensions.registerFacade(this) }
            System.gc() // startup is heavy & reflective operation, so we'd like to tell jvm to take a look on that
        }
    }

    private fun sortPlugins(): List<PluginEntry> =
        with (extensions.getPlugins()) {
            extensions.getPlugins().asSequence()
                .map { it.value.metadata }
                .sortedBy { it.name }
                .associateBy({ it.name }, { it.dependencies.toList() })
                .let { toFlattenedDependencyGraph(it) }
                .mapNotNull { this[it] }
        }

    internal fun loadPluginsByServiceFiles() {
        if (pluginsDirectory.notExists()) {
            pluginsDirectory.createDirectories()
        }

        check(pluginsDirectory.isDirectory()) { "The path is not a directory" }
        extensions.logger.debug("Plugins directory: ${pluginsDirectory.absolute()}")

        pluginsDirectory.useDirectoryEntries { pluginDirectoryStream ->
            pluginDirectoryStream.filter { it.extension == ".jar" }
                .filter { isValidJarFile(it.toFile()) }
                .map { it.toUri().toURL() }
                .toList()
                .onEach { extensions.logger.debug("Plugin file: $it") }
                .let { URLClassLoader(it.toTypedArray()) }
                .let { ServiceLoader.load(ReposilitePlugin::class.java, it) }
                .onEach { extensions.logger.debug("Plugin class: $it") }
                .forEach { extensions.registerPlugin(it) }
        }
    }

    private fun isValidJarFile(file: File): Boolean =
        supplyThrowing { JarFile(file) }
            .map { it.close() }
            .onError { extensions.logger.warn("Invalid JAR file: ${file.absolutePath}") }
            .isOk

}
