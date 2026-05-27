package tapstotrips.csv;

import org.junit.jupiter.api.Test;
import tapstotrips.model.Tap;
import tapstotrips.model.TapType;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TapCsvReaderTest {

    private final TapCsvReader reader = new TapCsvReader();

    @Test
    void readsSpecExample_withTrailingSpacesAfterCommas() throws IOException {
        String csv = """
                ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
                1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5500005555555559
                2, 22-01-2023 13:05:00, OFF, Stop2, Company1, Bus37, 5500005555555559
                """;

        List<Tap> taps = reader.read(new StringReader(csv));

        assertThat(taps).hasSize(2);
        Tap first = taps.getFirst();
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.dateTime()).isEqualTo(LocalDateTime.of(2023, 1, 22, 13, 0, 0));
        assertThat(first.type()).isEqualTo(TapType.ON);
        assertThat(first.stopId()).isEqualTo("Stop1");
        assertThat(first.companyId()).isEqualTo("Company1");
        assertThat(first.busId()).isEqualTo("Bus37");
        assertThat(first.pan()).isEqualTo("5500005555555559");
        assertThat(taps.getLast().type()).isEqualTo(TapType.OFF);
    }

    @Test
    void handlesMixedCaseHeaders() throws IOException {
        String csv = """
                id,datetimeutc,taptype,stopid,companyid,busid,pan
                1,22-01-2023 13:00:00,ON,Stop1,Company1,Bus37,5500005555555559
                """;

        List<Tap> taps = reader.read(new StringReader(csv));

        assertThat(taps).hasSize(1);
        assertThat(taps.getFirst().id()).isEqualTo(1L);
    }

    @Test
    void emptyFile_returnsEmptyList() throws IOException {
        String csv = "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN\n";

        List<Tap> taps = reader.read(new StringReader(csv));

        assertThat(taps).isEmpty();
    }

    @Test
    void malformedDateTime_throws() {
        String csv = """
                ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
                1, not-a-date, ON, Stop1, Company1, Bus37, 5500005555555559
                """;

        assertThatThrownBy(() -> reader.read(new StringReader(csv)))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    @Test
    void malformedTapType_throws() {
        String csv = """
                ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
                1, 22-01-2023 13:00:00, MAYBE, Stop1, Company1, Bus37, 5500005555555559
                """;

        assertThatThrownBy(() -> reader.read(new StringReader(csv)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
