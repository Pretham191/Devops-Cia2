package com.beforeyougo.service;

import com.beforeyougo.domain.Contribution;
import com.beforeyougo.domain.ContributionStatus;
import com.beforeyougo.domain.ServiceGuide;
import com.beforeyougo.repository.ContributionRepository;
import com.beforeyougo.repository.ServiceGuideRepository;
import com.beforeyougo.web.form.ContributionForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ContributionService {

    private final ContributionRepository contributions;
    private final ServiceGuideRepository guides;

    public ContributionService(ContributionRepository contributions, ServiceGuideRepository guides) {
        this.contributions = contributions;
        this.guides = guides;
    }

    /** Stores a visitor submission as PENDING; it is only shown after an admin approves it. */
    @Transactional
    public Contribution submit(Long guideId, ContributionForm form) {
        ServiceGuide guide = guides.findById(guideId).orElseThrow(() -> new GuideNotFoundException(guideId));
        String name = form.getDisplayName() == null || form.getDisplayName().isBlank() ? null : form.getDisplayName().trim();
        return contributions.save(new Contribution(guide, form.getType(), form.getText().trim(), form.getVisitDate(), name));
    }

    @Transactional(readOnly = true)
    public List<Contribution> approvedFor(Long guideId) {
        return contributions.findByGuide_IdAndStatusOrderByVisitDateDescCreatedAtDesc(guideId, ContributionStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public List<Contribution> pending() {
        return contributions.findByStatusOrderByCreatedAtAsc(ContributionStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return contributions.countByStatus(ContributionStatus.PENDING);
    }

    /**
     * Approves a pending contribution. An approved DISCREPANCY or OUTDATED_WARNING marks the guide
     * "Reported as possibly changed" — the community report never edits the requirements themselves.
     */
    @Transactional
    public Contribution approve(Long id) {
        Contribution contribution = findPending(id);
        contribution.setStatus(ContributionStatus.APPROVED);
        if (contribution.getType().flagsGuide()) {
            contribution.getGuide().setFlaggedOutdated(true);
        }
        return contribution;
    }

    @Transactional
    public Contribution reject(Long id) {
        Contribution contribution = findPending(id);
        contribution.setStatus(ContributionStatus.REJECTED);
        return contribution;
    }

    private Contribution findPending(Long id) {
        Contribution contribution = contributions.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No contribution with id " + id));
        if (contribution.getStatus() != ContributionStatus.PENDING) {
            throw new IllegalStateException("Contribution " + id + " has already been moderated");
        }
        return contribution;
    }
}
