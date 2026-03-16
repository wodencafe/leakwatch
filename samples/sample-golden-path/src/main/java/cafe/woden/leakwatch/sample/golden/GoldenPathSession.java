package cafe.woden.leakwatch.sample.golden;

import cafe.woden.leakwatch.annotations.CleanupMethod;
import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(captureStackTrace = true, tags = {"sample", "golden-path", "session"})
public final class GoldenPathSession implements AutoCloseable {
    private final String name;

    public GoldenPathSession(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    @CleanupMethod
    public void close() {
        System.out.println("Closed golden-path session " + name);
    }
}
