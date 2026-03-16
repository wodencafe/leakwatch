package cafe.woden.leakwatch.sample.retentioninstrumentation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrumentationRetentionLaunchModeIntegrationTest {
    @Test
    void withoutAgentFallsBackToJol() throws Exception {
        ProcessResult result = runProbe(false);

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("DISCOVERED_ESTIMATOR=org.openjdk.jol.vm.VM.current().sizeOf via leakwatch-jol"), result.output());
        assertTrue(result.output().contains("HAS_APPROX_BYTES_REPORT=true"), result.output());
        assertTrue(result.output().contains("REPORT_CLASS=" + InstrumentationRetentionTrackedObject.class.getName()), result.output());
    }

    @Test
    void withAgentPrefersInstrumentationOverJol() throws Exception {
        ProcessResult result = runProbe(true);

        assertEquals(0, result.exitCode());
        assertTrue(
            result.output().contains(
                "DISCOVERED_ESTIMATOR=java.lang.instrument.Instrumentation#getObjectSize via leakwatch-instrumentation agent"
            ),
            result.output()
        );
        assertTrue(result.output().contains("HAS_APPROX_BYTES_REPORT=true"), result.output());
        assertTrue(result.output().contains("REPORT_CLASS=" + InstrumentationRetentionTrackedObject.class.getName()), result.output());
    }

    private static ProcessResult runProbe(boolean withAgent) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable().toString());
        if (withAgent) {
            command.add("-javaagent:" + System.getProperty("leakwatch.instrumentation.jar"));
        }
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(InstrumentationRetentionProbeMain.class.getName());

        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();

        try (InputStream inputStream = process.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            inputStream.transferTo(output);
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, output.toString(StandardCharsets.UTF_8));
        }
    }

    private static Path javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java");
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
