plugins {
    id("com.kneelawk.kpublish")
    id("com.kneelawk.javadocmc")
}

javadocMc {
    applyModDevNeoForge()
}

kpublish {
    createPublication(tasks = arrayOf(tasks.mcJavadocJar))
}
