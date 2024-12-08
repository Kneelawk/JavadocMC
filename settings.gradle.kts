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
        maven("https://maven.kneelawk.com/releases/") {
            name = "Kneelawk"
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        val moddev_version: String by settings
        id("net.neoforged.moddev") version moddev_version
        val fabric_loom_version: String by settings
        id("fabric-loom") version fabric_loom_version
        val versioning_version: String by settings
        id("com.kneelawk.versioning") version versioning_version
        val kpublish_version: String by settings
        id("com.kneelawk.kpublish") version kpublish_version
    }
}

rootProject.name = "javadoc-mc"

include("mojmap-vanilla-loom")
include("mojmap-vanilla-moddev")
include("mojmap-neoforge-moddev")
