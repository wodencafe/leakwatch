package cafe.woden.leakwatch.testkit;

import cafe.woden.leakwatch.core.LeakReport;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Small GC-oriented test helpers for waiting on LeakWatch behavior.
 */
public final class GcAwaiter {
    private GcAwaiter() {
    }

    /**
     * Repeatedly nudges GC until a matching report appears or the timeout expires.
     */
    public static LeakReport awaitReport(
        WeakReference<?> reference,
        Supplier<List<LeakReport>> reportsSupplier,
        Predicate<LeakReport> predicate,
        Duration timeout
    ) throws InterruptedException {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(reportsSupplier, "reportsSupplier");
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(timeout, "timeout");

        long deadline = System.nanoTime() + timeout.toNanos();
        List<byte[]> pressure = new ArrayList<>();

        while (System.nanoTime() < deadline) {
            LeakReport report = reportsSupplier.get().stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
            if (report != null) {
                return report;
            }

            System.gc();
            pressure.add(new byte[128 * 1024]);
            if (pressure.size() > 32) {
                pressure.clear();
            }
            Thread.sleep(100L);

            if (reference.get() == null) {
                report = reportsSupplier.get().stream()
                    .filter(predicate)
                    .findFirst()
                    .orElse(null);
                if (report != null) {
                    return report;
                }
            }
        }

        throw new AssertionError("Timed out waiting for matching leak report. Reports seen: " + reportsSupplier.get());
    }

    /**
     * Repeatedly nudges GC until the supplied condition becomes true or the timeout expires.
     */
    public static void awaitCondition(
        Duration timeout,
        BooleanSupplier condition,
        Supplier<String> failureMessageSupplier
    ) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(failureMessageSupplier, "failureMessageSupplier");

        long deadline = System.nanoTime() + timeout.toNanos();
        List<byte[]> pressure = new ArrayList<>();

        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            System.gc();
            pressure.add(new byte[128 * 1024]);
            if (pressure.size() > 32) {
                pressure.clear();
            }
            Thread.sleep(100L);
        }

        throw new AssertionError(failureMessageSupplier.get());
    }
}
