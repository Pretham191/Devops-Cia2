package com.beforeyougo.service;

import com.beforeyougo.domain.Freshness;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class FreshnessCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 30);

    private final FreshnessCalculator calculator = new FreshnessCalculator(
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    @ParameterizedTest(name = "{0} days ago -> {1}")
    @CsvSource({
            "0, FRESH",
            "90, FRESH",
            "91, AGEING",
            "180, AGEING",
            "181, POSSIBLY_OUTDATED",
            "1000, POSSIBLY_OUTDATED"
    })
    @DisplayName("badge follows the 90 / 180 day boundaries")
    void badgeBoundaries(long daysAgo, Freshness expected) {
        assertThat(calculator.badgeFor(TODAY.minusDays(daysAgo))).isEqualTo(expected);
    }

    @Test
    void neverVerifiedIsPossiblyOutdated() {
        assertThat(calculator.badgeFor(null)).isEqualTo(Freshness.POSSIBLY_OUTDATED);
    }

    @Test
    void futureDateCountsAsFresh() {
        assertThat(calculator.badgeFor(TODAY.plusDays(3))).isEqualTo(Freshness.FRESH);
    }

    @Test
    void todayComesFromInjectedClock() {
        assertThat(calculator.today()).isEqualTo(TODAY);
        assertThat(calculator.daysSince(TODAY.minusDays(42))).isEqualTo(42);
    }
}
