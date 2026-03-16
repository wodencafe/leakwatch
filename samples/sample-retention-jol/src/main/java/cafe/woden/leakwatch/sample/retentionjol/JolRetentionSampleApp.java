package cafe.woden.leakwatch.sample.retentionjol;

import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.ShallowSizeEstimators;
import cafe.woden.leakwatch.core.Slf4jLeakReporter;

public final class JolRetentionSampleApp {
    private JolRetentionSampleApp() {
    }

    public static void main(String[] args) {
        LeakWatchRuntime.configure(LeakWatchConfig.defaults(), new Slf4jLeakReporter());
        System.out.println("LeakWatch size estimator: " + ShallowSizeEstimators.autoDiscover().description());
        new JolRetentionTrackedObject();
        new JolRetentionTrackedObject();
        new JolRetentionTrackedObject();
    }
}
