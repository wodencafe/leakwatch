package cafe.woden.leakwatch.smoketest;

import cafe.woden.leakwatch.core.InMemoryLeakReporter;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.testkit.LeakWatchTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishedConsumerIntegrationTest {
    private InMemoryLeakReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = LeakWatchTestSupport.configureInMemory();
    }

    @Test
    void publishedArtifactsCanRegisterAndCleanupAnnotatedType() {
        PublishedAnnotatedResource resource = new PublishedAnnotatedResource();

        assertEquals(1, LeakWatchRuntime.registry().activeCount());

        resource.release();

        assertEquals(0, LeakWatchRuntime.registry().activeCount());
        assertTrue(reporter.snapshot().isEmpty());
    }

    @Test
    void publishedArtifactsAlsoSupportConventionalCleanupDetection() {
        PublishedConventionalResource resource = new PublishedConventionalResource();

        assertEquals(1, LeakWatchRuntime.registry().activeCount());

        resource.close();

        assertEquals(0, LeakWatchRuntime.registry().activeCount());
        assertTrue(reporter.snapshot().isEmpty());
    }
}
