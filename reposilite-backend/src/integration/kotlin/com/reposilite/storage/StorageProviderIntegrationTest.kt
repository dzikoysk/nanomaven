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

@file:Suppress("FunctionName")

package com.reposilite.storage

import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.FileType.FILE
import com.reposilite.storage.api.toLocation
import com.reposilite.storage.specification.StorageProviderSpecification
import io.javalin.http.ContentType.APPLICATION_JAR
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal abstract class StorageProviderIntegrationTest : StorageProviderSpecification() {

    @Test
    fun `should store and return valid resource` () {
        // given: a destination path to the resource and its content
        val resource = "/directory/file.data".toLocation()
        val content = "content".toByteArray()

        // when: resource is put in storage and then requested
        val putResponse = storageProvider.putFile(resource, content.inputStream())

        // then: put request should succeed
        assertOk(putResponse)
        assertTrue(storageProvider.exists(resource))

        // when: stored resource is requested
        val fetchResponse = storageProvider.getFile(resource)

        // then: provider should return proper resource
        assertOk(fetchResponse)
        assertArrayEquals(content, fetchResponse.get().readBytes())
    }

    @Test
    fun `should return error if non-existing resource has been requested` () {
        // given: some path to the resource that doesn't exist
        val resource = "/not/found.data".toLocation()

        // when: non-existing resource is requested
        val nonExistingResource = storageProvider.getFile(resource)

        // then: response should contain error
        assertError(nonExistingResource)
    }

    @Test
    fun `should return valid file details` () {
        // given: a destination path to the resource and its content
        val resource = "/directory/file.jar".toLocation()
        val content = "content".toByteArray()
        storageProvider.putFile(resource, content.inputStream())

        // when: file details are requested
        val response = storageProvider.getFileDetails(resource)

        // then: response should contain expected file details
        val fileDetails = assertOk(response) as DocumentInfo
        assertEquals(FILE, fileDetails.type)
        assertEquals(resource.getSimpleName(), fileDetails.name)
        assertEquals(content.size.toLong(), fileDetails.contentLength)
        assertEquals(APPLICATION_JAR, fileDetails.contentType)
    }

}