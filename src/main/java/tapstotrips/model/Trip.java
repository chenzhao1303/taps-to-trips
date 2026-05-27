package tapstotrips.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A matched trip. For INCOMPLETE trips caused by an ON without an OFF, the OFF-side fields
 * (finished, durationSecs, toStopId) are null. For orphan OFFs (no preceding ON), the ON-side
 * fields (started, fromStopId) are null instead.
 */
public record Trip(
        LocalDateTime started,
        LocalDateTime finished,
        Long durationSecs,
        String fromStopId,
        String toStopId,
        BigDecimal chargeAmount,
        String companyId,
        String busId,
        String pan,
        TripStatus status
) {
}
