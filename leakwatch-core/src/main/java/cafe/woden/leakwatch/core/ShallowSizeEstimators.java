package cafe.woden.leakwatch.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

/**
 * Factory methods for discovering and selecting shallow-size estimators.
 */
public final class ShallowSizeEstimators {
    private ShallowSizeEstimators() {
    }

    /**
     * Resolves the best available estimator right now.
     */
    public static ShallowSizeEstimator autoDiscover() {
        ServiceLoader<ShallowSizeEstimatorProvider> loader = ServiceLoader.load(ShallowSizeEstimatorProvider.class);
        List<ShallowSizeEstimatorProvider> providers = new ArrayList<>();
        for (ServiceLoader.Provider<ShallowSizeEstimatorProvider> provider : loader.stream().toList()) {
            try {
                providers.add(provider.get());
            } catch (Throwable ignored) {
                // fall through to the next provider
            }
        }
        return autoDiscover(providers).estimator();
    }

    /**
     * Returns a lazily discovering estimator that resolves the best provider on first use.
     */
    public static ShallowSizeEstimator autoDiscovering() {
        return new AutoDiscoveringShallowSizeEstimator();
    }

    static DiscoveryResult autoDiscover(Iterable<ShallowSizeEstimatorProvider> providers) {
        Objects.requireNonNull(providers, "providers");

        List<ShallowSizeEstimatorProvider> orderedProviders = StreamSupport.stream(providers.spliterator(), false)
            .filter(Objects::nonNull)
            .sorted(
                Comparator.comparingInt(ShallowSizeEstimatorProvider::priority)
                    .reversed()
                    .thenComparing(ShallowSizeEstimatorProvider::name)
            )
            .toList();

        for (ShallowSizeEstimatorProvider provider : orderedProviders) {
            try {
                ShallowSizeEstimator estimator = provider.createEstimator();
                if (estimator != null && estimator.isAvailable()) {
                    return new DiscoveryResult(estimator, provider.name(), provider.priority());
                }
            } catch (Throwable ignored) {
                // fall through to the next provider
            }
        }

        return new DiscoveryResult(ShallowSizeEstimator.unsupported(), "unsupported", Integer.MIN_VALUE);
    }

    record DiscoveryResult(ShallowSizeEstimator estimator, String providerName, int providerPriority) {
    }

    private static final class AutoDiscoveringShallowSizeEstimator implements ShallowSizeEstimator {
        private volatile ShallowSizeEstimator delegate;

        @Override
        public long estimateShallowSize(Object instance) {
            return delegate().estimateShallowSize(instance);
        }

        @Override
        public boolean isAvailable() {
            return delegate().isAvailable();
        }

        @Override
        public String description() {
            return delegate().description();
        }

        private ShallowSizeEstimator delegate() {
            ShallowSizeEstimator current = delegate;
            if (current == null) {
                synchronized (this) {
                    current = delegate;
                    if (current == null) {
                        current = ShallowSizeEstimators.autoDiscover();
                        delegate = current;
                    }
                }
            }
            return current;
        }
    }
}
