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

package com.reposilite.auth.specification

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.BasicAuthenticator
import com.reposilite.auth.LdapAuthenticator
import com.reposilite.auth.application.LdapSettings
import com.reposilite.status.FailureFacade
import com.reposilite.token.specification.AccessTokenSpecification
import org.junit.jupiter.api.BeforeEach
import panda.std.reactive.toMutableReference

internal abstract class AuthenticationSpecification : AccessTokenSpecification() {

    protected val failureFacade = FailureFacade(logger)
    protected val ldapConfiguration = LdapSettings().toMutableReference()

    protected lateinit var authenticationFacade: AuthenticationFacade

    @BeforeEach
    fun createAuthenticationFacade() {
        this.authenticationFacade = AuthenticationFacade(
            journalist = logger,
            authenticators = listOf(
                BasicAuthenticator(accessTokenFacade),
                LdapAuthenticator(
                    journalist = logger,
                    ldapSettings = ldapConfiguration,
                    accessTokenFacade = accessTokenFacade,
                    failureFacade = failureFacade
                )
            ),
            accessTokenFacade = accessTokenFacade
        )
    }

}
