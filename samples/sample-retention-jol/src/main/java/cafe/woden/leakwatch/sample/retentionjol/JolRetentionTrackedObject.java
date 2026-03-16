package cafe.woden.leakwatch.sample.retentionjol;

import cafe.woden.leakwatch.annotations.RetentionSuspect;

@RetentionSuspect(
    maxApproxShallowBytes = 24,
    captureStackTrace = true,
    tags = {"sample", "retention", "jol"}
)
public final class JolRetentionTrackedObject {
    private final byte[] payload = new byte[4096];
}
