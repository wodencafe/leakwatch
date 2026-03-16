rootProject.name = "leakwatch"

include(
    "leakwatch-annotations",
    "leakwatch-core",
    "leakwatch-aspectj",
    "leakwatch-jfr",
    "leakwatch-testkit",
    "leakwatch-instrumentation",
    "leakwatch-jol",
    "leakwatch-metrics",
    "leakwatch-micrometer",
    "leakwatch-bom",
    "samples:sample-closeable",
    "samples:sample-golden-path",
    "samples:sample-dispose",
    "samples:sample-retention",
    "samples:sample-retention-jol",
    "samples:sample-retention-instrumentation",
    "samples:sample-fallback",
    "samples:sample-strict",
    "samples:sample-observability",
    "samples:sample-micrometer",
    "samples:sample-jfr",
    "samples:sample-rolling-json",
    "samples:sample-property-configured-jfr"
)
