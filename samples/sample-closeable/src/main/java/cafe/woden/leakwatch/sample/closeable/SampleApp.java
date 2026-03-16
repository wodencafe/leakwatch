package cafe.woden.leakwatch.sample.closeable;

public final class SampleApp {
    public static void main(String[] args) throws Exception {
        SampleResource cleaned = new SampleResource("cleaned-explicit");
        cleaned.close();

        ConventionalCleanupResource conventional = new ConventionalCleanupResource("cleaned-conventional");
        conventional.shutdown();

        new SampleResource("leaked");

        System.gc();
        Thread.sleep(1500L);

        System.out.println("Sample complete.");
    }
}
