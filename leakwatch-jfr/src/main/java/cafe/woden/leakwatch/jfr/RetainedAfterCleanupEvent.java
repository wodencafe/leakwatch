package cafe.woden.leakwatch.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
/**
 * JFR event for an object that stayed reachable after cleanup.
 */

@Name("cafe.woden.leakwatch.RetainedAfterCleanup")
@Label("LeakWatch retained after cleanup")
@Category({"LeakWatch", "Lifecycle"})
@Description("Emitted when a cleaned-up object remains strongly reachable past its post-cleanup grace period.")
public final class RetainedAfterCleanupEvent extends AbstractLeakWatchEvent {
}
