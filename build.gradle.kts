import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.plugins.signing.SigningExtension
import java.io.File

plugins {
    base
    `maven-publish`
    id("io.freefair.aspectj") version "9.2.0" apply false
    id("io.freefair.aspectj.post-compile-weaving") version "9.2.0" apply false
}

val projectUrl = "https://github.com/wodencafe/leakwatch"
val issuesUrl = "https://github.com/wodencafe/leakwatch/issues"
val scmUrl = "https://github.com/wodencafe/leakwatch.git"

fun configuredText(propertyName: String, vararg environmentNames: String): String? =
    buildList {
        add(providers.gradleProperty(propertyName).orNull)
        environmentNames.forEach { add(providers.environmentVariable(it).orNull) }
    }
        .mapNotNull { it?.trim() }
        .firstOrNull { it.isNotEmpty() }

val defaultProjectVersion = "0.1.0-SNAPSHOT"
val resolvedProjectVersion = configuredText("releaseVersion", "LEAKWATCH_RELEASE_VERSION") ?: defaultProjectVersion

val githubPackagesRepository = configuredText("githubPackagesRepository", "GITHUB_REPOSITORY") ?: "wodencafe/leakwatch"
val githubPackagesUsername = configuredText("githubPackagesUsername", "GITHUB_ACTOR")
val githubPackagesPassword = configuredText("githubPackagesPassword", "GITHUB_TOKEN", "GH_PACKAGES_TOKEN")

val leakwatchSigningRequired = configuredText("leakwatchSigningRequired", "LEAKWATCH_SIGNING_REQUIRED")?.toBoolean() ?: false
val leakwatchUseGpgCmd = configuredText("leakwatchUseGpgCmd", "LEAKWATCH_USE_GPG_CMD")?.toBoolean() ?: false
val signingKeyId = configuredText("signingKeyId", "MAVEN_GPG_KEY_ID")
val signingKey = configuredText("signingKey", "MAVEN_GPG_PRIVATE_KEY")
val signingPassword = configuredText("signingPassword", "MAVEN_GPG_PASSPHRASE")

fun MavenPublication.configureLeakWatchPom(projectName: String) {
    pom {
        name.set(
            when (projectName) {
                "leakwatch-bom" -> "LeakWatch BOM"
                else -> projectName
            }
        )
        description.set(
            when (projectName) {
                "leakwatch-annotations" -> "Public annotations and enums for LeakWatch lifecycle and retention tracking."
                "leakwatch-core" -> "Core runtime, registry, reporters, cleaner integration, and retention tracking for LeakWatch."
                "leakwatch-aspectj" -> "AspectJ integration for LeakWatch constructor registration and cleanup interception."
                "leakwatch-jfr" -> "Optional JFR event reporter and mappings for LeakWatch."
                "leakwatch-testkit" -> "Testing helpers for LeakWatch unit tests and GC-tolerant integration tests."
                "leakwatch-instrumentation" -> "Optional Java agent exposing Instrumentation-backed shallow-size estimates for LeakWatch."
                "leakwatch-jol" -> "Optional JOL-backed shallow-size estimator module for LeakWatch."
                "leakwatch-metrics" -> "Optional lightweight in-process counters for LeakWatch reports."
                "leakwatch-micrometer" -> "Optional Micrometer reporter module for LeakWatch observability."
                "leakwatch-bom" -> "Bill of materials for aligning LeakWatch module versions in consumer builds."
                else -> "LeakWatch module $projectName."
            }
        )
        url.set(projectUrl)

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        inceptionYear.set("2026")

        developers {
            developer {
                id.set("wodencafe")
                name.set("WodenCafe")
                email.set("wodencafe@woden.cafe")
                url.set("https://github.com/wodencafe")
            }
        }

        issueManagement {
            system.set("GitHub Issues")
            url.set(issuesUrl)
        }

        scm {
            url.set(projectUrl)
            connection.set("scm:git:$scmUrl")
            developerConnection.set("scm:git:$scmUrl")
        }
    }
}

fun Project.configurePublicationSigning() {
    val shouldConfigureSigning = leakwatchSigningRequired || leakwatchUseGpgCmd || !signingKey.isNullOrBlank()
    if (!shouldConfigureSigning) {
        return
    }

    val publishing = extensions.getByType(PublishingExtension::class.java)
    extensions.configure(SigningExtension::class.java) {
        isRequired = leakwatchSigningRequired

        when {
            !signingKey.isNullOrBlank() -> {
                val password = signingPassword
                    ?: throw GradleException(
                        "LeakWatch signing key material was supplied, but no signing password was configured. " +
                            "Set signingPassword (or MAVEN_GPG_PASSPHRASE)."
                    )
                if (!signingKeyId.isNullOrBlank()) {
                    useInMemoryPgpKeys(signingKeyId, signingKey, password)
                } else {
                    useInMemoryPgpKeys(signingKey, password)
                }
            }

            leakwatchUseGpgCmd -> useGpgCmd()

            leakwatchSigningRequired -> throw GradleException(
                "LeakWatch signing was requested, but no signing key configuration was supplied. " +
                    "Provide signingKey/signingPassword (or MAVEN_GPG_PRIVATE_KEY/MAVEN_GPG_PASSPHRASE), " +
                    "or enable leakwatchUseGpgCmd=true for a local GPG-agent based release."
            )
        }

        sign(publishing.publications)
    }
}


fun Project.isSamplesProject(): Boolean =
    path == ":samples" || path.startsWith(":samples:")

allprojects {
    group = "cafe.woden"
    version = resolvedProjectVersion

    repositories {
        mavenCentral()
    }
}

subprojects {
    val isSampleProject = isSamplesProject()
    val isBomProject = name == "leakwatch-bom"
    val isPublishedModule = !isSampleProject

    if (!isBomProject) {
        apply(plugin = "java-library")
    }

    if (isPublishedModule) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        extensions.configure<PublishingExtension>("publishing") {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/$githubPackagesRepository")
                    credentials {
                        username = githubPackagesUsername
                        password = githubPackagesPassword
                    }
                }
            }
        }
    }

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension>("java") {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release.set(17)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        dependencies {
            "testImplementation"(platform("org.junit:junit-bom:6.0.3"))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }
    }

    pluginManager.withPlugin("java-library") {
        if (isPublishedModule) {
            extensions.configure<PublishingExtension>("publishing") {
                publications {
                    create<MavenPublication>("mavenJava") {
                        from(components["java"])
                        artifactId = project.name
                        configureLeakWatchPom(project.name)
                    }
                }
            }
            configurePublicationSigning()
        }
    }

    pluginManager.withPlugin("java-platform") {
        if (isPublishedModule) {
            extensions.configure<PublishingExtension>("publishing") {
                publications {
                    create<MavenPublication>("mavenJava") {
                        from(components["javaPlatform"])
                        artifactId = project.name
                        configureLeakWatchPom(project.name)
                    }
                }
            }
            configurePublicationSigning()
        }
    }
}

tasks.register("publishReleaseModulesToMavenLocal") {
    group = "publishing"
    description = "Publishes all non-sample LeakWatch modules and the BOM to the local Maven repository."

    dependsOn(
        subprojects
            .filter { !it.isSamplesProject() }
            .map { "${it.path}:publishToMavenLocal" }
    )
}

tasks.register("publishReleaseModulesToGitHubPackages") {
    group = "publishing"
    description = "Publishes all non-sample LeakWatch modules and the BOM to the configured GitHub Packages Maven repository."

    dependsOn(
        subprojects
            .filter { !it.isSamplesProject() }
            .map { "${it.path}:publishAllPublicationsToGitHubPackagesRepository" }
    )
}

fun pomFileFor(project: Project): File =
    project.layout.buildDirectory.file("publications/mavenJava/pom-default.xml").get().asFile

fun publishedJar(project: Project, classifier: String): File =
    project.layout.buildDirectory.file("libs/${project.name}-${project.version}-${classifier}.jar").get().asFile

val publishedProjectsProvider = provider {
    subprojects.filter { !it.isSamplesProject() }
}

val consumerSmokeTest = tasks.register<GradleBuild>("consumerSmokeTest") {
    group = "verification"
    description = "Publishes LeakWatch modules to Maven Local and verifies external consumption from a nested smoke-test build."

    dependsOn("publishReleaseModulesToMavenLocal")
    dir = file("smoke-tests/consumer-bom")
    tasks = listOf("clean", "test")
    startParameter.projectProperties = mapOf("leakwatchVersion" to project.version.toString())
}

val verifyReleaseReadiness = tasks.register("verifyReleaseReadiness") {
    group = "verification"
    description = "Verifies release metadata, generated artifacts, repository docs, and a nested consumer smoke test."

    dependsOn("check")
    dependsOn(consumerSmokeTest)

    val publishedProjects = publishedProjectsProvider.get()
    val releaseArtifactTaskPaths = publishedProjects.flatMap { project ->
        if (project.name == "leakwatch-bom") {
            listOfNotNull(project.tasks.findByName("generatePomFileForMavenJavaPublication")?.path)
        } else {
            listOfNotNull(
                project.tasks.findByName("generatePomFileForMavenJavaPublication")?.path,
                project.tasks.findByName("sourcesJar")?.path,
                project.tasks.findByName("javadocJar")?.path
            )
        }
    }
    dependsOn(releaseArtifactTaskPaths)

    doLast {
        val requiredRepoFiles = listOf(
            "LICENSE",
            "README.md",
            ".github/workflows/ci.yml",
            ".github/workflows/publish.yml",
            ".github/pull_request_template.md",
            ".github/dependabot.yml"
        )
        val missingRepoFiles = requiredRepoFiles.filter { !rootProject.file(it).isFile }
        check(missingRepoFiles.isEmpty()) {
            "Missing repository files required for GitHub/release hygiene: ${missingRepoFiles.joinToString()}"
        }

        publishedProjects.forEach { project ->
            val pomFile = pomFileFor(project)
            check(pomFile.isFile) { "Missing generated POM for ${project.path}: ${pomFile.absolutePath}" }
            val pomText = pomFile.readText()
            val requiredPomSnippets = listOf(
                "<name>",
                "<description>",
                "<url>${projectUrl}</url>",
                "<license>",
                "<developers>",
                "<email>wodencafe@woden.cafe</email>",
                "<issueManagement>",
                "<scm>"
            )
            val missingPomSnippets = requiredPomSnippets.filter { it !in pomText }
            check(missingPomSnippets.isEmpty()) {
                "POM metadata check failed for ${project.path}; missing: ${missingPomSnippets.joinToString()}"
            }

            if (project.plugins.hasPlugin("java")) {
                val missingClassifiedJars = listOf("sources", "javadoc").filter { classifier ->
                    !publishedJar(project, classifier).isFile
                }
                check(missingClassifiedJars.isEmpty()) {
                    "Missing generated classified jars for ${project.path}: ${missingClassifiedJars.joinToString()}"
                }
            }
        }

        if (providers.gradleProperty("release").map(String::toBoolean).orElse(false).get()) {
            check(!project.version.toString().endsWith("-SNAPSHOT")) {
                "Release verification was requested, but version ${project.version} is still a snapshot."
            }
        }
    }
}

tasks.register("printPublishedCoordinates") {
    group = "help"
    description = "Prints the Maven coordinates for the publishable LeakWatch modules."

    doLast {
        subprojects
            .filter { !it.isSamplesProject() }
            .sortedBy { it.name }
            .forEach { println("${it.group}:${it.name}:${it.version}") }
    }
}
