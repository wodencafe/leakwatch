package cafe.woden.leakwatch.sample.dispose;

public final class DisposeSampleApp {
    public static void main(String[] args) throws Exception {
        DisposableWindow cleaned = new DisposableWindow("cleaned-window");
        cleaned.dispose();

        new DisposableWindow("leaked-window");

        System.gc();
        Thread.sleep(1500L);

        System.out.println("Dispose sample complete.");
    }
}
