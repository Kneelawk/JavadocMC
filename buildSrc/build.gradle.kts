plugins {
    id("java-gradle-plugin")
    kotlin("jvm") version "2.0.0"
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    maven("https://maven.quiltmc.org/repository/release") { name = "Quilt" }
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://kneelawk.com/maven") { name = "Kneelawk" }
}

dependencies {
    val fabric_loom_version: String by project
    implementation("fabric-loom:fabric-loom.gradle.plugin:$fabric_loom_version")

    val neogradle_version: String by project
    implementation("net.neoforged.gradle.vanilla:net.neoforged.gradle.vanilla.gradle.plugin:$neogradle_version")
}

gradlePlugin {
    plugins {
        create("javadocMcLoomPlugin") {
            id = "com.kneelawk.javadocmc"
            implementationClass = "com.kneelawk.javadocmc.JavadocMcPlugin"
        }
    }
}
