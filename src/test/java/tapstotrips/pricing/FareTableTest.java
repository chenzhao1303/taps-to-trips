package tapstotrips.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FareTableTest {

    private final FareTable fares = FareTable.standard();

    @Test
    void fareBetween_isBidirectional() {
        assertThat(fares.fareBetween("Stop1", "Stop2")).isEqualByComparingTo("3.25");
        assertThat(fares.fareBetween("Stop2", "Stop1")).isEqualByComparingTo("3.25");
        assertThat(fares.fareBetween("Stop2", "Stop3")).isEqualByComparingTo("5.50");
        assertThat(fares.fareBetween("Stop3", "Stop2")).isEqualByComparingTo("5.50");
        assertThat(fares.fareBetween("Stop1", "Stop3")).isEqualByComparingTo("7.30");
        assertThat(fares.fareBetween("Stop3", "Stop1")).isEqualByComparingTo("7.30");
    }

    @Test
    void fareBetween_unknownPair_throws() {
        assertThatThrownBy(() -> fares.fareBetween("Stop1", "Stop99"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fareBetween_sameStop_throws() {
        assertThatThrownBy(() -> fares.fareBetween("Stop1", "Stop1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distinct");
    }

    @Test
    void maxFareFrom_picksLargestReachable() {
        assertThat(fares.maxFareFrom("Stop1")).isEqualByComparingTo("7.30");
        assertThat(fares.maxFareFrom("Stop2")).isEqualByComparingTo("5.50");
        assertThat(fares.maxFareFrom("Stop3")).isEqualByComparingTo("7.30");
    }

    @Test
    void maxFareFrom_unknownStop_throws() {
        assertThatThrownBy(() -> fares.maxFareFrom("Stop99"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canConstructFromCustomMap() {
        FareTable custom = new FareTable(java.util.Map.of(
                java.util.Set.of("A", "B"), new BigDecimal("1.00")));
        assertThat(custom.fareBetween("A", "B")).isEqualByComparingTo("1.00");
    }
}
