package com.beforeyougo.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin create/edit form. Requirement lists are textareas with one item per line. */
public class GuideForm {

    @NotNull(message = "Choose an institution")
    private Long institutionId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title can be at most 200 characters")
    private String title;

    @Size(max = 2000, message = "Purpose can be at most 2000 characters")
    private String purpose;

    @Size(max = 10000)
    private String documentsText;

    @Size(max = 10000)
    private String prerequisitesText;

    @Size(max = 10000)
    private String stepsText;

    @Size(max = 10000)
    private String commonMistakesText;

    @Size(max = 500, message = "At most 500 characters")
    private String fees;

    @Size(max = 500, message = "At most 500 characters")
    private String appointmentInfo;

    @Size(max = 500, message = "At most 500 characters")
    private String timings;

    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getDocumentsText() { return documentsText; }
    public void setDocumentsText(String documentsText) { this.documentsText = documentsText; }
    public String getPrerequisitesText() { return prerequisitesText; }
    public void setPrerequisitesText(String prerequisitesText) { this.prerequisitesText = prerequisitesText; }
    public String getStepsText() { return stepsText; }
    public void setStepsText(String stepsText) { this.stepsText = stepsText; }
    public String getCommonMistakesText() { return commonMistakesText; }
    public void setCommonMistakesText(String commonMistakesText) { this.commonMistakesText = commonMistakesText; }
    public String getFees() { return fees; }
    public void setFees(String fees) { this.fees = fees; }
    public String getAppointmentInfo() { return appointmentInfo; }
    public void setAppointmentInfo(String appointmentInfo) { this.appointmentInfo = appointmentInfo; }
    public String getTimings() { return timings; }
    public void setTimings(String timings) { this.timings = timings; }
}
