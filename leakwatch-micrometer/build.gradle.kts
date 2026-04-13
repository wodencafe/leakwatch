plugins {
    `java-library`
}

dependencies {
    api(project(":leakwatch-core"))
    api("io.micrometer:micrometer-core:1.16.5")
}
