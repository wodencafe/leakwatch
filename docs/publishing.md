# Publishing

LeakWatch now has a simple signed publishing story aimed at GitHub first:

- local rehearsal through `mavenLocal()`
- signed publishing to GitHub Packages from GitHub Actions
- a version override path for releases and tags

This keeps the repo practical without pretending that GitHub Packages is a full Maven Central replacement. GitHub Packages is the automation path here, but consumers should still expect package-registry authentication rather than anonymous Maven Central-style downloads.

## Publishable modules

These modules are intended to publish as Maven artifacts:

- `leakwatch-annotations`
- `leakwatch-core`
- `leakwatch-aspectj`
- `leakwatch-jfr`
- `leakwatch-testkit`
- `leakwatch-instrumentation`
- `leakwatch-jol`
- `leakwatch-metrics`
- `leakwatch-micrometer`
- `leakwatch-bom`

Sample projects are intentionally not part of the published artifact set.

## Local publishing rehearsal

Use this command from the repo root:

```bash
./gradlew publishReleaseModulesToMavenLocal
```

Useful follow-up command:

```bash
./gradlew printPublishedCoordinates
```

## Signed publishing to GitHub Packages

The repo now exposes a root task for GitHub Packages:

```bash
./gradlew publishReleaseModulesToGitHubPackages \
  -PleakwatchSigningRequired=true \
  -PsigningKey="$(cat /path/to/private-key.asc)" \
  -PsigningPassword=your-passphrase \
  -PgithubPackagesUsername=YOUR_GITHUB_USERNAME \
  -PgithubPackagesPassword=YOUR_GITHUB_TOKEN
```

If you are exporting a dedicated signing subkey instead of a primary secret key, also pass:

```bash
-PsigningKeyId=YOUR_SUBKEY_ID
```

For a local machine release that should use your existing GPG agent instead of an in-memory key, you can use:

```bash
./gradlew publishReleaseModulesToGitHubPackages \
  -PleakwatchSigningRequired=true \
  -PleakwatchUseGpgCmd=true \
  -PgithubPackagesUsername=YOUR_GITHUB_USERNAME \
  -PgithubPackagesPassword=YOUR_GITHUB_TOKEN
```

## GitHub Actions workflow

The repo includes `.github/workflows/publish.yml`.

It can be triggered either:

- manually with `workflow_dispatch`
- automatically when you push a tag like `v0.1.0`

The workflow:

- resolves the publish version from the manual input or tag name
- runs `verifyReleaseReadiness`
- publishes signed artifacts to GitHub Packages

### Expected GitHub secrets

Add these repository secrets before running the publish workflow:

- `MAVEN_GPG_PRIVATE_KEY` - ASCII-armored OpenPGP secret key text
- `MAVEN_GPG_PASSPHRASE` - passphrase for that secret key
- `MAVEN_GPG_KEY_ID` - optional, but recommended when you export a signing subkey

The workflow uses GitHub's built-in token for package publishing, so you do not need to store a separate package token just to publish from Actions.

## Version override

By default the repo version stays at `0.1.0-SNAPSHOT`.

For a real publish, override it without editing the build file:

```bash
./gradlew publishReleaseModulesToGitHubPackages \
  -PreleaseVersion=0.1.0 \
  -PleakwatchSigningRequired=true \
  -PsigningKey="$(cat /path/to/private-key.asc)" \
  -PsigningPassword=your-passphrase \
  -PgithubPackagesUsername=YOUR_GITHUB_USERNAME \
  -PgithubPackagesPassword=YOUR_GITHUB_TOKEN
```

The publish workflow does the same thing automatically when you dispatch it with a version input or push a `v...` tag.

## Recommended consumer usage

Prefer the BOM when using more than one LeakWatch module.

### Gradle Kotlin DSL

```kotlin
dependencies {
    implementation(platform("cafe.woden:leakwatch-bom:0.1.0-SNAPSHOT"))

    implementation("cafe.woden:leakwatch-aspectj")
    aspect("cafe.woden:leakwatch-aspectj:0.1.0-SNAPSHOT")

    implementation("cafe.woden:leakwatch-jfr")
    implementation("cafe.woden:leakwatch-jol")
}
```

### Maven

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>cafe.woden</groupId>
      <artifactId>leakwatch-bom</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>cafe.woden</groupId>
    <artifactId>leakwatch-aspectj</artifactId>
  </dependency>
  <dependency>
    <groupId>cafe.woden</groupId>
    <artifactId>leakwatch-jfr</artifactId>
  </dependency>
</dependencies>
```

## Publication metadata

The Gradle build attaches basic Maven POM metadata for every publishable module:

- artifact name
- description
- project URL
- Apache 2.0 license
- developer id, name, email, and URL
- SCM coordinates
- issue tracker URL

## Consumer smoke test

The repo includes a nested Gradle build at `smoke-tests/consumer-bom` that depends on LeakWatch by published Maven coordinates rather than project dependencies.

Run it from the root with:

```bash
./gradlew consumerSmokeTest
```

That task first publishes all non-sample modules to `mavenLocal()` and then executes the nested smoke-test build. It is meant to catch packaging mistakes such as missing transitive dependencies, incomplete BOM alignment, or weaving setup drift before the project is published publicly.

## Release-readiness verification

For a broader repository-level pass, use:

```bash
./gradlew verifyReleaseReadiness
```

This task checks:

- generated POM metadata for each publishable module
- sources and javadoc jars for Java-based published modules
- the small set of required repository files such as the README, license, CI workflow, and publish workflow
- the nested consumer smoke test

If you want to make it stricter for a real release build, pass `-Prelease=true`. That mode fails if the project version still ends with `-SNAPSHOT`.
