# Configuration

LeakWatch supports programmatic configuration, system-property mapping, and environment-variable mapping.

## Programmatic configuration

```java
LeakWatchConfig config = LeakWatchConfig.defaults();
LeakWatchRuntime.configure(config, reporter);
```

## Current fields

`LeakWatchConfig` currently exposes:

- `enabled`
- `defaultCaptureStackTraces`
- `strictMode`
- `defaultConventionCleanupDetection`
- `excludedPackages`
- `retainedAfterCleanupDiagnosticHook` (programmatic-only, defaults to a no-op singleton)

## Defaults

`LeakWatchConfig.defaults()` currently means:

- LeakWatch enabled
- stack trace capture off by default
- strict mode off
- conventional cleanup detection on by default
- common JDK and LeakWatch internal packages excluded
- retained-after-cleanup diagnostics hook disabled by default

## Typical test configuration

```java
LeakWatchRuntime.configure(
    new LeakWatchConfig(
        true,
        false,
        true,
        true,
        LeakWatchConfig.defaults().excludedPackages()
    ),
    new InMemoryLeakReporter()
);
```

That enables strict mode without turning on allocation stack traces globally.

For optional retained-after-cleanup diagnostic escalation, keep using the default config shape and add the hook fluently:

```java
LeakWatchConfig config = LeakWatchConfig.defaults()
    .withRetainedAfterCleanupDiagnosticHook(context -> {
        System.out.println("retained after cleanup: " + context.className());
    });
```

## System-property and environment configuration

Applications that prefer startup flags can map a small set of JVM system properties into `LeakWatchConfig`:

```java
LeakWatchRuntime.configureFromSystemProperties(new Slf4jLeakReporter());
```

Applications that deploy through environment variables can also load config without custom binding glue:

```java
LeakWatchRuntime.configureFromEnvironment(new Slf4jLeakReporter());
```

If you want both, with JVM properties winning over environment variables, use:

```java
LeakWatchRuntime.configureFromSystemPropertiesAndEnvironment(new Slf4jLeakReporter());
```

That currently reads these property keys (and their environment-variable equivalents such as `LEAKWATCH_ENABLED` and `LEAKWATCH_CAPTURE_STACK_TRACES`):

- `leakwatch.enabled`
- `leakwatch.captureStackTraces`
- `leakwatch.strictMode`
- `leakwatch.conventionCleanupDetection`
- `leakwatch.additionalExcludedPackages`

Example:

```bash
java \
  -Dleakwatch.enabled=true \
  -Dleakwatch.strictMode=true \
  -Dleakwatch.captureStackTraces=true \
  -Dleakwatch.additionalExcludedPackages=com.example.generated,com.example.thirdparty \
  -jar app.jar
```

`leakwatch.additionalExcludedPackages` is additive. It extends the default excluded package set rather than replacing it, so LeakWatch internal packages stay excluded.

Boolean properties currently accept `true` or `false` case-insensitively. Invalid values fall back to the default.

Environment variable names are derived by uppercasing the property name, replacing dots with underscores, and splitting camel-case boundaries. For example:

- `leakwatch.strictMode` -> `LEAKWATCH_STRICT_MODE`
- `leakwatch.captureStackTraces` -> `LEAKWATCH_CAPTURE_STACK_TRACES`
- `leakwatch.rollingJson.maxBytes` -> `LEAKWATCH_ROLLING_JSON_MAX_BYTES`

## System-property reporter selection

For core built-in reporters, LeakWatch can also create the reporter chain from configuration:

```java
LeakWatchRuntime.configureFromSystemProperties();
```

```java
LeakWatchRuntime.configureFromSystemPropertiesAndEnvironment();
```

Supported reporter ids in `leakwatch.reporters` / `LEAKWATCH_REPORTERS`:

- `slf4j`
- `json`
- `rolling-json`
- `jfr` (requires `leakwatch-jfr` on the runtime classpath)

The value is comma-separated and order-preserving. Duplicate ids are ignored.

Examples:

```bash
java \
  -Dleakwatch.reporters=slf4j,json \
  -Dleakwatch.json.path=build/leakwatch/reports.ndjson \
  -jar app.jar
```

```bash
java \
  -Dleakwatch.reporters=slf4j,rolling-json \
  -Dleakwatch.rollingJson.path=build/leakwatch/reports.ndjson \
  -Dleakwatch.rollingJson.maxBytes=262144 \
  -Dleakwatch.rollingJson.maxArchives=5 \
  -jar app.jar
```

Current reporter properties / environment variables:

- `leakwatch.reporters` / `LEAKWATCH_REPORTERS`
- `leakwatch.json.path` / `LEAKWATCH_JSON_PATH`
- `leakwatch.rollingJson.path` / `LEAKWATCH_ROLLING_JSON_PATH`
- `leakwatch.rollingJson.maxBytes` / `LEAKWATCH_ROLLING_JSON_MAX_BYTES`
- `leakwatch.rollingJson.maxArchives` / `LEAKWATCH_ROLLING_JSON_MAX_ARCHIVES`

Reporter path properties fail fast when a requested sink is missing required configuration. Optional-module reporter ids such as `jfr` are instantiated reflectively so `leakwatch-core` still avoids a compile-time dependency on `leakwatch-jfr`.

## Reporter composition

Multiple reporters can be used together through `CompositeLeakReporter`:

```java
LeakWatchRuntime.configure(
    LeakWatchConfig.defaults(),
    new CompositeLeakReporter(
        new Slf4jLeakReporter(),
        new JfrLeakReporter(),
        new LeakReportMetrics()
    )
);
```

## Metrics aggregation

If you want cheap in-process counters without wiring a full metrics backend yet, add `leakwatch-metrics` and compose `LeakReportMetrics` into the reporter chain.

## Fallback cleanup configuration posture

Detached fallback cleanup is currently annotation-driven rather than globally configurable.

That is deliberate for v1:

- explicit cleanup stays primary
- fallback cleanup stays opt-in per type
- dangerous global pseudo-finalizer behavior stays out of scope

## Future direction

The config surface is still intentionally small. There is room for richer property binding and framework-specific adapters later, but the v1 posture remains explicit and easy to reason about.

## Optional shallow-size backend discovery

The default `LeakWatchRuntime.configure(...)` path now auto-discovers `ShallowSizeEstimator` implementations from the runtime classpath.

Priority order:

1. `leakwatch-instrumentation`
2. `leakwatch-jol`
3. unsupported/no byte-budget sizing

That keeps byte-budget retention opt-in without forcing every application to wire an estimator manually.

See the root `README.md` for the side-by-side launch-mode summary and the sample modules that demonstrate each runtime posture.
