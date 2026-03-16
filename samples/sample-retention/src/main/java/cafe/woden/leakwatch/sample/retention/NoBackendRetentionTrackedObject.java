package cafe.woden.leakwatch.sample.retention;

import cafe.woden.leakwatch.annotations.RetentionSuspect;

@RetentionSuspect(
    maxApproxShallowBytes = 64,
    captureStackTrace = true,
    tags = {"sample", "retention", "no-backend"}
)
public final class NoBackendRetentionTrackedObject {
    private final byte[] payload = new byte[4096];
}
