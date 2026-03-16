plugins {
    `java-library`
}

dependencies {
    api(project(":leakwatch-annotations"))
    api("org.slf4j:slf4j-api:2.0.17")
    testImplementation(project(":leakwatch-testkit"))
}
