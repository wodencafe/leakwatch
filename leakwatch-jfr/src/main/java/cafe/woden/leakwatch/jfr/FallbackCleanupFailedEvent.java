package cafe.woden.leakwatch.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
/**
 * JFR event for a detached fallback cleanup action that failed.
 */

@Name("cafe.woden.leakwatch.FallbackCleanupFailed")
@Label("LeakWatch fallback cleanup failed")
@Category({"LeakWatch", "Lifecycle"})
@Description("Emitted when detached fallback cleanup throws after GC-before-cleanup.")
public final class FallbackCleanupFailedEvent extends AbstractLeakWatchEvent {
}
