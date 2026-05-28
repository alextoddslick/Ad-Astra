plugins {
    java
    id("maven-publish")
    id("net.fabricmc.fabric-loom")
}

val minecraftVersion: String by project
val mcMinorVersion: String by project
val fabricLoaderVersion: String by project
val fabricApiVersion: String by project
val mixinExtrasVersion: String by project
val modMenuVersion: String by project
val jeiVersion: String by project
val reiVersion: String by project
val resourcefulLibVersion: String by project
val resourcefulConfigVersion: String by project
val commonStorageLibVersion: String by project
val athenaVersion: String by project

val modId = rootProject.name
val modLoader = project.name

val stationsFile: String = rootProject.file("stations.json").absolutePath

base {
    archivesName.set("$modId-$modLoader-$minecraftVersion")
}

val runLogsDir = project.file("run/logs").apply { mkdirs() }

loom {
    runConfigs.configureEach {
        property("adastra.stations", stationsFile)
        vmArgs(
            // Match -Xms to -Xmx so G1 commits the full heap at startup. With -Xms256m
            // the heap kept expanding tier-by-tier (256m -> 512m -> 1G -> ...) and each
            // expansion stalled the render thread — visible in F3 as the "Allocated"
            // number stepping up just before a stutter. With matched min/max + pre-touch
            // there are no expansion stalls.
            "-Xmx4G",
            "-Xms4G",
            "-XX:+AlwaysPreTouch",
            // Silence Fabric tag-convention warning about untranslated item tags. Cosmetic
            // dev-mode noise — the warning appears on every server start because Ad Astra's
            // item tags don't have lang entries. Default is "SHORT" (long warning); use
            // "SILENCED" to drop entirely.
            "-Dfabric-tag-conventions-v2.missingTagTranslationWarning=SILENCED",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:HeapDumpPath=${runLogsDir.resolve("heap-dump.hprof").absolutePath}",
            "-Xlog:gc*,gc+age=trace,safepoint:file=${runLogsDir.resolve("gc.log").absolutePath}:time,uptime,level,tags:filecount=5,filesize=20M",
            "-XX:StartFlightRecording=name=adastra,filename=${runLogsDir.resolve("adastra.jfr").absolutePath},maxsize=500m,settings=profile,dumponexit=true",
            "-XX:FlightRecorderOptions=stackdepth=128"
        )
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://maven.fabricmc.net/")
    maven(url = "https://maven.terraformersmc.com/releases/")
    maven(url = "https://maven.teamresourceful.com/repository/maven-public/")
    maven(url = "https://maven.teamresourceful.com/repository/terrarium/")
    maven(url = "https://maven.resourcefulbees.com/repository/maven-public/")
    maven(url = "https://kneelawk.com/maven")
    maven(url = "https://maven.firstdarkdev.xyz/snapshots")
    maven {
        url = uri("https://www.cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraftVersion")
    // No mappings line — 26.1 is unobfuscated.

    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    api("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion+$minecraftVersion")

    api(
        group = "com.teamresourceful.resourcefullib",
        name = "resourcefullib-fabric-$mcMinorVersion",
        version = resourcefulLibVersion
    )
    api("com.teamresourceful:bytecodecs:1.1.3")
    api("com.teamresourceful:yabn:1.0.3")
    api(
        group = "com.teamresourceful.resourcefulconfig",
        name = "resourcefulconfig-fabric-$mcMinorVersion",
        version = resourcefulConfigVersion
    )
    api(
        group = "earth.terrarium.common_storage_lib",
        name = "common-storage-lib-fabric-$minecraftVersion",
        version = commonStorageLibVersion
    )

    include(implementation(group = "javazoom", name = "jlayer", version = "1.0.1"))

    compileOnly(group = "mezz.jei", name = "jei-$minecraftVersion-common-api", version = jeiVersion)
    // REI integration kept dormant — no 26.x builds yet.
    // compileOnly(group = "me.shedaniel", name = "RoughlyEnoughItems-api-fabric", version = reiVersion)
    // compileOnly(group = "me.shedaniel", name = "RoughlyEnoughItems-default-plugin-fabric", version = reiVersion)

    annotationProcessor(group = "io.github.llamalad7", name = "mixinextras-common", version = mixinExtrasVersion)
    implementation(group = "io.github.llamalad7", name = "mixinextras-common", version = mixinExtrasVersion)

    api("com.terraformersmc:modmenu:$modMenuVersion")

    // Athena, Cadmus, Argonauts, Patchouli, Shimmer not yet available for 26.1.2 — left commented for future
    // api("earth.terrarium.athena:athena-fabric-$minecraftVersion:$athenaVersion")
    // api("earth.terrarium.cadmus:cadmus-fabric-$minecraftVersion:$cadmusVersion") { isTransitive = false }
    // api("earth.terrarium.argonauts:argonauts-fabric-$minecraftVersion:$argonautsVersion") { isTransitive = false }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

sourceSets.main.get().resources.srcDir("src/main/generated/resources")

sourceSets.main.get().java.exclude("**/compat/rei/**")

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jar {
    exclude(".cache/**")               // Remove datagen cache from jar.
    exclude("**/adastra/datagen/**")    // Remove datagen code from jar.
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "$modId-$modLoader-$minecraftVersion"
            from(components["java"])

            pom {
                name.set("Ad Astra $modLoader")
                url.set("https://github.com/terrarium-earth/$modId")

                scm {
                    connection.set("git:https://github.com/terrarium-earth/$modId.git")
                    developerConnection.set("git:https://github.com/terrarium-earth/$modId.git")
                    url.set("https://github.com/terrarium-earth/$modId")
                }

                licenses {
                    license {
                        name.set("Terrarium Licence (https://gist.github.com/CodexAdrian/4bb2a1868bb2d2a91ca74ea40424e69d)")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            setUrl("https://maven.teamresourceful.com/repository/terrarium/")
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_PASS")
            }
        }
    }
}
