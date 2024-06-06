plugins {
    id("com.kneelawk.versioning")
    id("com.kneelawk.kpublish")
    id("com.kneelawk.javadocmc")
}

javadocMc {
    applyLoom()
}

dependencies {
    val fabric_loader_version: String by project
    add("modCompileOnly", "net.fabricmc:fabric-loader:$fabric_loader_version")
}

tasks.mcJavadoc.configure {
    (options as StandardJavadocDocletOptions).apply {
        val fabric_loader_version: String by project
        links(
            "https://maven.fabricmc.net/docs/fabric-loader-${fabric_loader_version}/",
        )
    }
}

kpublish {
    createPublication(tasks = arrayOf(tasks.mcJavadocJar))
}
