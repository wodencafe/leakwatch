plugins {
    `java-library`
}

dependencies {
    api(project(":leakwatch-core"))
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "cafe.woden.leakwatch.instrumentation.LeakWatchInstrumentationAgent",
            "Agent-Class" to "cafe.woden.leakwatch.instrumentation.LeakWatchInstrumentationAgent",
            "Can-Redefine-Classes" to "false",
            "Can-Retransform-Classes" to "false"
        )
    }
}
