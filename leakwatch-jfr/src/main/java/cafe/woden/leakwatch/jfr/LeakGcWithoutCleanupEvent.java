package cafe.woden.leakwatch.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
/**
 * JFR event for a tracked object that reached GC without observed cleanup.
 */

@Name("cafe.woden.leakwatch.LeakGcWithoutCleanup")
@Label("LeakWatch GC without cleanup")
@Category({"LeakWatch", "Lifecycle"})
@Description("Emitted when a tracked object becomes phantom reachable before cleanup is observed.")
public final class LeakGcWithoutCleanupEvent extends AbstractLeakWatchEvent {
}
