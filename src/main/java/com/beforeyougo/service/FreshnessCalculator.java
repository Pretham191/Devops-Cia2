package com.beforeyougo.service;

import com.beforeyougo.domain.Freshness;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pure date logic: works out the freshness badge from the last verified date.
 * FRESH up to 90 days, AGEING 91-180 days, POSSIBLY_OUTDATED after 180 days or if never verified.
 */
@Component
public class FreshnessCalculator {

    public static final int FRESH_MAX_DAYS = 90;
    public static final int AGEING_MAX_DAYS = 180;

    private final Clock clock;

    public FreshnessCalculator(Clock clock) {
        this.clock = clock;
    }

    public Freshness badgeFor(LocalDate lastVerifiedAt) {
        if (lastVerifiedAt == null) {
            return Freshness.POSSIBLY_OUTDATED;
        }
        long days = daysSince(lastVerifiedAt);
        if (days <= FRESH_MAX_DAYS) {
            return Freshness.FRESH;
        }
        if (days <= AGEING_MAX_DAYS) {
            return Freshness.AGEING;
        }
        return Freshness.POSSIBLY_OUTDATED;
    }

    /** Days between the given date and today; negative for a future date. */
    public long daysSince(LocalDate date) {
        return ChronoUnit.DAYS.between(date, today());
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }
}
