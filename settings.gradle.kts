enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "adastra"

pluginManagement {
    repositories {
        maven(url = "https://maven.fabricmc.net/")
        maven(url = "https://maven.teamresourceful.com/repository/maven-public/")
        mavenCentral()
        gradlePluginPortal()
    }
}

include("fabric")
