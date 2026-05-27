package tapstotrips.model;

import java.time.Instant;

public record Tap(
        long id,
        Instant dateTime,
        TapType type,
        String stopId,
        String companyId,
        String busId,
        String pan
) {
}
