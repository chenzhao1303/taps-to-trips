package tapstotrips.matching;

import org.junit.jupiter.api.Test;
import tapstotrips.model.Tap;
import tapstotrips.model.TapType;
import tapstotrips.model.Trip;
import tapstotrips.model.TripStatus;
import tapstotrips.pricing.FareTable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripMatcherTest {

    private static final String PAN_A = "5500005555555559";
    private static final String PAN_B = "4111111111111111";
    private static final String CO = "Company1";
    private static final String BUS = "Bus37";

    private final TripMatcher matcher = new TripMatcher(FareTable.standard());

    @Test
    void completedTrip_isPricedByFareTable() {
        Tap on = tap(1, "2023-01-22T13:00:00", TapType.ON, "Stop1", PAN_A);
        Tap off = tap(2, "2023-01-22T13:05:00", TapType.OFF, "Stop2", PAN_A);

        List<Trip> trips = matcher.match(List.of(on, off));

        assertThat(trips).hasSize(1);
        Trip trip = trips.getFirst();
        assertThat(trip.status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trip.fromStopId()).isEqualTo("Stop1");
        assertThat(trip.toStopId()).isEqualTo("Stop2");
        assertThat(trip.durationSecs()).isEqualTo(300L);
        assertThat(trip.chargeAmount()).isEqualByComparingTo("3.25");
    }

    @Test
    void cancelledTrip_sameStop_chargesZero() {
        Tap on = tap(1, "2023-01-23T08:00:00", TapType.ON, "Stop1", PAN_B);
        Tap off = tap(2, "2023-01-23T08:02:00", TapType.OFF, "Stop1", PAN_B);

        List<Trip> trips = matcher.match(List.of(on, off));

        assertThat(trips).hasSize(1);
        Trip trip = trips.getFirst();
        assertThat(trip.status()).isEqualTo(TripStatus.CANCELLED);
        assertThat(trip.fromStopId()).isEqualTo("Stop1");
        assertThat(trip.toStopId()).isEqualTo("Stop1");
        assertThat(trip.chargeAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void incompleteOn_chargesMaxFareFromBoardingStop() {
        Tap on = tap(1, "2023-01-22T09:20:00", TapType.ON, "Stop3", PAN_B);

        List<Trip> trips = matcher.match(List.of(on));

        assertThat(trips).hasSize(1);
        Trip trip = trips.getFirst();
        assertThat(trip.status()).isEqualTo(TripStatus.INCOMPLETE);
        assertThat(trip.fromStopId()).isEqualTo("Stop3");
        assertThat(trip.toStopId()).isNull();
        assertThat(trip.finished()).isNull();
        assertThat(trip.durationSecs()).isNull();
        assertThat(trip.chargeAmount()).isEqualByComparingTo("7.30");
    }

    @Test
    void orphanOff_chargesMaxFareFromOffStop_andHasBlankOnSide() {
        Tap off = tap(6, "2023-01-24T16:30:00", TapType.OFF, "Stop2", PAN_A);

        List<Trip> trips = matcher.match(List.of(off));

        assertThat(trips).hasSize(1);
        Trip trip = trips.getFirst();
        assertThat(trip.status()).isEqualTo(TripStatus.INCOMPLETE);
        assertThat(trip.started()).isNull();
        assertThat(trip.fromStopId()).isNull();
        assertThat(trip.toStopId()).isEqualTo("Stop2");
        assertThat(trip.chargeAmount()).isEqualByComparingTo("5.50");
    }

    @Test
    void twoConsecutiveOns_closeFirstAsIncomplete() {
        Tap on1 = tap(1, "2023-01-22T10:00:00", TapType.ON, "Stop1", PAN_A);
        Tap on2 = tap(2, "2023-01-22T10:30:00", TapType.ON, "Stop2", PAN_A);
        Tap off = tap(3, "2023-01-22T10:35:00", TapType.OFF, "Stop3", PAN_A);

        List<Trip> trips = matcher.match(List.of(on1, on2, off));

        assertThat(trips).hasSize(2);
        assertThat(trips.get(0).status()).isEqualTo(TripStatus.INCOMPLETE);
        assertThat(trips.get(0).fromStopId()).isEqualTo("Stop1");
        assertThat(trips.get(1).status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trips.get(1).fromStopId()).isEqualTo("Stop2");
        assertThat(trips.get(1).toStopId()).isEqualTo("Stop3");
    }

    @Test
    void differentBuses_areMatchedIndependently() {
        Tap on1 = tap(1, "2023-01-22T08:00:00", TapType.ON, "Stop1", PAN_A, CO, "Bus10");
        Tap off1 = tap(2, "2023-01-22T08:05:00", TapType.OFF, "Stop2", PAN_A, CO, "Bus10");
        // Same PAN, same company, different bus — must not pair across buses.
        Tap on2 = tap(3, "2023-01-22T09:00:00", TapType.ON, "Stop2", PAN_A, CO, "Bus20");
        Tap off2 = tap(4, "2023-01-22T09:10:00", TapType.OFF, "Stop3", PAN_A, CO, "Bus20");

        List<Trip> trips = matcher.match(List.of(on1, off1, on2, off2));

        assertThat(trips).hasSize(2);
        assertThat(trips).allMatch(t -> t.status() == TripStatus.COMPLETED);
        assertThat(trips.get(0).busId()).isEqualTo("Bus10");
        assertThat(trips.get(1).busId()).isEqualTo("Bus20");
    }

    @Test
    void outOfOrderInput_isSortedByTimestampWithinGroup() {
        Tap off = tap(2, "2023-01-22T13:05:00", TapType.OFF, "Stop2", PAN_A);
        Tap on = tap(1, "2023-01-22T13:00:00", TapType.ON, "Stop1", PAN_A);

        // Note: OFF appears before ON in the input.
        List<Trip> trips = matcher.match(List.of(off, on));

        assertThat(trips).hasSize(1);
        assertThat(trips.getFirst().status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trips.getFirst().fromStopId()).isEqualTo("Stop1");
        assertThat(trips.getFirst().toStopId()).isEqualTo("Stop2");
    }

    @Test
    void outputIsSortedByStartedAscending_orphansByFinished() {
        // Build trips spanning several days/PANs to exercise the final sort.
        Tap on1 = tap(1, "2023-01-22T13:00:00", TapType.ON, "Stop1", PAN_A);
        Tap off1 = tap(2, "2023-01-22T13:05:00", TapType.OFF, "Stop2", PAN_A);
        Tap onlyOff = tap(3, "2023-01-22T09:00:00", TapType.OFF, "Stop2", PAN_B);
        Tap on2 = tap(4, "2023-01-23T08:00:00", TapType.ON, "Stop1", PAN_B, CO, "Bus42");

        List<Trip> trips = matcher.match(List.of(on1, off1, onlyOff, on2));

        assertThat(trips).hasSize(3);
        // Expected order by sortKey(started ?? finished): 09:00 (orphan), 13:00, 08:00 next day.
        assertThat(trips.get(0).status()).isEqualTo(TripStatus.INCOMPLETE);
        assertThat(trips.get(0).finished()).isEqualTo(Instant.parse("2023-01-22T09:00:00Z"));
        assertThat(trips.get(1).started()).isEqualTo(Instant.parse("2023-01-22T13:00:00Z"));
        assertThat(trips.get(2).started()).isEqualTo(Instant.parse("2023-01-23T08:00:00Z"));
    }

    @Test
    void maskPan_showsOnlyLastFourDigits() {
        assertThat(TripMatcher.maskPan("5500005555555559")).isEqualTo("****5559");
        assertThat(TripMatcher.maskPan("1234")).isEqualTo("****1234");
        assertThat(TripMatcher.maskPan("12")).isEqualTo("****");
        assertThat(TripMatcher.maskPan(null)).isEqualTo("****");
    }

    private static Tap tap(long id, String iso, TapType type, String stopId, String pan) {
        return tap(id, iso, type, stopId, pan, CO, BUS);
    }

    private static Tap tap(long id, String iso, TapType type, String stopId,
                           String pan, String companyId, String busId) {
        return new Tap(id, Instant.parse(iso + "Z"), type, stopId, companyId, busId, pan);
    }
}
