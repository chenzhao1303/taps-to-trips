package tapstotrips;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AppIntegrationTest {

    private static final Path SAMPLE_INPUT = Path.of("sample/taps.csv");
    private static final Path SAMPLE_EXPECTED = Path.of("sample/trips.csv");

    @Test
    void endToEnd_producesExpectedTripsCsv(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("trips.csv");

        App.run(SAMPLE_INPUT, output);

        assertThat(output).exists();
        assertThat(Files.readString(output))
                .isEqualToNormalizingNewlines(Files.readString(SAMPLE_EXPECTED));
    }
}
