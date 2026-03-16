plugins {
    application
    id("io.freefair.aspectj.post-compile-weaving")
}

dependencies {
    implementation(project(":leakwatch-aspectj"))
    implementation(project(":leakwatch-instrumentation"))
    implementation(project(":leakwatch-jol"))
    aspect(project(":leakwatch-aspectj"))
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
    testImplementation(project(":leakwatch-testkit"))
}

val leakwatchInstrumentationJar = project(":leakwatch-instrumentation").tasks.named<Jar>("jar")

tasks.test {
    dependsOn(leakwatchInstrumentationJar)
    doFirst {
        systemProperty(
            "leakwatch.instrumentation.jar",
            leakwatchInstrumentationJar.get().archiveFile.get().asFile.absolutePath
        )
    }
}

application {
    mainClass.set("cafe.woden.leakwatch.sample.retentioninstrumentation.InstrumentationRetentionSampleApp")
}


tasks.register<JavaExec>("runWithLeakWatchAgent") {
    group = "application"
    description = "Runs the sample with the leakwatch-instrumentation Java agent enabled"
    dependsOn(leakwatchInstrumentationJar)
    mainClass.set("cafe.woden.leakwatch.sample.retentioninstrumentation.InstrumentationRetentionSampleApp")
    classpath = sourceSets.main.get().runtimeClasspath
    jvmArgs("-javaagent:${leakwatchInstrumentationJar.get().archiveFile.get().asFile.absolutePath}")
}
