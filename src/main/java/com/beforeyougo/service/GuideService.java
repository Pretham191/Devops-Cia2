package com.beforeyougo.service;

import com.beforeyougo.domain.Category;
import com.beforeyougo.domain.Freshness;
import com.beforeyougo.domain.Institution;
import com.beforeyougo.domain.ServiceGuide;
import com.beforeyougo.domain.VerificationStatus;
import com.beforeyougo.repository.InstitutionRepository;
import com.beforeyougo.repository.ServiceGuideRepository;
import com.beforeyougo.web.form.GuideForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class GuideService {

    private final ServiceGuideRepository guides;
    private final InstitutionRepository institutions;
    private final FreshnessCalculator freshness;

    public GuideService(ServiceGuideRepository guides, InstitutionRepository institutions, FreshnessCalculator freshness) {
        this.guides = guides;
        this.institutions = institutions;
        this.freshness = freshness;
    }

    @Transactional(readOnly = true)
    public List<ServiceGuide> search(String query, Category category) {
        return guides.search(query, category);
    }

    /** Loads a guide with all requirement lists initialised, ready for rendering. */
    @Transactional(readOnly = true)
    public ServiceGuide getDetail(Long id) {
        ServiceGuide guide = guides.findById(id).orElseThrow(() -> new GuideNotFoundException(id));
        guide.getDocuments().size();
        guide.getPrerequisites().size();
        guide.getSteps().size();
        guide.getCommonMistakes().size();
        return guide;
    }

    @Transactional(readOnly = true)
    public List<ServiceGuide> findAll() {
        return guides.findAllWithInstitution();
    }

    /** Guides an admin should look at: possibly outdated by date, or flagged by an approved community report. */
    @Transactional(readOnly = true)
    public List<ServiceGuide> needsReview() {
        return guides.findAllWithInstitution().stream()
                .filter(g -> g.isFlaggedOutdated() || freshness.badgeFor(g.getLastVerifiedAt()) == Freshness.POSSIBLY_OUTDATED)
                .toList();
    }

    @Transactional
    public ServiceGuide create(GuideForm form) {
        ServiceGuide guide = new ServiceGuide(findInstitution(form.getInstitutionId()), form.getTitle().trim());
        apply(guide, form);
        return guides.save(guide);
    }

    @Transactional
    public ServiceGuide update(Long id, GuideForm form) {
        ServiceGuide guide = guides.findById(id).orElseThrow(() -> new GuideNotFoundException(id));
        guide.setInstitution(findInstitution(form.getInstitutionId()));
        guide.setTitle(form.getTitle().trim());
        apply(guide, form);
        return guide;
    }

    /** Admin re-verified the guide today: officially verified, dated today, community flag cleared. */
    @Transactional
    public ServiceGuide markVerified(Long id) {
        ServiceGuide guide = guides.findById(id).orElseThrow(() -> new GuideNotFoundException(id));
        guide.setVerificationStatus(VerificationStatus.OFFICIALLY_VERIFIED);
        guide.setLastVerifiedAt(freshness.today());
        guide.setFlaggedOutdated(false);
        guide.setUpdatedAt(Instant.now());
        return guide;
    }

    @Transactional(readOnly = true)
    public GuideForm toForm(Long id) {
        ServiceGuide g = getDetail(id);
        GuideForm form = new GuideForm();
        form.setInstitutionId(g.getInstitution().getId());
        form.setTitle(g.getTitle());
        form.setPurpose(g.getPurpose());
        form.setDocumentsText(String.join("\n", g.getDocuments()));
        form.setPrerequisitesText(String.join("\n", g.getPrerequisites()));
        form.setStepsText(String.join("\n", g.getSteps()));
        form.setCommonMistakesText(String.join("\n", g.getCommonMistakes()));
        form.setFees(g.getFees());
        form.setAppointmentInfo(g.getAppointmentInfo());
        form.setTimings(g.getTimings());
        return form;
    }

    /** Textarea → list: one item per line, trimmed, blank lines dropped. */
    public static List<String> parseLines(String text) {
        if (text == null) {
            return List.of();
        }
        return Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    public static boolean hasLineLongerThan(String text, int max) {
        return parseLines(text).stream().anyMatch(line -> line.length() > max);
    }

    /** Blank optional text becomes null so the page shows "Not confirmed" instead of an empty value. */
    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void apply(ServiceGuide guide, GuideForm form) {
        guide.setPurpose(blankToNull(form.getPurpose()));
        guide.setDocuments(parseLines(form.getDocumentsText()));
        guide.setPrerequisites(parseLines(form.getPrerequisitesText()));
        guide.setSteps(parseLines(form.getStepsText()));
        guide.setCommonMistakes(parseLines(form.getCommonMistakesText()));
        guide.setFees(blankToNull(form.getFees()));
        guide.setAppointmentInfo(blankToNull(form.getAppointmentInfo()));
        guide.setTimings(blankToNull(form.getTimings()));
        guide.setUpdatedAt(Instant.now());
    }

    private Institution findInstitution(Long id) {
        return institutions.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown institution " + id));
    }
}
