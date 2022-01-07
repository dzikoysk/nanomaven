package com.reposilite.settings.application

import com.reposilite.ReposiliteParameters
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.application.SettingsPlugin.Companion.LOCAL_CONFIGURATION_FILE
import com.reposilite.settings.infrastructure.FileSystemConfigurationProvider

internal object LocalConfigurationFactory {

    fun createLocalConfiguration(parameters: ReposiliteParameters): LocalConfiguration =
        FileSystemConfigurationProvider(
            LOCAL_CONFIGURATION_FILE,
            "Local configuration",
            null,
            parameters.workingDirectory,
            parameters.localConfigurationMode,
            parameters.localConfigurationPath,
            LocalConfiguration()
        )
        .also { it.initialize() }
        .configuration

}