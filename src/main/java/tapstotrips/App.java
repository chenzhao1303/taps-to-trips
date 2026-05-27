package tapstotrips;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tapstotrips.csv.TapCsvReader;
import tapstotrips.csv.TripCsvWriter;
import tapstotrips.matching.TripMatcher;
import tapstotrips.model.Tap;
import tapstotrips.model.Trip;
import tapstotrips.model.TripStatus;
import tapstotrips.pricing.FareTable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static final String DEFAULT_INPUT = "taps.csv";
    private static final String DEFAULT_OUTPUT = "trips.csv";

    public static void main(String[] args) throws IOException {
        Path input = Path.of(args.length >= 1 ? args[0] : DEFAULT_INPUT);
        Path output = Path.of(args.length >= 2 ? args[1] : DEFAULT_OUTPUT);
        run(input, output);
    }

    static void run(Path input, Path output) throws IOException {
        log.info("Reading taps from {}", input);
        List<Tap> taps = new TapCsvReader().read(input);
        log.info("Read {} taps", taps.size());

        List<Trip> trips = new TripMatcher(FareTable.standard()).match(taps);
        Map<TripStatus, Long> counts = trips.stream()
                .collect(Collectors.groupingBy(Trip::status, Collectors.counting()));
        log.info("Matched {} trips: {}", trips.size(), counts);

        new TripCsvWriter().write(trips, output);
        log.info("Wrote trips to {}", output);
    }
}
