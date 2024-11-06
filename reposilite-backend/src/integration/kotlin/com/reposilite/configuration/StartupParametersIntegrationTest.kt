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

@file:Suppress("FunctionName")

package com.reposilite.configuration

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.ReposiliteParameters
import com.reposilite.ReposiliteSpecification
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.frontend.application.FrontendSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class StartupParametersIntegrationTest : ReposiliteSpecification() {

    override fun overrideParameters(parameters: ReposiliteParameters) {
        // given: a custom shared configuration
        val sharedConfiguration = reposiliteWorkingDirectory.resolve("custom-shared-configuration.json")
        parameters.sharedConfigurationPath = sharedConfiguration.toPath()
        sharedConfiguration.writeText(
            """
            {
                "frontend": {
                    "id": "test-repository"
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun `should load custom shared configuration`() {
        // when: modified settings are requested
        val sharedConfigurationFacade = reposilite.extensions.facade<SharedConfigurationFacade>()
        val frontendSettings = sharedConfigurationFacade.getDomainSettings<FrontendSettings>()

        // then: Reposilite should use file-based shared configuration
        assertThat(frontendSettings.map { it.id }).isEqualTo("test-repository")
    }

}