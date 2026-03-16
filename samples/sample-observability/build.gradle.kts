plugins {
    application
    id("io.freefair.aspectj.post-compile-weaving")
}

dependencies {
    implementation(project(":leakwatch-aspectj"))
    implementation(project(":leakwatch-metrics"))
    aspect(project(":leakwatch-aspectj"))
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
    testImplementation(project(":leakwatch-testkit"))
}

application {
    mainClass.set("cafe.woden.leakwatch.sample.observability.ObservabilitySampleApp")
}
