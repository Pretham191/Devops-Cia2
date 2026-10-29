package com.beforeyougo.service;

import com.beforeyougo.domain.Category;
import com.beforeyougo.domain.Contribution;
import com.beforeyougo.domain.ContributionStatus;
import com.beforeyougo.domain.ContributionType;
import com.beforeyougo.domain.Institution;
import com.beforeyougo.domain.ServiceGuide;
import com.beforeyougo.repository.ContributionRepository;
import com.beforeyougo.repository.ServiceGuideRepository;
import com.beforeyougo.web.form.ContributionForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock
    private ContributionRepository contributions;

    @Mock
    private ServiceGuideRepository guides;

    private ContributionService service;
    private ServiceGuide guide;

    @BeforeEach
    void setUp() {
        service = new ContributionService(contributions, guides);
        guide = new ServiceGuide(new Institution("Test Office", Category.GOVERNMENT, "Chennai"), "Test guide");
    }

    private Contribution pendingOf(ContributionType type) {
        Contribution c = new Contribution(guide, type, "Something I noticed on my visit", LocalDate.of(2026, 6, 1), null);
        when(contributions.findById(1L)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void approvingDiscrepancyFlagsGuide() {
        Contribution c = pendingOf(ContributionType.DISCREPANCY);

        service.approve(1L);

        assertThat(c.getStatus()).isEqualTo(ContributionStatus.APPROVED);
        assertThat(guide.isFlaggedOutdated()).isTrue();
    }

    @Test
    void approvingOutdatedWarningFlagsGuide() {
        pendingOf(ContributionType.OUTDATED_WARNING);

        service.approve(1L);

        assertThat(guide.isFlaggedOutdated()).isTrue();
    }

    @Test
    void approvingTipDoesNotFlagGuide() {
        Contribution c = pendingOf(ContributionType.TIP);

        service.approve(1L);

        assertThat(c.getStatus()).isEqualTo(ContributionStatus.APPROVED);
        assertThat(guide.isFlaggedOutdated()).isFalse();
    }

    @Test
    void rejectingLeavesGuideUnchanged() {
        Contribution c = pendingOf(ContributionType.DISCREPANCY);

        service.reject(1L);

        assertThat(c.getStatus()).isEqualTo(ContributionStatus.REJECTED);
        assertThat(guide.isFlaggedOutdated()).isFalse();
    }

    @Test
    void alreadyModeratedContributionCannotBeApprovedAgain() {
        Contribution c = pendingOf(ContributionType.TIP);
        c.setStatus(ContributionStatus.REJECTED);

        assertThatThrownBy(() -> service.approve(1L)).isInstanceOf(IllegalStateException.class);
        assertThat(c.getStatus()).isEqualTo(ContributionStatus.REJECTED);
    }

    @Test
    void submitStoresPendingWithTrimmedFieldsAndBlankNameAsNull() {
        when(guides.findById(7L)).thenReturn(Optional.of(guide));
        when(contributions.save(any(Contribution.class))).thenAnswer(inv -> inv.getArgument(0));
        ContributionForm form = new ContributionForm();
        form.setType(ContributionType.TIP);
        form.setText("  Bring a pen, they don't have spare ones.  ");
        form.setVisitDate(LocalDate.of(2026, 6, 10));
        form.setDisplayName("   ");

        service.submit(7L, form);

        ArgumentCaptor<Contribution> saved = ArgumentCaptor.forClass(Contribution.class);
        verify(contributions).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ContributionStatus.PENDING);
        assertThat(saved.getValue().getText()).isEqualTo("Bring a pen, they don't have spare ones.");
        assertThat(saved.getValue().getDisplayName()).isNull();
    }
}
