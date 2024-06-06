package com.kneelawk.javadocmc

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

open class PatchSourcesTask @Inject constructor(private val executor: WorkerExecutor) : DefaultTask() {
    @get:InputFiles
    val froms = mutableListOf<FileCollection>()

    @get:OutputDirectory
    lateinit var into: File

    fun from(from: Any) {
        froms.add(if (from is FileCollection) from else project.files(from))
    }

    fun into(into: Any) {
        this.into = project.file(into)
    }

    @TaskAction
    fun execute() {
        for (fc in froms) {
            if (fc is FileTree) {
                fc.visit {
                    if (!isDirectory && name.endsWith(".java")) {
                        val destination = File(into, path)
                        submitWorker(file, destination)
                    }
                }
            } else {
                for (file in fc.files) {
                    if (!file.isDirectory && file.name.endsWith(".java")) {
                        val destination = File(into, file.name)
                        submitWorker(file, destination)
                    }
                }
            }
        }
    }

    private fun submitWorker(source: File, destination: File) {
        executor.noIsolation().submit(ParallelWorker::class) {
            sourceFile.set(source)
            destinationFile.set(destination)
        }
    }

    abstract class ParallelWorker : WorkAction<FixDecompileParameters> {
        override fun execute() {
            val source = parameters.sourceFile.get().asFile
            val destination = parameters.destinationFile.get().asFile

            destination.parentFile.mkdirs()

            // let us begin
            val builder = StringBuilder()
            val annotationBuilder = StringBuilder()

            var lookingForRecord = false
            var inRecord = false
            for (line in source.readLines()) {
                var skip = false

                if (inRecord) {
                    if (line.contains('@') && !line.endsWith(';')) {
                        annotationBuilder.append(line).append(' ')
                        skip = true
                    }
                    if (line.contains("final") && !line.contains("static") && line.endsWith(';')) {
                        annotationBuilder.clear()
                        skip = true
                    }
                }

                if (line.contains("record")) {
                    lookingForRecord = true
                }
                if (lookingForRecord && line.endsWith('{')) {
                    lookingForRecord = false
                    inRecord = true
                } else {
                    if (line.contains('{') || line.contains('}')) {
                        inRecord = false
                    }
                }

                if (!skip) {
                    var line = line.replace("EntityRendererProvider<>", "EntityRendererProvider<?>")
                    line = line.replace("(E[])(65, 70, 75, 80)", "new Integer[]{65, 70, 75, 80}")
                    line = line.replace(
                        "SwitchBootstraps.typeSwitch<\"typeSwitch\",Player,OminousItemSpawner>(var2, var3)",
                        "0"
                    )
                    line = line.replace(
                        "SwitchBootstraps.typeSwitch<\"typeSwitch\",FlatLevelSource,DebugLevelSource,NoiseBasedChunkGenerator>(chunkGenerator, i)",
                        "0"
                    )
                    builder.append(annotationBuilder.toString()).append(line).append('\n')
                    annotationBuilder.clear()
                }
            }

            destination.writeText(builder.toString())
        }
    }

    interface FixDecompileParameters : WorkParameters {
        val sourceFile: RegularFileProperty
        val destinationFile: RegularFileProperty
    }
}
