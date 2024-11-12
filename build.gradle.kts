plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
}

group = "net.azisaba"
version = "2.0.0+1.15.2"

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    //maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://jitpack.io/") } // Statz
    maven { url = uri("https://mvn.lumine.io/repository/maven-public/") } // for MythicMobs
    maven { url = uri("https://nexus.neetgames.com/repository/maven-public/") } // for mcMMO
    maven { url = uri("https://repo.azisaba.net/repository/maven-public/") } // LoreEditor, ItemStash
    if (properties["azisabaNmsUsername"] != null && properties["azisabaNmsPassword"] != null) {
        maven {
            name = "azisabaNms"
            credentials(PasswordCredentials::class)
            url = uri("https://repo.azisaba.net/repository/nms/")
        }
    }
    mavenLocal()
}

dependencies {
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.0.6")
    compileOnly("net.azisaba:TAB-BukkitBridge:3.0.0")
    compileOnly("org.spigotmc:spigot:1.15.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("io.lumine:Mythic-Dist:4.13.0")
    compileOnly("net.azisaba.loreeditor:api:1.0.0-SNAPSHOT:all")
    compileOnly("net.azisaba:ItemStash:1.0.0-SNAPSHOT")

    // for migrations
    compileOnly("com.github.Staartvin:Statz:v1.5.5") {
        exclude("nl.lolmewn.stats", "Stats")
        exclude("me.staartvin", "PluginLibrary")
        exclude("org.bukkit", "bukkit")
    }
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.1.196") {
        exclude("com.sk89q.worldguard", "worldguard-core")
        exclude("com.sk89q.worldguard", "worldguard-legacy")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
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
