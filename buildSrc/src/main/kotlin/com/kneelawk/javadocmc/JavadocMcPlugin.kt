package com.kneelawk.javadocmc

import com.kneelawk.getProperty
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.JavadocMemberLevel
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

class JavadocMcPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.gradle.java-library")

        project.extensions.create("javadocMc", JavadocMcExtension::class, project)

        val javaEx = project.extensions.getByType(JavaPluginExtension::class)

        javaEx.toolchain.languageVersion.set(JavaLanguageVersion.of(project.getProperty<String>("java_version")))

        project.group = project.getProperty("maven_group")

        run {
            val minecraftVersion = project.getProperty<String>("minecraft_version")
            val parchmentMcVersion = project.getProperty<String>("parchment_mc_version")
            val parchmentVersion = project.getProperty<String>("parchment_version")
            val buildNumber = project.getProperty<String>("build_number")

            project.version =
                "${minecraftVersion}+parchment.${parchmentMcVersion}-${parchmentVersion}-build.${buildNumber}"
        }

        val mcJavadoc = project.tasks.create("mcJavadoc", Javadoc::class) {
            options.encoding("UTF-8")

            (options as StandardJavadocDocletOptions).apply {
                addBooleanOption("-ignore-source-errors", true)
                addBooleanOption("Xdoclint:none", true)
                addBooleanOption("quiet", true)

                encoding = "UTF-8"
                charSet = "UTF-8"
                memberLevel = JavadocMemberLevel.PRIVATE
                splitIndex(true)

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
