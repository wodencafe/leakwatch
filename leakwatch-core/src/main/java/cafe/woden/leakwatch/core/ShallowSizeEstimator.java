package cafe.woden.leakwatch.core;

/**
 * Strategy for estimating an object's shallow size.
 * <p>
 * LeakWatch uses this for optional retention byte budgets. Returning {@code -1} means sizing is not available.
 */
public interface ShallowSizeEstimator {
    /**
     * Returns the estimated shallow size for the supplied instance.
     */
    long estimateShallowSize(Object instance);

    /**
     * Returns whether this estimator is currently usable.
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Returns a short human-readable description of the estimator.
     */
    default String description() {
        return getClass().getName();
    }

    /**
     * Returns the default estimator used when no sizing implementation is available.
     */
    static ShallowSizeEstimator unsupported() {
        return UnsupportedShallowSizeEstimator.INSTANCE;
    }

    enum UnsupportedShallowSizeEstimator implements ShallowSizeEstimator {
        INSTANCE;

        @Override
        public long estimateShallowSize(Object instance) {
            return -1L;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public String description() {
            return "unsupported";
        }
    }
}
