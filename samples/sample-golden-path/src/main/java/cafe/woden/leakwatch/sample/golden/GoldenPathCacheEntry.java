package cafe.woden.leakwatch.sample.golden;

import cafe.woden.leakwatch.annotations.RetentionSuspect;
import cafe.woden.leakwatch.annotations.Severity;

@RetentionSuspect(
    maxLiveInstances = 1,
    maxApproxShallowBytes = 24,
    captureStackTrace = true,
    severity = Severity.ERROR,
    tags = {"sample", "golden-path", "retention"}
)
public final class GoldenPathCacheEntry {
    private final byte[] payload = new byte[4096];

    public int payloadLength() {
        return payload.length;
    }
}
