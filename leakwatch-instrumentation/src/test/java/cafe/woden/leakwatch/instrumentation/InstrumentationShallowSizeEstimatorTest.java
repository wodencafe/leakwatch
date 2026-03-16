package cafe.woden.leakwatch.instrumentation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrumentationShallowSizeEstimatorTest {
    @AfterEach
    void tearDown() {
        LeakWatchInstrumentation.clear();
    }

    @Test
    void reportsUnavailableUntilAgentInstallsInstrumentation() {
        InstrumentationShallowSizeEstimator estimator = new InstrumentationShallowSizeEstimator();

        assertFalse(estimator.isAvailable());
        assertThrows(IllegalStateException.class, () -> estimator.estimateShallowSize(new Object()));
    }

    @Test
    void delegatesToInstrumentationWhenInstalled() {
        LeakWatchInstrumentation.install(fakeInstrumentation(96L));
        InstrumentationShallowSizeEstimator estimator = new InstrumentationShallowSizeEstimator();

        assertTrue(estimator.isAvailable());
        assertEquals(96L, estimator.estimateShallowSize(new Object()));
    }

    private static Instrumentation fakeInstrumentation(long objectSize) {
        return (Instrumentation) Proxy.newProxyInstance(
            Instrumentation.class.getClassLoader(),
            new Class<?>[]{Instrumentation.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getObjectSize")) {
                    return objectSize;
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == boolean.class) {
                    return false;
                }
                if (returnType == long.class) {
                    return 0L;
                }
                if (returnType == int.class) {
                    return 0;
                }
                return null;
            }
        );
    }
}
