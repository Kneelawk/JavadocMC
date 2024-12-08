plugins {
    id("com.kneelawk.kpublish")
    id("com.kneelawk.javadocmc")
}

javadocMc {
    applyModDevVanilla()
}

kpublish {
    createPublication(tasks = arrayOf(tasks.mcJavadocJar))
}
