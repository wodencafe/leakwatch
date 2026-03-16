package cafe.woden.leakwatch.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link LeakReporter} that logs LeakWatch reports through SLF4J.
 */
public final class Slf4jLeakReporter implements LeakReporter {
    private static final Logger log = LoggerFactory.getLogger(Slf4jLeakReporter.class);

    @Override
    public void report(LeakReport report) {
        log.warn(
            "LEAKWATCH {} severity={} id={} class={} expectedCleanupMethods={} observedCleanupMethod={} age={} message={}",
            report.type(),
            report.severity(),
            report.id(),
            report.className(),
            report.expectedCleanupMethods(),
            report.observedCleanupMethodName(),
            report.age(),
            report.message(),
            report.allocationSite()
        );
    }
}
