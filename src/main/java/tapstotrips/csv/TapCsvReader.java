package tapstotrips.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import tapstotrips.model.Tap;
import tapstotrips.model.TapType;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads taps from a CSV file. The input format is the spec's header-bearing file with the
 * columns: {@code ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN}. Header and field
 * whitespace is trimmed since the spec's example has spaces after commas.
 */
public final class TapCsvReader {

    static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .build();

    public List<Tap> read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return read(reader);
        }
    }

    public List<Tap> read(Reader reader) throws IOException {
        List<Tap> taps = new ArrayList<>();
        for (CSVRecord record : FORMAT.parse(reader)) {
            taps.add(toTap(record));
        }
        return taps;
    }

    private static Tap toTap(CSVRecord record) {
        return new Tap(
                Long.parseLong(record.get("ID")),
                DATE_TIME_FORMAT.parse(record.get("DateTimeUTC"), Instant::from),
                TapType.valueOf(record.get("TapType")),
                record.get("StopId"),
                record.get("CompanyId"),
                record.get("BusID"),
                record.get("PAN"));
    }
}
