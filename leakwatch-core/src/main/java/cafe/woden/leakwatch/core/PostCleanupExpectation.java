package cafe.woden.leakwatch.core;

record PostCleanupExpectation(
    long gracePeriodMillis,
    boolean captureCleanupStackTrace
) {
}
