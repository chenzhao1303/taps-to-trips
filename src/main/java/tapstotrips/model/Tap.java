package tapstotrips.model;

import java.time.LocalDateTime;

public record Tap(
        long id,
        LocalDateTime dateTime,
        TapType type,
        String stopId,
        String companyId,
        String busId,
        String pan
) {
}
