package cafe.woden.leakwatch.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * {@link LeakReporter} that writes newline-delimited JSON and rotates the output file when it grows too large.
 */
public final class RollingLeakReportJsonFileReporter implements LeakReporter {
    private final Path outputFile;
    private final long maxBytes;
    private final int maxArchives;
    private final Object lock = new Object();

    public RollingLeakReportJsonFileReporter(Path outputFile, long maxBytes, int maxArchives) {
        this.outputFile = Objects.requireNonNull(outputFile, "outputFile");
        if (maxBytes <= 0L) {
            throw new IllegalArgumentException("maxBytes must be greater than zero.");
        }
        if (maxArchives < 0) {
            throw new IllegalArgumentException("maxArchives must be zero or greater.");
        }
        this.maxBytes = maxBytes;
        this.maxArchives = maxArchives;
    }

    public Path outputFile() {
        return outputFile;
    }

    public long maxBytes() {
        return maxBytes;
    }

    public int maxArchives() {
        return maxArchives;
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
                if (Files.isDirectory(outputFile)) {
                    throw new IOException("Output path is a directory: " + outputFile);
                }

                rotateIfNeeded(line);

                Files.writeString(
                    outputFile,
                    line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write rolling LeakWatch report JSON to " + outputFile, e);
        }
    }

    private void rotateIfNeeded(String nextLine) throws IOException {
        if (!Files.exists(outputFile)) {
            return;
        }

        long currentSize = Files.size(outputFile);
        if (currentSize + nextLine.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= maxBytes) {
            return;
        }

        if (maxArchives == 0) {
            Files.deleteIfExists(outputFile);
            return;
        }

        Path oldestArchive = archivePath(maxArchives);
        Files.deleteIfExists(oldestArchive);

        for (int index = maxArchives - 1; index >= 1; index--) {
            Path source = archivePath(index);
            if (Files.exists(source)) {
                Files.move(source, archivePath(index + 1), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        Files.move(outputFile, archivePath(1), StandardCopyOption.REPLACE_EXISTING);
    }

    private Path archivePath(int index) {
        return outputFile.resolveSibling(outputFile.getFileName() + "." + index);
    }
}
