package cafe.woden.leakwatch.sample.dispose;

import cafe.woden.leakwatch.annotations.LeakTracked;

@LeakTracked(captureStackTrace = true, tags = { "sample", "dispose" })
public final class DisposableWindow {
    private final String name;

    public DisposableWindow(String name) {
        this.name = name;
    }

    public void dispose() {
        System.out.println("Disposed " + name);
    }
}
