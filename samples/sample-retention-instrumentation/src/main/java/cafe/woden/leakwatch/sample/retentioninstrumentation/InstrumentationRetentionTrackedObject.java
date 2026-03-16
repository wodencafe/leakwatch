package cafe.woden.leakwatch.sample.retentioninstrumentation;

import cafe.woden.leakwatch.annotations.RetentionSuspect;

@RetentionSuspect(
    maxApproxShallowBytes = 24,
    captureStackTrace = true,
    tags = {"sample", "retention", "instrumentation"}
)
public final class InstrumentationRetentionTrackedObject {
    private final byte[] payload = new byte[4096];
}
