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

plugins {
    kotlin("jvm") version "1.5.30"
    `maven-publish`
}

allprojects {
    group = "org.panda-lang"
    version = "3.0.0-alpha.1"

    apply(plugin = "maven-publish")

    publishing {
        repositories {
            maven {
                credentials {
                    username = property("mavenUser") as String
                    password = property("mavenPassword") as String
                }
                name = "panda-repository"
                url = uri("https://repo.panda-lang.org/releases")
            }
        }
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.panda-lang.org/releases") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases/") }
        maven { url = uri("https://jitpack.io") }
    }
}