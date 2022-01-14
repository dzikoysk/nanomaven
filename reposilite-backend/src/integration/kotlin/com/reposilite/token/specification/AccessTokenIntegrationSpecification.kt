package com.reposilite.token.specification

import com.reposilite.ReposiliteSpecification
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.api.CreateAccessTokenRequest

internal abstract class AccessTokenIntegrationSpecification : ReposiliteSpecification() {

    protected fun useToken(name: String, secret: String) =
        Pair(
            useFacade<AccessTokenFacade>().createAccessToken(CreateAccessTokenRequest(PERSISTENT, name, secret)).accessToken,
            secret
        )

    protected fun useTokenDescription(name: String, secret: String, permissions: Set<AccessTokenPermission> = emptySet()) =
        Triple(name, secret, permissions)

    protected fun getPermissions(identifier: AccessTokenIdentifier): Set<AccessTokenPermission> =
        useFacade<AccessTokenFacade>().getPermissions(identifier)

}