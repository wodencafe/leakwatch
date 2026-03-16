package cafe.woden.leakwatch.sample.micrometer;

import cafe.woden.leakwatch.core.CompositeLeakReporter;
import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.micrometer.MicrometerLeakReporter;
import cafe.woden.leakwatch.testkit.GcAwaiter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicrometerIntegrationTest {
    private InMemoryLeakReporter inMemory;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        inMemory = new InMemoryLeakReporter();
        registry = new SimpleMeterRegistry();
        LeakWatchRuntime.configure(
            LeakWatchConfig.defaults(),
            new CompositeLeakReporter(
                inMemory,
                new MicrometerLeakReporter(registry)
            )
        );
    }

    @AfterEach
    void tearDown() {
        registry.close();
    }

    @Test
    void leakedTrackedObjectPublishesMicrometerCounters() throws Exception {
        WeakReference<MeteredSampleResource> reference = createLeakedResource();

        LeakReport report = GcAwaiter.awaitReport(
            reference,
            inMemory::snapshot,
            candidate -> candidate.type() == LeakReportType.GC_WITHOUT_CLEANUP,
            Duration.ofSeconds(10)
        );

        assertEquals(1.0d, registry.get(MicrometerLeakReporter.TOTAL_REPORTS_METER).counter().count(), 0.0001d);
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
            registry.get(MicrometerLeakReporter.REPORTS_BY_CLASS_METER)
                .tags(
                    "class_name", MeteredSampleResource.class.getName(),
                    "report_type", LeakReportType.GC_WITHOUT_CLEANUP.name()
                )
                .counter()
                .count(),
            0.0001d
        );
        assertTrue(report.tags().contains("micrometer"));
        assertTrue(report.message().contains("garbage collected before an explicit cleanup method"));
    }

    private WeakReference<MeteredSampleResource> createLeakedResource() {
        MeteredSampleResource leaked = new MeteredSampleResource("leaked-metered-test");
        return new WeakReference<>(leaked);
    }
}
