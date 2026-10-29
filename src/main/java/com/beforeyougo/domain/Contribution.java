package com.beforeyougo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;

import java.time.Instant;
import java.time.LocalDate;

/** A community-reported visitor experience. Never treated as an official requirement. */
@Entity
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private ServiceGuide guide;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_type", nullable = false, length = 30)
    private ContributionType type;

    @Column(name = "body", nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Column(length = 60)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContributionStatus status = ContributionStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt;

    protected Contribution() {
    }

    public Contribution(ServiceGuide guide, ContributionType type, String text, LocalDate visitDate, String displayName) {
        this.guide = guide;
        this.type = type;
        this.text = text;
        this.visitDate = visitDate;
        this.displayName = displayName;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public ServiceGuide getGuide() { return guide; }
    public ContributionType getType() { return type; }
    public String getText() { return text; }
    public LocalDate getVisitDate() { return visitDate; }
    public String getDisplayName() { return displayName; }
    public ContributionStatus getStatus() { return status; }
    public void setStatus(ContributionStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
