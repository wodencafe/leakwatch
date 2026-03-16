package cafe.woden.leakwatch.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * {@link LeakReporter} wrapper that limits how many similar reports pass through during a time window.
 */
public final class RateLimitingLeakReporter implements LeakReporter {
    private final LeakReporter delegate;
    private final Duration window;
    private final int maxReportsPerWindow;
    private final Function<LeakReport, String> keyFunction;
    private final Clock clock;
    private final ConcurrentMap<String, WindowState> states = new ConcurrentHashMap<>();
    private final AtomicLong suppressedReports = new AtomicLong();

    public RateLimitingLeakReporter(LeakReporter delegate, Duration window, int maxReportsPerWindow) {
        this(delegate, window, maxReportsPerWindow, classAndTypeKey(), Clock.systemUTC());
    }

    public RateLimitingLeakReporter(
        LeakReporter delegate,
        Duration window,
        int maxReportsPerWindow,
        Function<LeakReport, String> keyFunction
    ) {
        this(delegate, window, maxReportsPerWindow, keyFunction, Clock.systemUTC());
    }

    RateLimitingLeakReporter(
        LeakReporter delegate,
        Duration window,
        int maxReportsPerWindow,
        Function<LeakReport, String> keyFunction,
        Clock clock
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.window = Objects.requireNonNull(window, "window");
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be greater than zero.");
        }
        if (maxReportsPerWindow <= 0) {
            throw new IllegalArgumentException("maxReportsPerWindow must be greater than zero.");
        }
        this.maxReportsPerWindow = maxReportsPerWindow;
    }

    /**
     * Groups reports by report type and tracked class name.
     */
    public static Function<LeakReport, String> classAndTypeKey() {
        return report -> report.type().name() + "|" + safeValue(report.className());
    }

    /**
     * Groups reports by report type only.
     */
    public static Function<LeakReport, String> typeOnlyKey() {
        return report -> report.type().name();
    }

    /**
     * Groups reports by type, class, fallback action, and failure class.
     */
    public static Function<LeakReport, String> classTypeAndFallbackOutcomeKey() {
        return report -> report.type().name()
            + "|" + safeValue(report.className())
            + "|" + safeValue(report.fallbackActionClassName())
            + "|" + safeValue(report.failureClassName());
    }

    @Override
    public void report(LeakReport report) {
        Objects.requireNonNull(report, "report");

        String key = Objects.requireNonNull(keyFunction.apply(report), "keyFunction returned null");
        Instant now = clock.instant();
        WindowState state = states.computeIfAbsent(key, ignored -> new WindowState());

        boolean forward;
        synchronized (state) {
            forward = state.onReport(now, window, maxReportsPerWindow);
        }

        if (forward) {
            delegate.report(report);
        } else {
            suppressedReports.incrementAndGet();
        }
    }

    /**
     * Returns the number of reports suppressed by the rate limit.
     */
    public long suppressedReports() {
        return suppressedReports.get();
    }

    /**
     * Returns the current suppression totals, both overall and per key.
     */
    public Snapshot snapshot() {
        LinkedHashMap<String, Long> suppressedByKey = new LinkedHashMap<>();
        states.forEach((key, state) -> {
            long suppressed;
            synchronized (state) {
                suppressed = state.suppressedTotal;
            }
            suppressedByKey.put(key, suppressed);
        });
        return new Snapshot(suppressedReports(), Map.copyOf(suppressedByKey));
    }

    private static String safeValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    public record Snapshot(long suppressedReports, Map<String, Long> suppressedByKey) {
    }

    private static final class WindowState {
        private Instant windowStart;
        private int forwardedInWindow;
        private long suppressedTotal;

        private boolean onReport(Instant now, Duration window, int maxReportsPerWindow) {
            if (windowStart == null || !now.isBefore(windowStart.plus(window))) {
                windowStart = now;
                forwardedInWindow = 1;
                return true;
            }

            if (forwardedInWindow < maxReportsPerWindow) {
                forwardedInWindow++;
                return true;
            }

            suppressedTotal++;
            return false;
        }
    }
}
