package cafe.woden.leakwatch.sample.retentioninstrumentation;

import cafe.woden.leakwatch.core.LeakWatchConfig;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.ShallowSizeEstimators;
import cafe.woden.leakwatch.core.Slf4jLeakReporter;

public final class InstrumentationRetentionSampleApp {
    private InstrumentationRetentionSampleApp() {
    }

    public static void main(String[] args) {
        LeakWatchRuntime.configure(LeakWatchConfig.defaults(), new Slf4jLeakReporter());
        System.out.println("LeakWatch size estimator: " + ShallowSizeEstimators.autoDiscover().description());
        new InstrumentationRetentionTrackedObject();
        new InstrumentationRetentionTrackedObject();
        new InstrumentationRetentionTrackedObject();
    }
}
