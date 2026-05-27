package tapstotrips.pricing;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Bidirectional fare table. Fares are looked up by the unordered pair of stop ids, so
 * {Stop1, Stop2} and {Stop2, Stop1} resolve to the same fare. For an INCOMPLETE trip we
 * also need the maximum fare reachable from a given stop, which {@link #maxFareFrom} returns.
 */
public final class FareTable {

    private final Map<Set<String>, BigDecimal> fares;

    public FareTable(Map<Set<String>, BigDecimal> fares) {
        this.fares = Map.copyOf(fares);
    }

    public static FareTable standard() {
        return new FareTable(Map.of(
                Set.of("Stop1", "Stop2"), new BigDecimal("3.25"),
                Set.of("Stop2", "Stop3"), new BigDecimal("5.50"),
                Set.of("Stop1", "Stop3"), new BigDecimal("7.30")
        ));
    }

    public BigDecimal fareBetween(String stopA, String stopB) {
        if (stopA.equals(stopB)) {
            throw new IllegalArgumentException(
                    "fareBetween requires two distinct stops, got " + stopA + " twice");
        }
        BigDecimal fare = fares.get(Set.of(stopA, stopB));
        if (fare == null) {
            throw new IllegalArgumentException(
                    "No fare configured between " + stopA + " and " + stopB);
        }
        return fare;
    }

    public BigDecimal maxFareFrom(String stop) {
        return fares.entrySet().stream()
                .filter(e -> e.getKey().contains(stop))
                .map(Map.Entry::getValue)
                .max(BigDecimal::compareTo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stop " + stop + " is not present in any fare entry"));
    }
}
