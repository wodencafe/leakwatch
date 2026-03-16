plugins {
    `java-library`
    id("io.freefair.aspectj")
}

dependencies {
    api(project(":leakwatch-core"))
    api("org.aspectj:aspectjrt:1.9.25")
}
