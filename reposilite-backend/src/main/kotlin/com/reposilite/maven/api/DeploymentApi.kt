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

package com.reposilite.maven.api

import com.reposilite.maven.Repository
import com.reposilite.plugin.api.Event
import com.reposilite.storage.api.Location
import java.io.InputStream

data class DeployRequest(
    val repository: String,
    val gav: Location,
    val by: String,
    val content: InputStream
)

/**
 * Called when deployed file has been successfully stored in repository
 */
class DeployEvent(
    val repository: Repository,
    val gav: Location,
    val by: String
) : Event