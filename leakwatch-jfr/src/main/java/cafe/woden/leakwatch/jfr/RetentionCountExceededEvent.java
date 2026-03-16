package cafe.woden.leakwatch.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
/**
 * JFR event for a retention-suspect type that crossed its live-count budget.
 */

@Name("cafe.woden.leakwatch.RetentionCountExceeded")
@Label("LeakWatch retention count exceeded")
@Category({"LeakWatch", "Retention"})
@Description("Emitted when a retention-suspect class crosses its live-instance count budget.")
public final class RetentionCountExceededEvent extends AbstractLeakWatchEvent {
}
