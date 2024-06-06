plugins {
    id("java-gradle-plugin")
    kotlin("jvm") version "2.0.0"
    `kotlin-dsl`
    antlr
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
    antlr("org.antlr:antlr4:4.13.1")

    val architectury_loom_version: String by project
    implementation("dev.architectury.loom:dev.architectury.loom.gradle.plugin:$architectury_loom_version")
}

gradlePlugin {
    plugins {
        create("javadocMcPlugin") {
            id = "com.kneelawk.javadocmc"
            implementationClass = "com.kneelawk.javadocmc.JavadocMcPlugin"
        }
    }
}

tasks.generateGrammarSource.configure {
    val pkg = "com.kneelawk.javadocmc"
    arguments = arguments + listOf("-package", pkg)
    outputDirectory = outputDirectory.resolve(pkg.split(".").joinToString("/"))
}

tasks.compileKotlin.get().dependsOn("generateGrammarSource")
