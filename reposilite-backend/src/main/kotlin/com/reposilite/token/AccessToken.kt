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
package com.reposilite.token

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.token.application.AccessTokenPlugin.Companion.MAX_TOKEN_NAME
import io.javalin.openapi.OpenApiIgnore
import net.dzikoysk.exposed.shared.IdentifiableEntity
import net.dzikoysk.exposed.shared.UNINITIALIZED_ENTITY_ID
import java.time.LocalDate

enum class AccessTokenType {
    PERSISTENT,
    TEMPORARY
}

typealias AccessTokenId = Int

internal data class AccessToken(
    override val id: AccessTokenId = UNINITIALIZED_ENTITY_ID,
    val type: AccessTokenType = PERSISTENT,
    val name: String,
    @Transient @JsonIgnore @get:OpenApiIgnore
    val encryptedSecret: String = "",
    val createdAt: LocalDate = LocalDate.now(),
    val description: String = "",
) : IdentifiableEntity {

    init {
        if (name.length > MAX_TOKEN_NAME) {
            throw IllegalStateException("Name is too long (${name.length} > $MAX_TOKEN_NAME)")
        }
    }

    fun toDto(): AccessTokenDto =
        AccessTokenDto(
            id = id,
            type = type,
            name = name,
            createdAt = createdAt,
            description = description
        )

}

data class AccessTokenPermissions(
    val permissions: Set<AccessTokenPermission> = emptySet()
) {

    fun hasPermission(permission: AccessTokenPermission): Boolean =
        permissions.contains(permission)

}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class AccessTokenPermission(val identifier: String, val shortcut: String) {

    MANAGER("access-token:manager", "m");

    companion object {

        fun findAccessTokenPermissionByIdentifier(identifier: String) =
            values().firstOrNull { it.identifier == identifier }

        fun findAccessTokenPermissionByShortcut(shortcut: String) =
            values().firstOrNull { it.shortcut == shortcut }

        fun findByAll(permission: String) =
            findAccessTokenPermissionByIdentifier(permission) ?: findAccessTokenPermissionByShortcut(permission)

        @JsonCreator
        @JvmStatic
        fun fromObject(data: Map<String, String>): AccessTokenPermission =
            findAccessTokenPermissionByIdentifier(data["identifier"]!!)!!

    }

}


