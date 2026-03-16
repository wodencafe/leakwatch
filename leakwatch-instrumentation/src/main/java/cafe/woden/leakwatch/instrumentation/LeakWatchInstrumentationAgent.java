package cafe.woden.leakwatch.instrumentation;

import java.lang.instrument.Instrumentation;

/**
 * Java agent entry point that installs instrumentation support for LeakWatch.
 */
public final class LeakWatchInstrumentationAgent {
    private LeakWatchInstrumentationAgent() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        LeakWatchInstrumentation.install(instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        LeakWatchInstrumentation.install(instrumentation);
    }
}
