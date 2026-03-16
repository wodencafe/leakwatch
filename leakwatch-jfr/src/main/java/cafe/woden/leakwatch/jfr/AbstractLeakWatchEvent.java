package cafe.woden.leakwatch.jfr;

import cafe.woden.leakwatch.core.LeakReport;
import jdk.jfr.Description;
import jdk.jfr.Label;

abstract class AbstractLeakWatchEvent extends jdk.jfr.Event {
    @Label("Leak id")
    @Description("Unique leak handle identifier when available.")
    long leakId;

    @Label("Class name")
    @Description("Tracked class associated with the report.")
    String className;

    @Label("Expected cleanup methods")
    @Description("Recognized cleanup methods for the tracked class.")
    String expectedCleanupMethods;

    @Label("Observed cleanup method")
    @Description("Cleanup method observed before terminal state, when present.")
    String observedCleanupMethod;

    @Label("Age millis")
    @Description("Approximate age of the tracked instance or retention signal in milliseconds.")
    long ageMillis;

    @Label("Tags")
    @Description("Comma-separated tags attached to the tracked type.")
    String tags;

    @Label("Message")
    @Description("Human-readable summary of the report.")
    String message;

    @Label("Severity")
    @Description("Severity associated with the report.")
    String severity;

    @Label("Fallback action class")
    @Description("Detached fallback cleanup action class, when applicable.")
    String fallbackActionClassName;

    @Label("Failure class")
    @Description("Failure type for fallback cleanup failures, when applicable.")
    String failureClassName;

    @Label("Failure message")
    @Description("Failure message for fallback cleanup failures, when applicable.")
    String failureMessage;

    @Label("Retention live count")
    @Description("Current weak live-instance count when present.")
    long retentionLiveCount;

    @Label("Retention max live instances")
    @Description("Configured live-instance budget when present.")
    long retentionMaxLiveInstances;

    @Label("Retention approx shallow bytes")
    @Description("Approximate summed shallow bytes when present.")
    long retentionApproxShallowBytes;

    @Label("Retention max approx shallow bytes")
    @Description("Configured approximate shallow-byte budget when present.")
    long retentionMaxApproxShallowBytes;

    @Label("Retention over-budget bytes")
    @Description("Approximate bytes over budget when present.")
    long retentionApproxBytesOverBudget;

    @Label("Cleaned at")
    @Description("Cleanup timestamp when the report concerns post-cleanup reachability.")
    String cleanedAt;

    @Label("Age since cleanup millis")
    @Description("Time between observed cleanup and report emission when present.")
    long ageSinceCleanupMillis;

    @Label("Post-cleanup grace millis")
    @Description("Configured post-cleanup grace period when present.")
    long postCleanupGraceMillis;

    void populateFrom(LeakReport report) {
        this.leakId = report.id();
        this.className = report.className();
        this.expectedCleanupMethods = report.expectedCleanupMethodsCsv();
        this.observedCleanupMethod = report.observedCleanupMethodName() == null ? "" : report.observedCleanupMethodName();
        this.ageMillis = report.age().toMillis();
        this.tags = report.tagsCsv();
        this.message = report.message();
        this.severity = report.severity().name();
        this.fallbackActionClassName = report.fallbackActionClassName() == null ? "" : report.fallbackActionClassName();
        this.failureClassName = report.failureClassName() == null ? "" : report.failureClassName();
        this.failureMessage = report.failureMessage() == null ? "" : report.failureMessage();
        this.retentionLiveCount = report.retentionLiveCount() == null ? -1L : report.retentionLiveCount();
        this.retentionMaxLiveInstances = report.retentionMaxLiveInstances() == null ? -1L : report.retentionMaxLiveInstances();
        this.retentionApproxShallowBytes = report.retentionApproxShallowBytes() == null ? -1L : report.retentionApproxShallowBytes();
        this.retentionMaxApproxShallowBytes = report.retentionMaxApproxShallowBytes() == null ? -1L : report.retentionMaxApproxShallowBytes();
        this.retentionApproxBytesOverBudget = report.retentionApproxBytesOverBudget() == null ? -1L : report.retentionApproxBytesOverBudget();
        this.cleanedAt = report.cleanedAt() == null ? "" : report.cleanedAt().toString();
        this.ageSinceCleanupMillis = report.ageSinceCleanup() == null ? -1L : report.ageSinceCleanup().toMillis();
        this.postCleanupGraceMillis = report.postCleanupGraceMillis() == null ? -1L : report.postCleanupGraceMillis();
    }
}
