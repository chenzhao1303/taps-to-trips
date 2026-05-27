package tapstotrips.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import tapstotrips.model.Trip;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes trips to a CSV file. Columns match the spec's example:
 * {@code Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status}.
 * Money is formatted {@code $X.XX}; nullable fields (e.g., the OFF side of an INCOMPLETE trip)
 * are emitted as blank.
 */
public final class TripCsvWriter {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private static final String[] HEADERS = {
            "Started", "Finished", "DurationSecs",
            "FromStopId", "ToStopId", "ChargeAmount",
            "CompanyId", "BusID", "PAN", "Status"
    };

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(HEADERS)
            .setRecordSeparator('\n')
            .build();

    public void write(List<Trip> trips, Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            write(trips, writer);
        }
    }

    public void write(List<Trip> trips, Writer writer) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(writer, FORMAT)) {
            for (Trip trip : trips) {
                // Pass null (not "") for blank fields so Commons CSV emits a true empty
                // cell rather than the quoted empty string `""`.
                printer.printRecord(
                        formatDateTime(trip.started()),
                        formatDateTime(trip.finished()),
                        trip.durationSecs(),
                        trip.fromStopId(),
                        trip.toStopId(),
                        formatMoney(trip.chargeAmount()),
                        trip.companyId(),
                        trip.busId(),
                        trip.pan(),
                        trip.status().name());
            }
        }
    }

    private static String formatDateTime(LocalDateTime dt) {
        return dt == null ? null : DATE_TIME_FORMAT.format(dt);
    }

    private static String formatMoney(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
