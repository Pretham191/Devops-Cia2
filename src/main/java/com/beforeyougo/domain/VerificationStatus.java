package com.beforeyougo.domain;

public enum VerificationStatus {
    OFFICIALLY_VERIFIED("Officially verified"),
    UNVERIFIED("Unverified");

    private final String label;

    VerificationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
