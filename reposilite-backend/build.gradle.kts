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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    application
    jacoco
}

application {
    mainClass.set("com.reposilite.ReposiliteLauncherKt")
}

publishing {
    publications {
        create<MavenPublication>("library") {
            groupId = "org.panda-lang"
            artifactId = "reposilite"
            shadow.component(this)
            from(components.getByName("java"))
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.30")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.30")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    val expressible = "1.0.16"
    implementation("org.panda-lang:expressible:$expressible")
    implementation("org.panda-lang:expressible-kt:$expressible")
    implementation("org.panda-lang:expressible-kt-coroutines:$expressible")
    testImplementation("org.panda-lang:expressible-junit:$expressible")

    val awssdk = "2.17.44"
    implementation("software.amazon.awssdk:bom:$awssdk")
    implementation("software.amazon.awssdk:s3:$awssdk")

    val exposed = "0.35.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposed")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed")

    implementation("net.dzikoysk:exposed-upsert:1.0.3")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("mysql:mysql-connector-java:8.0.26")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.4")

    val fuel = "2.3.1"
    implementation("com.github.kittinunf.fuel:fuel:$fuel")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:$fuel")

    val openapi = "1.1.0"
    kapt("io.javalin-rfc:openapi-annotation-processor:$openapi")
    implementation("io.javalin-rfc:javalin-openapi-plugin:$openapi")
    implementation("io.javalin-rfc:javalin-swagger-plugin:$openapi")

    val javalinRfcs = "4.0.21"
    implementation("com.reposilite.javalin-rfcs:javalin-context:$javalinRfcs")
    implementation("com.reposilite.javalin-rfcs:javalin-reactive-routing:$javalinRfcs")

    // val javalin = "4.0.0.RC3"
    // implementation("io.javalin:javalin:$javalin")
    // implementation("com.github.tipsy.javalin:javalin:-SNAPSHOT")
    implementation("com.github.dzikoysk.javalin:javalin:90381e8e35")

    @Suppress("GradlePackageUpdate")
    implementation("org.eclipse.jetty:jetty-server:9.4.43.v20210629")

    val picocli = "4.6.1"
    kapt("info.picocli:picocli-codegen:$picocli")
    implementation("info.picocli:picocli:$picocli")

    val jackson = "2.12.5"
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jackson")

    val journalist = "1.0.9"
    implementation("com.reposilite:journalist:$journalist")
    implementation("com.reposilite:journalist-slf4j:$journalist")
    implementation("com.reposilite:journalist-tinylog:$journalist")

    val tinylog = "2.3.2"
    implementation("org.tinylog:slf4j-tinylog:$tinylog")
    implementation("org.tinylog:tinylog-api:$tinylog")
    implementation("org.tinylog:tinylog-impl:$tinylog")

    implementation("net.dzikoysk:cdn:1.9.1")
    implementation("com.google.http-client:google-http-client:1.40.0")
    implementation("org.springframework.security:spring-security-crypto:5.5.2")
    implementation("commons-io:commons-io:20030203.000550")

    implementation("org.jline:jline:3.20.0")
    implementation("org.fusesource.jansi:jansi:2.3.4")

    /* Tests */

    val unirest = "3.13.0"
    testImplementation("com.konghq:unirest-java:$unirest")
    testImplementation("com.konghq:unirest-objectmapper-jackson:$unirest")

    val testcontainers = "1.16.0"
    testImplementation("org.testcontainers:mariadb:$testcontainers")
    testImplementation("org.testcontainers:testcontainers:$testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainers")
    testImplementation("org.testcontainers:localstack:$testcontainers")

    testImplementation("com.amazonaws:aws-java-sdk-s3:1.12.72")
    testImplementation("com.google.http-client:google-http-client-jackson2:1.40.0")

    val junit = "5.8.1"
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
}

tasks.withType<ShadowJar> {
    archiveFileName.set("reposilite-${archiveVersion.get()}.jar")
    mergeServiceFiles()
    minimize {
        exclude(dependency("org.eclipse.jetty:.*"))
        exclude(dependency("org.eclipse.jetty.websocket:.*"))
        exclude(dependency("com.fasterxml.woodstox:woodstox-core:.*"))
        exclude(dependency("commons-logging:commons-logging:.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
        exclude(dependency("org.jetbrains.exposed:.*"))
        exclude(dependency("org.xerial:sqlite-jdbc.*"))
        exclude(dependency("org.sqlite:.*"))
        exclude(dependency("mysql:.*"))
        exclude(dependency("org.h2:.*"))
        exclude(dependency("com.h2database:.*"))
        exclude(dependency("org.tinylog:.*"))
        exclude(dependency("org.slf4j:.*"))
        exclude(dependency("software.amazon.awssdk:.*"))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

kapt {
    arguments {
        arg("project", "${project.group}/${project.name}") // picocli requirement
    }
}

jacoco {
    toolVersion = "0.8.7"
}

tasks.withType<Test> {
    testLogging {
        events(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }

    useJUnitPlatform()
}

tasks.withType<Test> {
    testLogging {
        events(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    useJUnitPlatform()
}

tasks.test {
    extensions.configure(JacocoTaskExtension::class) {
        setDestinationFile(file("$buildDir/jacoco/jacoco.exec"))
    }

    finalizedBy("jacocoTestReport")
}

tasks.jacocoTestReport {
    reports {
        html.required.set(false)
        csv.required.set(false)
        xml.required.set(true)
        xml.outputLocation.set(file("../build/reports/jacoco/reposilite-backend-report.xml"))
    }

    finalizedBy("jacocoTestCoverageVerification")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.0".toBigDecimal()
            }
        }
        rule {
            enabled = true
            element = "CLASS"
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.0".toBigDecimal()
            }
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.0".toBigDecimal()
            }
            excludes = listOf()
        }
    }
}

val testCoverage by tasks.registering {
    group = "verification"
    description = "Runs the unit tests with coverage"

    dependsOn(
        ":test",
        ":jacocoTestReport",
        ":jacocoTestCoverageVerification"
    )

    tasks["jacocoTestReport"].mustRunAfter(tasks["test"])
    tasks["jacocoTestCoverageVerification"].mustRunAfter(tasks["jacocoTestReport"])
}