package cafe.woden.leakwatch.core;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * {@link LeakReporter} that appends newline-delimited JSON reports to a file.
 */
public final class LeakReportJsonFileReporter implements LeakReporter {
    private final Path outputFile;
    private final Object lock = new Object();

    public LeakReportJsonFileReporter(Path outputFile) {
        this.outputFile = Objects.requireNonNull(outputFile, "outputFile");
    }

    public Path outputFile() {
        return outputFile;
    }

    @Override
    public void report(LeakReport report) {
        Objects.requireNonNull(report, "report");

        String line = LeakReportJsonFormatter.toJson(report) + System.lineSeparator();
        try {
            synchronized (lock) {
                Path parent = outputFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(
                    outputFile,
                    line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
                );
            }
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("Failed to write LeakWatch report JSON to " + outputFile, e);
        }
    }
}
