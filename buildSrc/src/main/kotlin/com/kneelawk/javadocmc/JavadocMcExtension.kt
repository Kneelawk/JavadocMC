package com.kneelawk.javadocmc

import com.kneelawk.getProperty
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.nfrtgradle.CreateMinecraftArtifacts
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.ModuleLibrary
import javax.inject.Inject

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

        val extractMc = project.tasks.create("extractMc") {
            dependsOn("genSourcesWithVineflower")

            abstract class Injected {
                @get:Inject
                abstract val fs: FileSystemOperations
            }

            val injected = project.objects.newInstance<Injected>()

            val outputDir = project.layout.buildDirectory.dir("extractMc")
            outputs.dir(outputDir)

            doLast {
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

                injected.fs.copy {
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

                    into(outputDir)
                }
            }
        }

        val patchSources = project.tasks.create("patchSources", PatchSourcesTask::class) {
            into(project.layout.buildDirectory.dir("patchedMc"))
            dependsOn(extractMc)
            for (root in extractMc.outputs.files) {
                from(project.fileTree(root))
            }
        }

        project.tasks.named<Javadoc>("mcJavadoc").configure {
            dependsOn(patchSources)
            source(patchSources.outputs)
        }
    }

    fun applyModDevVanilla() {
        project.plugins.apply("net.neoforged.moddev")

        project.repositories {
            mavenCentral()
        }

        val neoforgeEx = project.extensions.getByType<NeoForgeExtension>()

        val minecraft_version: String by project
        val neoform_version: String by project
        neoforgeEx.neoFormVersion.set("$minecraft_version-$neoform_version")

        neoforgeEx.parchment {
            val parchment_mc_version: String by project
            minecraftVersion = parchment_mc_version
            val parchment_version: String by project
            mappingsVersion = parchment_version
        }

        val createMinecraftArtifacts = project.tasks.named<CreateMinecraftArtifacts>("createMinecraftArtifacts")

        project.tasks.named<Javadoc>("mcJavadoc").configure {
            dependsOn(createMinecraftArtifacts)
            source(project.zipTree(createMinecraftArtifacts.get().sourcesArtifact))
        }
    }

    fun applyModDevNeoForge() {
        project.plugins.apply("net.neoforged.moddev")

        project.repositories {
            mavenCentral()
        }

        val neoforgeEx = project.extensions.getByType<NeoForgeExtension>()

        val neoforge_version: String by project
        neoforgeEx.version.set(neoforge_version)

        neoforgeEx.parchment {
            val parchment_mc_version: String by project
            minecraftVersion = parchment_mc_version
            val parchment_version: String by project
            mappingsVersion = parchment_version
        }

        val createMinecraftArtifacts = project.tasks.named<CreateMinecraftArtifacts>("createMinecraftArtifacts")

        val filterMc = project.tasks.create<Copy>("filterMc") {
            dependsOn(createMinecraftArtifacts)
            from(project.zipTree(createMinecraftArtifacts.get().sourcesArtifact))
            exclude("assets/**", "data/**", "reports/**", "META-INF/**", "*.json", "*.png", "*.mcmeta", "forge.sas", "forge.exc", "forge.srg")
            into(project.layout.buildDirectory.dir("filterMc"))
        }

        project.tasks.named<Javadoc>("mcJavadoc").configure {
            dependsOn(filterMc)
            source(filterMc.outputs)
        }
    }
}
