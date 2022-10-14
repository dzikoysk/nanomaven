﻿<div align="center">
 <h1>Reposilite</h1>
 <div>
  <a href="https://github.com/dzikoysk/reposilite/actions/workflows/gradle.yml">
   <img alt="Reposilite CI" src="https://github.com/dzikoysk/reposilite/actions/workflows/gradle.yml/badge.svg" />
  </a>
  <a href="https://github.com/dzikoysk/reposilite/releases">
   <img src="https://maven.reposilite.com/api/badge/latest/releases/com/reposilite/reposilite?color=40c14a&name=Reposilite&prefix=v" />
  </a>
  <a href="https://codecov.io/gh/dzikoysk/reposilite">
   <img alt="CodeCov" src="https://codecov.io/gh/dzikoysk/reposilite/branch/main/graph/badge.svg?token=9flNHTSJpp" />
  </a>
  <a href="https://hub.docker.com/r/dzikoysk/reposilite">
   <img alt="Docker Pulls" src="https://img.shields.io/docker/pulls/dzikoysk/reposilite.svg?label=pulls&logo=docker" />
  </a>
  <!--
  <a href="(https://www.codefactor.io/repository/github/dzikoysk/reposilite/overview/main">
   <img alt="CodeFactor" src="https://www.codefactor.io/repository/github/dzikoysk/reposilite/badge/main" />
  </a>
  -->
  <a href="https://discord.gg/qGRqmGjUFX">
   <img alt="Discord" src="https://img.shields.io/badge/discord-reposilite-738bd7.svg?style=square" />
  </a>
  <a href="https://discord.gg/qGRqmGjUFX">
   <img alt="Discord Online" src="https://img.shields.io/discord/204728244434501632.svg" />
  </a>
 </div>
 <br>
 <div>
  Lightweight and easy-to-use repository manager for Maven based artifacts in JVM ecosystem. 
This is simple, extensible and scalable self-hosted solution to replace managers like Nexus, Archiva or Artifactory, with reduced resources consumption. 
 </div>
 <br>
 <div>
  <a href="https://reposilite.com">Website</a>
  |
  <a href="https://reposilite.com/guide/about">Official Guide</a>
  |
  <a href="https://github.com/dzikoysk/reposilite/releases">GitHub Releases</a>
  |
  <a href="https://hub.docker.com/r/dzikoysk/reposilite">DockerHub Images</a>
  |
  <a href="https://panda-lang.org/support">Support</a>
  |
  <a href="https://maven.reposilite.com">Demo</a>
 </div>
 <br>
 <img alt="Preview" src="https://user-images.githubusercontent.com/4235722/133891983-966e5c6d-97b1-48cc-b754-6e88117ee4f7.png" />
 <br>
</div>

### Installation
To run Reposilite for your personal needs you should assign around 16MB of RAM and at least Java 11+ installed. <br>
For huge public repositories you can adjust memory limit and even size of used thread pools in the configuration.

```bash
# Launching a standalone JAR file
$ java -Xmx16M -jar reposilite-3.0.4.jar

# Using the official Docker image
$ docker pull dzikoysk/reposilite:3.0.4

# Using the official Helm chart
$ helm repo add reposilite https://helm.reposilite.com/
$ helm repo update
$ helm install reposilite/reposilite
```

Visit official guide to read more about extra parameters and configuration details.

### Publications

Reposilite 3.x:
* [Reposilite 3.x / Official Guide](https://reposilite.com/guide/about)
* [Reddit / 3.x Thread](https://www.reddit.com/r/java/comments/xy07vc/reposilite_3x_released_alternative_lightweight/)
* [Medium / Setup your own Maven repository manager in 5 minutes](https://dzikoysk.medium.com/reposilite-3-x-setup-your-own-maven-repository-manager-in-5-minutes-e72cc8b67bc3)

Reposilite 2.x:
* [Reposilite 2.x / Official Guide](https://v2.reposilite.com/)
* [Reddit / 2.x Thread](https://www.reddit.com/r/java/comments/k8i2m0/reposilite_alternative_lightweight_maven/)
* [Dev.to / Publishing your artifacts to the Reposilite - a new self-hosted repository manager ](https://dev.to/dzikoysk/publishing-your-artifacts-to-the-reposilite-a-new-self-hosted-repository-manager-3n0h)
* [Medium / Looking for simple repository manager by David Kihato](https://kihats.medium.com/custom-self-hosted-maven-repository-cbb778031f68)

### Supporters
Thanks to all contributors and people that decided to support my work financially ❤️

<table>
 <tr>
  <td>
   <a href="https://github.com/sponsors/dzikoysk">Still active GitHub Sponsors</a>
  </td>
  <td>
    <a href="https://github.com/milkyway0308">milkyway0308</a>,
    <a href="https://github.com/tipsy">tipsy</a>, 
    <a href="https://github.com/Koressi">Koressi</a>,
    <a href="https://github.com/insertt">insertt</a>, 
    <a href="https://github.com/andrm">andrm</a>, 
    <a href="https://github.com/rdehuyss">rdehuyss</a>,
    <a href="https://github.com/zugazagoitia">zugazagoitia</a>,
    <a href="https://github.com/neg4n">neg4n</a>, 
    <a href="https://github.com/sebba-dev">sebba-dev</a>,
    <a href="https://github.com/FlawCra">FlawCra</a>,
    <a href="https://github.com/arthurr0">arthurr0</a>
  </td>
 </tr>
 <tr>
  <td>All time</td>
  <td>
   <a href="https://github.com/zzmgck">zzmgck</a>, 
   <a href="https://github.com/Koressi">Koressi</a>,
   <a href="https://github.com/insertt">insertt</a>, 
   <a href="https://github.com/milkyway0308">milkyway0308</a>,
   <a href="https://github.com/tipsy">tipsy</a>, 
   <a href="https://github.com/insertt">bmstefanski</a>, 
   <a href="https://github.com/rdehuyss">rdehuyss</a>
   <a href="https://github.com/andrm">andrm</a>,
   <a href="https://github.com/neg4n">neg4n</a>,
   <a href="https://github.com/maxant">maxant</a>,
   <a href="https://github.com/alexwhb">alexwhb</a>, 
   <a href="https://github.com/kay">Douglas Lawrie</a>,
   <a href="https://github.com/zugazagoitia">zugazagoitia</a>,
   <a href="https://github.com/FlawCra">FlawCra</a>,
   <a href="https://github.com/crejk">crejk</a>,
   <a href="https://github.com/EthanDevelops">EthanDevelops</a>, 
   <a href="https://github.com/escv">escv</a>,
   <a href="https://github.com/arthurr0">arthurr0</a>
   <a href="https://github.com/shitzuu">shitzuu</a>,
   <a href="https://github.com/peter-jerry-ye">peter-jerry-ye</a>,
   Rob,
   <a href="https://github.com/sebba-dev">sebba-dev</a>,
   <a href="https://github.com/mufinlive">mufinlive</a>
  </td>
 </tr>
</table>

`\(^-^)/` The list is updated periodically and entries are sorted by aggregated total payment size of the given person.

### For developers

Recommended tool to develop backend module is IntelliJ IDE, for frontend it might be e.g. VSC.

```bash
# Run only backend through CLI
$ ./gradlew run

# Run only frontend
$ cd reposilite-frontend && npm i && npm run full

# Run only Reposilite site
$ cd reposilite-site/website && npm i && npm run start
```

#### Stack

[Reposilite 3.x](https://reposilite.com/)
* Reposilite Backend: [Kotlin](https://kotlinlang.org/) + [Javalin](https://javalin.io) + [Exposed (SQL)](https://github.com/JetBrains/Exposed) + [AWS SDK](https://github.com/aws/aws-sdk-java) + [JUnit](https://junit.org/junit5/) + [Testcontainers](https://www.testcontainers.org/) + _(DDD & Hexagonal Architecture)_
* Reposilite Frontend: [Vue3](https://vuejs.org/) + [Vite](https://vitejs.dev/) + [WindiCSS](https://windicss.org/) + [JsonForms](https://jsonforms.io/)
* Reposilite Site: [Next.js](https://nextjs.org/) + [Vercel](https://vercel.com/)

[Reposilite 2.x](https://v2.reposilite.com/)
* Reposilite Backend: Java + [Javalin](https://javalin.io/) + [Groovy](https://groovy-lang.org/) ([JUnit](https://junit.org/junit5/)) + _(DDD)_
* Reposilite Frontend: [Vue2](https://v2.vuejs.org/) + [Pug](https://pugjs.org/api/getting-started.html) + [Stylus](https://stylus-lang.com/) + [TailwindCSS](https://tailwindcss.com/)
* Reposilite Site: [React.js](https://reactjs.org/) + [Docusaurus v1](https://docusaurus.io/)

Reposilite 1.x
* Reposilite: Java + [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)
