pluginManagement {
    repositories {
        maven("https://maven.quiltmc.org/repository/release") {
            name = "Quilt"
        }
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        maven("https://maven.architectury.dev/") {
            name = "Architectury"
        }
        maven("https://maven.neoforged.net/releases/") {
            name = "NeoForged"
        }
        maven("https://kneelawk.com/maven/") {
            name = "Kneelawk"
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        val neogradle_version: String by settings
        id("net.neoforged.gradle.vanilla") version neogradle_version
        id("net.neoforged.gradle.userdev") version neogradle_version
        val fabric_loom_version: String by settings
        id("fabric-loom") version fabric_loom_version
        val versioning_version: String by settings
        id("com.kneelawk.versioning") version versioning_version
        val kpublish_version: String by settings
        id("com.kneelawk.kpublish") version kpublish_version
        val submodule_version: String by settings
        id("com.kneelawk.submodule") version submodule_version
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "javadoc-mc"

include("mojmap-vanilla-loom")
include("mojmap-vanilla-neogradle")
