package com.reposilite.maven

import com.reposilite.journalist.Journalist
import com.reposilite.settings.SharedConfiguration.RepositoryConfiguration
import panda.std.reactive.Reference
import java.nio.file.Path

internal class RepositoryProvider(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    repositoriesSource: Reference<Map<String, RepositoryConfiguration>>,
) {

    private var repositories: Map<String, Repository>

    init {
        this.repositories = createRepositories(repositoriesSource.get())

        repositoriesSource.subscribe {
            repositories.forEach { (_, repository) -> repository.shutdown() }
            this.repositories = createRepositories(it)
        }
    }

    private fun createRepositories(repositoriesConfiguration: Map<String, RepositoryConfiguration>): Map<String, Repository> =
        RepositoryFactory(journalist, workingDirectory).let { factory ->
            repositoriesConfiguration.mapValues { (repositoryName, repositoryConfiguration) ->
                factory.createRepository(repositoryName, repositoryConfiguration)
            }
        }

    fun getRepositories(): Map<String, Repository> =
        repositories

}