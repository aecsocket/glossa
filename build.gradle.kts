plugins {
    id("parent-conventions")
}

group = "io.github.aecsocket"
version = "1.0.1-SNAPSHOT"
description = "Localization library for Adventure components"

tasks.register("printVersionType") {
    doFirst {
        println(if (net.kyori.indra.util.Versioning.isSnapshot(project)) "snapshot" else "release")
    }
}
