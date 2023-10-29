---
id: gradle
title: Gradle
---

### Gradle

Deploying with Gradle is quite similar, read the official [gradle publishing docs](https://docs.gradle.org/current/userguide/publishing_maven.html) to learn more.

If you are only deploying to a single repository, here is how to do it.
Firstly, add your access token to your `~/.gradle/gradle.properties`.

<CodeVariants>
  <CodeVariant name="~/.gradle/gradle.properties">

```properties
myDomainRepositoryUsername={token}
myDomainRepositoryPassword={secret}
```

`Warning` This should be in your _GRADLE_USER_HOME_, *not* your project _gradle.properties_ file:

  </CodeVariant>
  <CodeVariant name="Via command line properties">
  
```bash
$ ./gradlew publish \
  -PmyDomainRepositoryUsername={token} \
  -PmyDomainRepositoryPassword={secret}
```

  </CodeVariant>
</CodeVariants>

Next, add the `publishing` plugin, as well as the maven repository:

<CodeVariants>
  <CodeVariant name="Gradle (Kts)">

```kotlin
plugins {
  `maven-publish`
}

publishing {
  repositories {
    maven {
      name = "myDomainRepository"
      url = uri("https://repo.my-domain.com/releases")
      credentials(PasswordCredentials::class)
      authentication {
        create<BasicAuthentication>("basic")
      }
    }
  }
  publications {
    create<MavenPublication>("maven") {
      groupId = "com.example"
      artifactId = "library"
      version = "1.0.0"
      from(components["java"])
    }
  }
}
```

  </CodeVariant>
  <CodeVariant name="Gradle (Groovy)">

```groovy
plugins {
  id 'maven-publish'
}

publishing {
  repositories {
    maven {
      name = "myDomainRepository"
      url = "https://repo.my-domain.com/releases"
      credentials(PasswordCredentials)
      authentication {
        basic(BasicAuthentication)
      }
    }
  }
  publications {
    maven(MavenPublication) {
      groupId = "com.example"
      artifactId = "library"
      version = "1.0.0"
      from components.java
    }
  }
}
```

  </CodeVariant>
</CodeVariants>

To publish to several repositories, you have to declare it for all repositories _(which may or may not be the same)_, e.g.:

```properties
# Releases token & secret
myDomainRepositoryReleasesUsername={token}
myDomainRepositoryReleasesPassword={secret}

# Snapshots token & secret
myCoolRepositorySnapshotsUsername={token}
myCoolRepositorySnapshotsPassword={secret}
```

And declare multiple target in build file:

<CodeVariants>
  <CodeVariant name="Gradle (Kts)">

```kotlin
maven {
  name = "myDomainRepositoryReleases"
  url = uri("https://repo.my-domain.com/releases")
  credentials(PasswordCredentials::class)
  authentication {
    create<BasicAuthentication>("basic")
  }
}
maven {
  name = "myCoolRepositorySnapshots"
  url = uri("https://repo.my-cool.com/snapshots")
  credentials(PasswordCredentials::class)
  authentication {
    create<BasicAuthentication>("basic")
  }
}
```

  </CodeVariant>
  <CodeVariant name="Gradle (Groovy)">

```groovy
maven {
  name = "myDomainRepositoryReleases"
  url = "https://repo.my-domain.com/releases"
  credentials(PasswordCredentials)
  authentication {
    basic(BasicAuthentication)
  }
}
maven {
  name = "myCoolRepositorySnapshots"
  url = uri("https://repo.my-cool.com/snapshots")
  credentials(PasswordCredentials)
  authentication {
    basic(BasicAuthentication)
  }
}
```

  </CodeVariant>
</CodeVariants>
