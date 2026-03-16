package cafe.woden.leakwatch.sample.strict;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakReport;
import cafe.woden.leakwatch.core.LeakReportType;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.testkit.LeakWatchTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrictModeMisconfigurationIntegrationTest {
    private InMemoryLeakReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = LeakWatchTestSupport.configureInMemory(true);
    }

    @Test
    void missingCleanupWarningIsDeduplicatedAndTypeIsNotTracked() {
        new NoCleanupTrackedResource("first");
        new NoCleanupTrackedResource("second");

        List<LeakReport> reports = strictWarningsFor(NoCleanupTrackedResource.class);
        assertEquals(1, reports.size());
        assertTrue(reports.get(0).message().contains("was not lifecycle-tracked"));
        assertEquals(0, LeakWatchRuntime.registry().activeCount());
    }

    @Test
    void missingFallbackStateProviderEmitsStrictWarningButExplicitCleanupStillWorks() throws Exception {
        MissingFallbackStateProviderResource first = new MissingFallbackStateProviderResource("strict-missing-provider-first");
        MissingFallbackStateProviderResource second = new MissingFallbackStateProviderResource("strict-missing-provider-second");
        Path firstPath = first.path();
        Path secondPath = second.path();

        first.close();
        second.close();

        List<LeakReport> reports = strictWarningsFor(MissingFallbackStateProviderResource.class);
        assertEquals(1, reports.size());
        assertTrue(reports.get(0).message().contains("does not implement FallbackCleanupStateProvider"));
        assertFalse(Files.exists(firstPath));
        assertFalse(Files.exists(secondPath));
        assertEquals(0, LeakWatchRuntime.registry().activeCount());
    }

    @Test
    void selfRetainingFallbackStateEmitsStrictWarningButExplicitCleanupStillWorks() throws Exception {
        SelfRetainingFallbackResource resource = new SelfRetainingFallbackResource("strict-self-retaining");
        Path path = resource.path();
        resource.close();

        List<LeakReport> reports = strictWarningsFor(SelfRetainingFallbackResource.class);
        assertEquals(1, reports.size());
        assertTrue(reports.get(0).message().contains("returned the tracked instance itself"));
        assertFalse(Files.exists(path));
        assertEquals(0, LeakWatchRuntime.registry().activeCount());
    }

    @Test
    void explodingFallbackStateEmitsStrictWarningButExplicitCleanupStillWorks() throws Exception {
        ExplodingFallbackStateResource resource = new ExplodingFallbackStateResource("strict-state-throws");
        Path path = resource.path();
        resource.close();

        List<LeakReport> reports = strictWarningsFor(ExplodingFallbackStateResource.class);
        assertEquals(1, reports.size());
        assertTrue(reports.get(0).message().contains("leakWatchFallbackCleanupState() threw"));
        assertFalse(Files.exists(path));
        assertEquals(0, LeakWatchRuntime.registry().activeCount());
    }

    @Test
    void constructorBoundFallbackActionEmitsStrictWarningButExplicitCleanupStillWorks() throws Exception {
        ConstructorBoundFallbackActionResource resource = new ConstructorBoundFallbackActionResource("strict-action-instantiation");
        Path path = resource.path();
        resource.close();

        List<LeakReport> reports = strictWarningsFor(ConstructorBoundFallbackActionResource.class);
        assertEquals(1, reports.size());
        assertTrue(reports.get(0).message().contains("could not be instantiated with a no-arg constructor"));
        assertFalse(Files.exists(path));
        assertEquals(0, LeakWatchRuntime.registry().activeCount());
    }

    private List<LeakReport> strictWarningsFor(Class<?> type) {
        return reporter.snapshot().stream()
            .filter(report -> report.type() == LeakReportType.STRICT_MODE_WARNING)
            .filter(report -> report.className().equals(type.getName()))
            .toList();
    }
}
