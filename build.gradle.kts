plugins {
    id("io.papermc.paperweight.userdev") version "1.7.1"
    java
    id("com.gradleup.shadow") version "8.3.3"
    `maven-publish`
}

group = "net.azisaba"
version = "1.3.0+1.21.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    //maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://jitpack.io/") } // Statz
    maven { url = uri("https://mvn.lumine.io/repository/maven-public/") } // for MythicMobs
    maven { url = uri("https://nexus.neetgames.com/repository/maven-public/") } // for mcMMO
    maven { url = uri("https://repo.azisaba.net/repository/maven-public/") } // LoreEditor, ItemStash
}

dependencies {
    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.0")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("io.lumine:Mythic-Dist:5.7.2")
    compileOnly("net.azisaba.loreeditor:api:1.0.0-SNAPSHOT:all")
    compileOnly("net.azisaba:ItemStash:1.0.0-SNAPSHOT")
    compileOnly("net.azisaba:TAB-BukkitBridge:2.0.2")

    // for migrations
    compileOnly("com.github.Staartvin:Statz:v1.5.5") {
        exclude("nl.lolmewn.stats", "Stats")
        exclude("me.staartvin", "PluginLibrary")
    }
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.1.196") {
        exclude("com.sk89q.worldguard", "worldguard-core")
        exclude("com.sk89q.worldguard", "worldguard-legacy")
    }

    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}

tasks {
    processResources {
        filteringCharset = "UTF-8"
        from(sourceSets.main.get().resources.srcDirs) {
            include("**")

            val tokenReplacementMap = mapOf(
                "version" to project.version,
                "name" to project.rootProject.name,
            )

            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(projectDir) { include("LICENSE") }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    shadowJar {
        relocate("org.mariadb.jdbc", "net.azisaba.lifepvelevel.libs.org.mariadb.jdbc")
        relocate("com.zaxxer.hikari", "net.azisaba.lifepvelevel.libs.com.zaxxer.hikari")
    }
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["sourcesElements"]) {
    skip()
}

publishing {
    repositories {
        maven {
            name = "repo"
            credentials(PasswordCredentials::class)
            url = uri(
                if (project.version.toString().endsWith("SNAPSHOT"))
                    project.findProperty("deploySnapshotURL") ?: System.getProperty("deploySnapshotURL", "https://repo.azisaba.net/repository/maven-snapshots/")
                else
                    project.findProperty("deployReleasesURL") ?: System.getProperty("deployReleasesURL", "https://repo.azisaba.net/repository/maven-releases/")
            )
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks.getByName("sourcesJar"))
        }
    }
}
