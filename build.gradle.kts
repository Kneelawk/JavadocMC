plugins {
    id("fabric-loom") apply false
    id("com.kneelawk.submodule") apply false
    id("net.neoforged.gradle.vanilla") apply false
}

tasks.create("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    tasks {
        // make builds reproducible
        withType<AbstractArchiveTask>().configureEach {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
        }
    }
}
