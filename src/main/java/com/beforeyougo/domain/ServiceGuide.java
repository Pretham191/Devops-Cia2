package com.beforeyougo.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ServiceGuide {

    public static final int MAX_ITEM_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String purpose;

    @ElementCollection
    @CollectionTable(name = "guide_documents", joinColumns = @JoinColumn(name = "guide_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "item", length = MAX_ITEM_LENGTH)
    private List<String> documents = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "guide_prerequisites", joinColumns = @JoinColumn(name = "guide_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "item", length = MAX_ITEM_LENGTH)
    private List<String> prerequisites = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "guide_steps", joinColumns = @JoinColumn(name = "guide_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "item", length = MAX_ITEM_LENGTH)
    private List<String> steps = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "guide_common_mistakes", joinColumns = @JoinColumn(name = "guide_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "item", length = MAX_ITEM_LENGTH)
    private List<String> commonMistakes = new ArrayList<>();

    /** Nullable: null means "not confirmed", never a guessed value. */
    @Column(length = 500)
    private String fees;

    @Column(length = 500)
    private String appointmentInfo;

    @Column(length = 500)
    private String timings;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    private LocalDate lastVerifiedAt;

    @Column(nullable = false)
    private boolean flaggedOutdated;

    private Instant updatedAt;

    protected ServiceGuide() {
    }

    public ServiceGuide(Institution institution, String title) {
        this.institution = institution;
        this.title = title;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public List<String> getDocuments() { return documents; }
    public void setDocuments(List<String> documents) { replace(this.documents, documents); }
    public List<String> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(List<String> prerequisites) { replace(this.prerequisites, prerequisites); }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { replace(this.steps, steps); }
    public List<String> getCommonMistakes() { return commonMistakes; }
    public void setCommonMistakes(List<String> commonMistakes) { replace(this.commonMistakes, commonMistakes); }
    public String getFees() { return fees; }
    public void setFees(String fees) { this.fees = fees; }
    public String getAppointmentInfo() { return appointmentInfo; }
    public void setAppointmentInfo(String appointmentInfo) { this.appointmentInfo = appointmentInfo; }
    public String getTimings() { return timings; }
    public void setTimings(String timings) { this.timings = timings; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public LocalDate getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(LocalDate lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
    public boolean isFlaggedOutdated() { return flaggedOutdated; }
    public void setFlaggedOutdated(boolean flaggedOutdated) { this.flaggedOutdated = flaggedOutdated; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /** Mutate in place so Hibernate keeps tracking the same persistent collection. */
    private static void replace(List<String> target, List<String> source) {
        target.clear();
        if (source != null) {
            target.addAll(source);
        }
    }
}
