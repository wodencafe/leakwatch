package cafe.woden.leakwatch.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class ShallowSizeEstimatorsTest {
    @Test
    void prefersHighestPriorityAvailableEstimator() {
        ShallowSizeEstimator preferred = instance -> 64L;
        ShallowSizeEstimator fallback = instance -> 32L;

        ShallowSizeEstimators.DiscoveryResult result = ShallowSizeEstimators.autoDiscover(List.of(
            new TestProvider("fallback", 100, fallback),
            new TestProvider("preferred", 200, preferred)
        ));

        assertSame(preferred, result.estimator());
        assertEquals("preferred", result.providerName());
        assertEquals(200, result.providerPriority());
    }

    @Test
    void fallsBackWhenHigherPriorityEstimatorIsUnavailable() {
        ShallowSizeEstimator unavailable = new ShallowSizeEstimator() {
            @Override
            public long estimateShallowSize(Object instance) {
                return -1L;
            }

            @Override
            public boolean isAvailable() {
                return false;
            }
        };
        ShallowSizeEstimator fallback = instance -> 32L;

        ShallowSizeEstimators.DiscoveryResult result = ShallowSizeEstimators.autoDiscover(List.of(
            new TestProvider("instrumentation", 200, unavailable),
            new TestProvider("jol", 100, fallback)
        ));

        assertSame(fallback, result.estimator());
        assertEquals("jol", result.providerName());
    }

    @Test
    void returnsUnsupportedEstimatorWhenNoProvidersWork() {
        ShallowSizeEstimators.DiscoveryResult result = ShallowSizeEstimators.autoDiscover(List.of(
            new ThrowingProvider(),
            new TestProvider("unavailable", 100, new ShallowSizeEstimator() {
                @Override
                public long estimateShallowSize(Object instance) {
                    return -1L;
                }

                @Override
                public boolean isAvailable() {
                    return false;
                }
            })
        ));

        assertFalse(result.estimator().isAvailable());
        assertEquals("unsupported", result.providerName());
    }

    private record TestProvider(String name, int priority, ShallowSizeEstimator estimator) implements ShallowSizeEstimatorProvider {
        @Override
        public ShallowSizeEstimator createEstimator() {
            return estimator;
        }
    }

    private static final class ThrowingProvider implements ShallowSizeEstimatorProvider {
        @Override
        public String name() {
            return "throwing";
        }

        @Override
        public int priority() {
            return 500;
        }

        @Override
        public ShallowSizeEstimator createEstimator() {
            throw new IllegalStateException("boom");
        }
    }
}
