package cafe.woden.leakwatch.core;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitingLeakReporterTest {
    @Test
    void forwardsReportsUntilWindowLimitAndSuppressesAdditionalMatches() {
        InMemoryLeakReporter delegate = new InMemoryLeakReporter();
        MutableClock clock = new MutableClock(Instant.parse("2026-03-16T00:00:00Z"));
        RateLimitingLeakReporter reporter = new RateLimitingLeakReporter(
            delegate,
            Duration.ofMinutes(1),
            2,
            RateLimitingLeakReporter.classAndTypeKey(),
            clock
        );

        reporter.report(sampleReport(1L, LeakReportType.GC_WITHOUT_CLEANUP, "example.One"));
        reporter.report(sampleReport(2L, LeakReportType.GC_WITHOUT_CLEANUP, "example.One"));
        reporter.report(sampleReport(3L, LeakReportType.GC_WITHOUT_CLEANUP, "example.One"));

        assertEquals(2, delegate.snapshot().size());
        assertEquals(1L, reporter.suppressedReports());
        assertEquals(1L, reporter.snapshot().suppressedByKey().get("GC_WITHOUT_CLEANUP|example.One"));
    }

    @Test
    void newWindowAllowsReportsAgain() {
        InMemoryLeakReporter delegate = new InMemoryLeakReporter();
        MutableClock clock = new MutableClock(Instant.parse("2026-03-16T00:00:00Z"));
        RateLimitingLeakReporter reporter = new RateLimitingLeakReporter(
            delegate,
            Duration.ofSeconds(30),
            1,
            RateLimitingLeakReporter.classAndTypeKey(),
            clock
        );

        reporter.report(sampleReport(1L, LeakReportType.GC_WITHOUT_CLEANUP, "example.One"));
        reporter.report(sampleReport(2L, LeakReportType.GC_WITHOUT_CLEANUP, "example.One"));
        clock.advance(Duration.ofSeconds(30));
        reporter.report(sampleReport(3L, LeakReportType.GC_WITHOUT_CLEANUP, "example.One"));

        assertEquals(2, delegate.snapshot().size());
        assertEquals(1L, reporter.suppressedReports());
    }

    @Test
    void customKeyFunctionCanRateLimitAcrossClassesByTypeOnly() {
        InMemoryLeakReporter delegate = new InMemoryLeakReporter();
        MutableClock clock = new MutableClock(Instant.parse("2026-03-16T00:00:00Z"));
        RateLimitingLeakReporter reporter = new RateLimitingLeakReporter(
            delegate,
            Duration.ofMinutes(1),
            1,
            RateLimitingLeakReporter.typeOnlyKey(),
            clock
        );

        reporter.report(sampleReport(1L, LeakReportType.GC_WITHOUT_CLEANUP, "example.One"));
        reporter.report(sampleReport(2L, LeakReportType.GC_WITHOUT_CLEANUP, "example.Two"));

        assertEquals(1, delegate.snapshot().size());
        assertEquals(1L, reporter.suppressedReports());
        assertEquals(1L, reporter.snapshot().suppressedByKey().get(LeakReportType.GC_WITHOUT_CLEANUP.name()));
    }

    private static LeakReport sampleReport(long id, LeakReportType type, String className) {
        return new LeakReport(
            type,
            id,
            className,
            Set.of("close"),
            null,
            Instant.parse("2026-03-16T00:00:00Z"),
            Duration.ofMillis(123L),
            Set.of("sample"),
            null,
            "sample message"
        );
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
