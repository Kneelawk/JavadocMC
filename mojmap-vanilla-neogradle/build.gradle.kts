plugins {
    id("com.kneelawk.versioning")
    id("com.kneelawk.kpublish")
    id("com.kneelawk.javadocmc")
}

javadocMc {
    applyNeogradleVanilla()
}

kpublish {
    createPublication(tasks = arrayOf(tasks.mcJavadocJar))
}
