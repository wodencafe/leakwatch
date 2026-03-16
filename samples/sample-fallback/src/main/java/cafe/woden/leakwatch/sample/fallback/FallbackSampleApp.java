package cafe.woden.leakwatch.sample.fallback;

public final class FallbackSampleApp {
    private FallbackSampleApp() {
    }

    public static void main(String[] args) throws Exception {
        try (TrackedTempFile tracked = new TrackedTempFile("leakwatch-sample")) {
            System.out.println("Created temp file: " + tracked.path());
        }
    }
}
