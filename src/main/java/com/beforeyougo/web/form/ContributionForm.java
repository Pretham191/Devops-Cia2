package com.beforeyougo.web.form;

import com.beforeyougo.domain.ContributionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Visitor contribution. "Visit date not in the future" is checked in the controller against the app Clock. */
public class ContributionForm {

    @NotNull(message = "Choose what kind of contribution this is")
    private ContributionType type;

    @NotBlank(message = "Please describe your experience")
    @Size(min = 10, max = 1000, message = "Please write between 10 and 1000 characters")
    private String text;

    @NotNull(message = "Enter the date of your visit")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate visitDate;

    @Size(max = 60, message = "Display name can be at most 60 characters")
    private String displayName;

    public ContributionType getType() { return type; }
    public void setType(ContributionType type) { this.type = type; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
