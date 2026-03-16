package cafe.woden.leakwatch.sample.retention;

import cafe.woden.leakwatch.annotations.RetentionSuspect;

@RetentionSuspect(maxLiveInstances = 3, captureStackTrace = true, tags = { "sample", "retention" })
public final class RetentionTrackedObject {
    private final byte[] payload = new byte[1024];
}
