package tapstotrips.matching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tapstotrips.model.Tap;
import tapstotrips.model.TapType;
import tapstotrips.model.Trip;
import tapstotrips.model.TripStatus;
import tapstotrips.pricing.FareTable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pairs ON/OFF taps into trips. Taps are grouped by (pan, companyId, busId) — a passenger's
 * tap sequence on one bus, for one operator — then walked in chronological order:
 *
 * <ul>
 *   <li>ON followed by OFF at a different stop → COMPLETED, priced by the fare table.</li>
 *   <li>ON followed by OFF at the same stop → CANCELLED, $0.00.</li>
 *   <li>ON with no matching OFF → INCOMPLETE, priced as the max fare reachable from the ON stop.</li>
 *   <li>OFF with no preceding ON → INCOMPLETE (orphan), priced as the max fare from the OFF stop;
 *       a warning is logged.</li>
 *   <li>Two consecutive ONs → the first ON is closed as INCOMPLETE.</li>
 * </ul>
 *
 * Output is sorted by {@code started} ascending; orphan-OFF rows (no {@code started}) fall back
 * to {@code finished}.
 */
public final class TripMatcher {

    private static final Logger log = LoggerFactory.getLogger(TripMatcher.class);
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final FareTable fares;

    public TripMatcher(FareTable fares) {
        this.fares = fares;
    }

    public List<Trip> match(Collection<Tap> taps) {
        Map<GroupKey, List<Tap>> groups = taps.stream()
                .collect(Collectors.groupingBy(GroupKey::of));

        List<Trip> trips = new ArrayList<>();
        for (List<Tap> group : groups.values()) {
            group.sort(Comparator.comparing(Tap::dateTime));
            trips.addAll(matchGroup(group));
        }

        trips.sort(Comparator.comparing(this::sortKey));
        return trips;
    }

    private List<Trip> matchGroup(List<Tap> sorted) {
        List<Trip> out = new ArrayList<>();
        Tap pendingOn = null;

        for (Tap tap : sorted) {
            switch (tap.type()) {
                case ON -> {
                    if (pendingOn != null) {
                        out.add(incompleteFromOn(pendingOn));
                    }
                    pendingOn = tap;
                }
                case OFF -> {
                    if (pendingOn != null) {
                        out.add(completedOrCancelled(pendingOn, tap));
                        pendingOn = null;
                    } else {
                        log.warn("Orphan OFF tap (no preceding ON): id={}, pan={}, busId={}, stop={}, at={}",
                                tap.id(), maskPan(tap.pan()), tap.busId(), tap.stopId(), tap.dateTime());
                        out.add(orphanOff(tap));
                    }
                }
            }
        }
        if (pendingOn != null) {
            out.add(incompleteFromOn(pendingOn));
        }
        return out;
    }

    private Trip completedOrCancelled(Tap on, Tap off) {
        if (on.stopId().equals(off.stopId())) {
            return new Trip(
                    on.dateTime(), off.dateTime(),
                    Duration.between(on.dateTime(), off.dateTime()).toSeconds(),
                    on.stopId(), off.stopId(),
                    ZERO,
                    on.companyId(), on.busId(), on.pan(),
                    TripStatus.CANCELLED);
        }
        return new Trip(
                on.dateTime(), off.dateTime(),
                Duration.between(on.dateTime(), off.dateTime()).toSeconds(),
                on.stopId(), off.stopId(),
                fares.fareBetween(on.stopId(), off.stopId()),
                on.companyId(), on.busId(), on.pan(),
                TripStatus.COMPLETED);
    }

    private Trip incompleteFromOn(Tap on) {
        return new Trip(
                on.dateTime(), null, null,
                on.stopId(), null,
                fares.maxFareFrom(on.stopId()),
                on.companyId(), on.busId(), on.pan(),
                TripStatus.INCOMPLETE);
    }

    private Trip orphanOff(Tap off) {
        return new Trip(
                null, off.dateTime(), null,
                null, off.stopId(),
                fares.maxFareFrom(off.stopId()),
                off.companyId(), off.busId(), off.pan(),
                TripStatus.INCOMPLETE);
    }

    static String maskPan(String pan) {
        return pan == null || pan.length() < 4
                ? "****"
                : "****" + pan.substring(pan.length() - 4);
    }

    private Instant sortKey(Trip trip) {
        return trip.started() != null ? trip.started() : trip.finished();
    }

    private record GroupKey(String pan, String companyId, String busId) {
        static GroupKey of(Tap tap) {
            return new GroupKey(tap.pan(), tap.companyId(), tap.busId());
        }
    }
}
