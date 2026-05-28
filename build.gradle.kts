import groovy.json.StringEscapeUtils

plugins {
    java
    id("maven-publish")
    id("com.teamresourceful.resourcefulgradle") version "0.0.+"
    id("net.fabricmc.fabric-loom") version "1.15.5" apply false
}

resourcefulGradle {
    templates {
        register("embed") {
            val minecraftVersion: String by project
            val version: String by project
            val changelog: String = file("changelog.md").readText(Charsets.UTF_8)
            val fabricLink: String? = System.getenv("FABRIC_RELEASE_URL")

            source.set(file("templates/embed.json.template"))
            injectedValues.set(
                mapOf<String, Any>(
                    "minecraft" to minecraftVersion,
                    "version" to version,
                    "changelog" to StringEscapeUtils.escapeJava(changelog),
                    "fabric_link" to (fabricLink ?: ""),
                )
            )
        }
    }
}
