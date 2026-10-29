package com.beforeyougo.service;

import com.beforeyougo.domain.Category;
import com.beforeyougo.domain.Institution;
import com.beforeyougo.domain.ServiceGuide;
import com.beforeyougo.domain.VerificationStatus;
import com.beforeyougo.repository.InstitutionRepository;
import com.beforeyougo.repository.ServiceGuideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 30);

    @Mock
    private ServiceGuideRepository guides;

    @Mock
    private InstitutionRepository institutions;

    private GuideService service;
    private final Institution office = new Institution("Test Office", Category.GOVERNMENT, "Chennai");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new GuideService(guides, institutions, new FreshnessCalculator(clock));
    }

    private ServiceGuide guide(String title, LocalDate lastVerified, boolean flagged) {
        ServiceGuide g = new ServiceGuide(office, title);
        g.setLastVerifiedAt(lastVerified);
        g.setFlaggedOutdated(flagged);
        return g;
    }

    @Test
    void markVerifiedSetsStatusDateAndClearsFlag() {
        ServiceGuide g = guide("Old guide", TODAY.minusDays(400), true);
        when(guides.findById(5L)).thenReturn(Optional.of(g));

        service.markVerified(5L);

        assertThat(g.getVerificationStatus()).isEqualTo(VerificationStatus.OFFICIALLY_VERIFIED);
        assertThat(g.getLastVerifiedAt()).isEqualTo(TODAY);
        assertThat(g.isFlaggedOutdated()).isFalse();
    }

    @Test
    void markVerifiedUnknownGuideThrowsNotFound() {
        when(guides.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markVerified(99L)).isInstanceOf(GuideNotFoundException.class);
    }

    @Test
    void needsReviewIncludesOutdatedAndFlaggedOnly() {
        ServiceGuide fresh = guide("Fresh", TODAY.minusDays(10), false);
        ServiceGuide ageing = guide("Ageing", TODAY.minusDays(120), false);
        ServiceGuide outdated = guide("Outdated", TODAY.minusDays(200), false);
        ServiceGuide never = guide("Never verified", null, false);
        ServiceGuide flagged = guide("Flagged but fresh", TODAY.minusDays(5), true);
        when(guides.findAllWithInstitution()).thenReturn(List.of(fresh, ageing, outdated, never, flagged));

        assertThat(service.needsReview()).containsExactly(outdated, never, flagged);
    }

    @Test
    void parseLinesTrimsAndDropsBlankLines() {
        assertThat(GuideService.parseLines("  Photo ID \r\n\n Address proof\n   \n")).containsExactly("Photo ID", "Address proof");
        assertThat(GuideService.parseLines(null)).isEmpty();
    }
}
