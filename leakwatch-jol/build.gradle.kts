plugins {
    `java-library`
}

dependencies {
    api(project(":leakwatch-core"))
    implementation("org.openjdk.jol:jol-core:0.17")
}
