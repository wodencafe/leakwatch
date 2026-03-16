package cafe.woden.leakwatch.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
/**
 * JFR event for a strict-mode configuration warning.
 */

@Name("cafe.woden.leakwatch.StrictModeWarning")
@Label("LeakWatch strict-mode warning")
@Category({"LeakWatch", "Diagnostics"})
@Description("Emitted when strict mode detects a misconfigured tracked type.")
public final class StrictModeWarningEvent extends AbstractLeakWatchEvent {
}
