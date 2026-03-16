package cafe.woden.leakwatch.micrometer;

import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicrometerLeakReporterTest {
    @Test
    void reporterPublishesCountersForReports() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            MicrometerLeakReporter reporter = new MicrometerLeakReporter(registry);

            reporter.report(report(LeakReportType.GC_WITHOUT_CLEANUP, "example.SocketHandle"));
            reporter.report(report(LeakReportType.STRICT_MODE_WARNING, "example.SocketHandle"));
            reporter.report(fallbackFailureReport("example.TempFile", "example.DeleteFileAction", "java.io.IOException"));

            assertEquals(3.0d, registry.get(MicrometerLeakReporter.TOTAL_REPORTS_METER).counter().count(), 0.0001d);
            assertEquals(
                1.0d,
                registry.get(MicrometerLeakReporter.REPORTS_BY_TYPE_METER)
                    .tag("report_type", LeakReportType.GC_WITHOUT_CLEANUP.name())
                    .counter()
                    .count(),
                0.0001d
            );
            assertEquals(
                1.0d,
                registry.get(MicrometerLeakReporter.REPORTS_BY_TYPE_METER)
                    .tag("report_type", LeakReportType.FALLBACK_CLEANUP_FAILED.name())
                    .counter()
                    .count(),
                0.0001d
            );
            assertEquals(
                1.0d,
                registry.get(MicrometerLeakReporter.REPORTS_BY_CLASS_METER)
                    .tags("class_name", "example.SocketHandle", "report_type", LeakReportType.STRICT_MODE_WARNING.name())
                    .counter()
                    .count(),
                0.0001d
            );
            assertEquals(
                1.0d,
                registry.get(MicrometerLeakReporter.FALLBACK_FAILURES_METER)
                    .tags(
                        "class_name", "example.TempFile",
                        "fallback_action", "example.DeleteFileAction",
                        "failure_class", "java.io.IOException"
                    )
                    .counter()
                    .count(),
                0.0001d
            );
        } finally {
            registry.close();
        }
    }

    private static LeakReport report(LeakReportType type, String className) {
        return new LeakReport(
            type,
            1L,
            className,
            Set.of("close"),
            null,
            Instant.now(),
            Duration.ofMillis(10L),
            Set.of("sample"),
            null,
            type.name()
        );
    }

    private static LeakReport fallbackFailureReport(String className, String fallbackActionClassName, String failureClassName) {
        return new LeakReport(
            LeakReportType.FALLBACK_CLEANUP_FAILED,
            2L,
            className,
            Set.of("close"),
            null,
            Instant.now(),
            Duration.ofMillis(10L),
            Set.of("fallback", "failure"),
            null,
            "fallback cleanup failed",
            fallbackActionClassName,
            failureClassName,
            "boom"
        );
    }
}
