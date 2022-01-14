plugins {
    idea
    `java-library`
    application
    `maven-publish`

    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
}

allprojects {
    repositories {
        // maven {
        //     url = uri("http://localhost/releases")
        //     isAllowInsecureProtocol = true
        // }
        // mavenCentral()
        maven { url = uri("https://repo.panda-lang.org/releases") }
        maven { url = uri("https://jitpack.io") }
        mavenCentral()
    }
}

subprojects {
    version = "3.0.0-alpha.18"

    apply(plugin = "idea")
    apply(plugin = "java-library")
    apply(plugin = "application")
    apply(plugin = "maven-publish")

    dependencies {

    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        repositories {
            maven {
                name = "panda-repository"
                url = uri("https://repo.panda-lang.org/${if (version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"}")
                credentials {
                    username = System.getenv("MAVEN_NAME") ?: ""
                    password = System.getenv("MAVEN_TOKEN") ?: ""
                }
            }
        }
    }
}