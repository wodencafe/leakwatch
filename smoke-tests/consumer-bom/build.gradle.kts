import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java
    id("io.freefair.aspectj.post-compile-weaving") version "9.2.0"
}

val leakwatchVersion = providers.gradleProperty("leakwatchVersion").orElse("0.1.0-SNAPSHOT")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

// Repositories are declared in settings.gradle.kts via dependencyResolutionManagement.
// Do not add project repositories here, because this smoke test intentionally runs
// with FAIL_ON_PROJECT_REPOS to verify clean consumer behavior.

dependencies {
    implementation(platform("cafe.woden:leakwatch-bom:${leakwatchVersion.get()}"))
    implementation("cafe.woden:leakwatch-aspectj")

    // The FreeFair AspectJ plugin resolves its dedicated `aspect` configuration
    // separately from normal implementation/test configurations.
    // Declare the version explicitly here so the nested consumer build proves
    // that published aspect artifacts can still be resolved cleanly from Maven coordinates.
    aspect("cafe.woden:leakwatch-aspectj:${leakwatchVersion.get()}")

    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Gradle 9 requires the JUnit Platform launcher to be present on the test runtime classpath
    // for nested consumer builds like this smoke test.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("cafe.woden:leakwatch-testkit")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
