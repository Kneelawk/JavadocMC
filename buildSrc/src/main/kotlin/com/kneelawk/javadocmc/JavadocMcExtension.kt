package com.kneelawk.javadocmc

import com.kneelawk.getProperty
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems
import net.neoforged.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.ModuleLibrary

abstract class JavadocMcExtension(val project: Project) {
    fun applyLoom() {
        project.plugins.apply("fabric-loom")
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

        val extractMc = project.tasks.create("extractMc", Copy::class) {
            dependsOn("genSourcesWithVineflower")

            into(project.layout.buildDirectory.dir("extractMc"))

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

                project.configurations.getByName("minecraftNamedCompile").resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
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

                    from(project.zipTree(sourceStr))
                }
            }
        }

        project.tasks.named<PatchSourcesTask>("patchSources").configure {
            dependsOn(extractMc)
            for (root in extractMc.outputs.files) {
                from(project.fileTree(root))
            }
        }
    }

    fun applyNeogradleVanilla() {
        project.plugins.apply("net.neoforged.gradle.vanilla")

        project.repositories {
            mavenCentral()
        }

        val subsystemsEx = project.extensions.getByType(Subsystems::class)

        subsystemsEx.apply {
            parchment {
                val parchment_mc_version: String by project
                minecraftVersion = parchment_mc_version
                val parchment_version: String by project
                mappingsVersion = parchment_version
            }
        }

        val minecraft_version: String by project
        val minecraftDependency = project.dependencies.create("net.minecraft:client:$minecraft_version")
        val jetbrains_annotations_version: String by project

        project.dependencies {
            add("implementation", minecraftDependency)

            add("compileOnly", "org.jetbrains:annotations:$jetbrains_annotations_version")
        }

        val vanillaRuntimeExtension = project.extensions.getByType(VanillaRuntimeExtension::class)
        val runtimeDefinition = vanillaRuntimeExtension.definitions.values.first()

        project.tasks.named<PatchSourcesTask>("patchSources").configure {
            dependsOn(runtimeDefinition.sourceJarTask)
            from(project.zipTree(runtimeDefinition.sourceJarTask.get().output))
        }
    }
}
