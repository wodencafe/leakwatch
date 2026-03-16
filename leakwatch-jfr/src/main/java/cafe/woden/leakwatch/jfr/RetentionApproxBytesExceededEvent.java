package cafe.woden.leakwatch.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
/**
 * JFR event for a retention-suspect type that crossed its shallow-byte budget.
 */

@Name("cafe.woden.leakwatch.RetentionApproxBytesExceeded")
@Label("LeakWatch retention approx bytes exceeded")
@Category({"LeakWatch", "Retention"})
@Description("Emitted when a retention-suspect class crosses its approximate shallow-byte budget.")
public final class RetentionApproxBytesExceededEvent extends AbstractLeakWatchEvent {
}
