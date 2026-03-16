package cafe.woden.leakwatch.sample.retention;

import java.util.ArrayList;
import java.util.List;

public final class RetentionSampleApp {
    public static void main(String[] args) throws Exception {
        List<RetentionTrackedObject> objects = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            objects.add(new RetentionTrackedObject());
        }

        System.out.println("Created " + objects.size() + " tracked objects.");

        objects.clear();
        System.gc();
        Thread.sleep(1500L);

        System.out.println("Retention sample complete.");
    }
}
