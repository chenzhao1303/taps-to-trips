package tapstotrips.csv;

import org.junit.jupiter.api.Test;
import tapstotrips.model.Trip;
import tapstotrips.model.TripStatus;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripCsvWriterTest {

    private final TripCsvWriter writer = new TripCsvWriter();

    @Test
    void writesHeaderAndCompletedRow_inSpecFormat() throws IOException {
        Trip completed = new Trip(
                Instant.parse("2023-01-22T13:00:00Z"),
                Instant.parse("2023-01-22T13:05:00Z"),
                300L,
                "Stop1", "Stop2",
                new BigDecimal("3.25"),
                "Company1", "Bus37", "5500005555555559",
                TripStatus.COMPLETED);

        StringWriter out = new StringWriter();
        writer.write(List.of(completed), out);

        assertThat(out.toString()).isEqualToNormalizingNewlines(
                "Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status\n"
                        + "22-01-2023 13:00:00,22-01-2023 13:05:00,300,Stop1,Stop2,$3.25,Company1,Bus37,5500005555555559,COMPLETED\n");
    }

    @Test
    void incompleteOn_leavesOffSideBlank() throws IOException {
        Trip incomplete = new Trip(
                Instant.parse("2023-01-22T09:20:00Z"),
                null, null,
                "Stop3", null,
                new BigDecimal("7.30"),
                "Company1", "Bus36", "4111111111111111",
                TripStatus.INCOMPLETE);

        StringWriter out = new StringWriter();
        writer.write(List.of(incomplete), out);

        // After the header, the data row should have blanks for Finished/DurationSecs/ToStopId.
        String dataRow = out.toString().split("\\R")[1];
        assertThat(dataRow).isEqualTo(
                "22-01-2023 09:20:00,,,Stop3,,$7.30,Company1,Bus36,4111111111111111,INCOMPLETE");
    }

    @Test
    void orphanOff_leavesOnSideBlank() throws IOException {
        Trip orphan = new Trip(
                null,
                Instant.parse("2023-01-24T16:30:00Z"),
                null,
                null, "Stop2",
                new BigDecimal("5.50"),
                "Company1", "Bus37", "5500005555555559",
                TripStatus.INCOMPLETE);

        StringWriter out = new StringWriter();
        writer.write(List.of(orphan), out);

        String dataRow = out.toString().split("\\R")[1];
        assertThat(dataRow).isEqualTo(
                ",24-01-2023 16:30:00,,,Stop2,$5.50,Company1,Bus37,5500005555555559,INCOMPLETE");
    }

    @Test
    void cancelledTrip_chargeIsZeroDollars() throws IOException {
        Trip cancelled = new Trip(
                Instant.parse("2023-01-23T08:00:00Z"),
                Instant.parse("2023-01-23T08:02:00Z"),
                120L,
                "Stop1", "Stop1",
                new BigDecimal("0.00"),
                "Company1", "Bus37", "4111111111111111",
                TripStatus.CANCELLED);

        StringWriter out = new StringWriter();
        writer.write(List.of(cancelled), out);

        String dataRow = out.toString().split("\\R")[1];
        assertThat(dataRow).contains(",$0.00,").endsWith(",CANCELLED");
    }

    @Test
    void roundsCharge_toTwoDecimals() throws IOException {
        Trip trip = new Trip(
                Instant.parse("2023-01-22T13:00:00Z"),
                Instant.parse("2023-01-22T13:05:00Z"),
                300L,
                "Stop1", "Stop2",
                new BigDecimal("3.255"),  // halves up → $3.26
                "Company1", "Bus37", "5500005555555559",
                TripStatus.COMPLETED);

        StringWriter out = new StringWriter();
        writer.write(List.of(trip), out);

        assertThat(out.toString()).contains(",$3.26,");
    }
}
