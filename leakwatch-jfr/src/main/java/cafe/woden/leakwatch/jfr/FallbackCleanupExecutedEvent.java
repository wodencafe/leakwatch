package cafe.woden.leakwatch.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
/**
 * JFR event for a detached fallback cleanup action that ran successfully.
 */

@Name("cafe.woden.leakwatch.FallbackCleanupExecuted")
@Label("LeakWatch fallback cleanup executed")
@Category({"LeakWatch", "Lifecycle"})
@Description("Emitted when detached fallback cleanup executes after GC-before-cleanup.")
public final class FallbackCleanupExecutedEvent extends AbstractLeakWatchEvent {
}
