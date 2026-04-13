plugins {
    `java-platform`
}

dependencies {
    constraints {
        api(project(":leakwatch-annotations"))
        api(project(":leakwatch-core"))
        api(project(":leakwatch-aspectj"))
        api(project(":leakwatch-jfr"))
        api(project(":leakwatch-testkit"))
        api(project(":leakwatch-instrumentation"))
        api(project(":leakwatch-jol"))
        api(project(":leakwatch-metrics"))
        api(project(":leakwatch-micrometer"))

        api("org.slf4j:slf4j-api:2.0.17")
        api("org.aspectj:aspectjrt:1.9.25")
        api("org.openjdk.jol:jol-core:0.17")
        api("io.micrometer:micrometer-core:1.16.5")
    }
}
