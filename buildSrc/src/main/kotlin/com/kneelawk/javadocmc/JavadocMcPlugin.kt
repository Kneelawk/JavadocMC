package com.kneelawk.javadocmc

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.maven
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.ModuleLibrary

class JavadocMcPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("dev.architectury.loom")
        project.plugins.apply("org.gradle.idea")

        val loomEx = project.extensions.getByType(LoomGradleExtensionAPI::class)
        
        

        project.repositories.apply {
            mavenCentral()
            maven("https://maven.quiltmc.org/repository/release") { name = "Quilt" }
            maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
            maven("https://maven.firstdark.dev/snapshots") { name = "FirstDark" }
            maven("https://maven.kneelawk.com/releases/") { name = "Kneelawk" }
            maven("https://maven.alexiil.uk/") { name = "AlexIIL" }
            maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
            maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
            maven("https://thedarkcolour.github.io/KotlinForForge/") { name = "Kotlin" }

            mavenLocal()
        }

        project.dependencies.apply {
            val minecraftVersion = project.getProperty<String>("minecraft_version")
            add("minecraft", "com.mojang:minecraft:$minecraftVersion")

            val mappingsType = project.findProperty("mappings_type") as? String ?: "mojmap"

            when (mappingsType) {
                "mojmap" -> {
                    val parchmentMcVersion = project.getProperty<String>("parchment_mc_version")
                    val parchmentVersion = project.getProperty<String>("parchment_version")
                    add("mappings", loomEx.layered {
                        officialMojangMappings()
                        parchment("org.parchmentmc.data:parchment-$parchmentMcVersion:$parchmentVersion@zip")
                    })
                }
                "yarn" -> {
                    val yarnVersion = project.getProperty<String>("yarn_version")
                    val yarnPatch = project.getProperty<String>("yarn_patch")
                    add("mappings", loomEx.layered {
                        mappings("net.fabricmc:yarn:$minecraftVersion+build.$yarnVersion:v2")
                        mappings("dev.architectury:yarn-mappings-patch-neoforge:$yarnPatch")
                    })
                }
            }

            add("compileOnly", "com.google.code.findbugs:jsr305:3.0.2")
            add("testCompileOnly", "com.google.code.findbugs:jsr305:3.0.2")

            add("testImplementation", platform("org.junit:junit-bom:5.10.2"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

        val extractMcSources = project.tasks.create("extractMcSources", FixDecompileTask::class) {
            dependsOn("genSourcesWithVineflower")

            into(project.layout.buildDirectory.dir("decompileFixed"))

            project.afterEvaluate {
                val sourceMappings = mutableMapOf<String, MutableSet<String>>()

                project.extensions.getByType(IdeaModel::class).module.resolveDependencies().forEach { dep ->
                    if (dep is ModuleLibrary) {
                        dep.classes.forEach { classPath ->
                            val url = classPath.canonicalUrl
                            val classesStr = if (url.startsWith("jar://")) {
                                val endIndex = url.lastIndexOf('!')

                                if (endIndex < 0) {
                                    url.substring("jar://".length)
                                } else {
                                    url.substring("jar://".length, endIndex)
                                }
                            } else {
                                url
                            }

                            sourceMappings.computeIfAbsent(classesStr) { mutableSetOf() }
                                .addAll(dep.sources.map { it.url })
                        }
                    }
                }

                val sources = mutableSetOf<String>()

                configurations.getByName("minecraftNamedCompile").resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                    val id = artifact.id.componentIdentifier

                    if (id is ModuleComponentIdentifier && id.group.startsWith("net.minecraft")) {
                        val path = artifact.file.canonicalPath
                        val source = sourceMappings[path]
                        if (source != null) {
                            sources.addAll(source)
                        }
                    }
                }

                sources.forEach { source ->
                    val sourceStr = if (source.startsWith("jar://")) {
                        val endIndex = source.lastIndexOf('!')

                        if (endIndex < 0) {
                            source.substring("jar://".length)
                        } else {
                            source.substring("jar://".length, endIndex)
                        }
                    } else {
                        source
                    }

                    from(zipTree(sourceStr))
                }
            }
        }

        val mcJavadoc = project.tasks.create("mcJavadoc", Javadoc::class) {
            dependsOn(extractMcSources)
            source(extractMcSources.outputs)

            setDestinationDir(project.rootProject.layout.buildDirectory.dir("docs/vanilla").get().asFile)
            options.encoding("UTF-8")

            (options as StandardJavadocDocletOptions).apply {
                addBooleanOption("-ignore-source-errors", true)
                addBooleanOption("Xdoclint:none", true)
                addBooleanOption("quiet", true)

                val jetbrainsAnnotationsVersion = project.getProperty<String>("jetbrains_annotations_version")

                links(
                    "https://guava.dev/releases/32.1.2-jre/api/docs/",
                    "https://www.javadoc.io/doc/com.google.code.gson/gson/2.10.1/",
                    "https://logging.apache.org/log4j/2.x/javadoc/log4j-api/",
                    "https://www.slf4j.org/apidocs/",
                    "https://javadoc.io/doc/org.jetbrains/annotations/${jetbrainsAnnotationsVersion}/",
                    "https://javadoc.lwjgl.org/",
                    "https://fastutil.di.unimi.it/docs/",
                    "https://javadoc.scijava.org/JOML/",
                    "https://netty.io/4.1/api/",
                    "https://www.oshi.ooo/oshi-core-java11/apidocs/",
                    "https://java-native-access.github.io/jna/5.13.0/javadoc/",
                    "https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/",
                    "https://jopt-simple.github.io/jopt-simple/apidocs/",
                    "https://solutions.weblite.ca/java-objective-c-bridge/docs/",
                    "https://commons.apache.org/proper/commons-logging/apidocs/",
                    "https://commons.apache.org/proper/commons-lang/javadocs/api-release/",
                    "https://commons.apache.org/proper/commons-io/apidocs/",
                    "https://commons.apache.org/proper/commons-codec/archives/1.15/apidocs/",
                    "https://commons.apache.org/proper/commons-compress/apidocs/",
                    "https://hc.apache.org/httpcomponents-client-4.5.x/current/httpclient/apidocs/",
                    "https://docs.oracle.com/en/java/javase/21/docs/api/"
                )
            }

            classpath = project.configurations.getByName("compileClasspath")
        }

        project.tasks.create("mcJavadocJar", Jar::class) {
            from(mcJavadoc)

            archiveClassifier.set("javadoc")

            destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
        }
    }
}
