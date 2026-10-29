package com.beforeyougo.domain;

public enum ContributionType {
    TIP("Tip", false),
    DISCREPANCY("Discrepancy", true),
    OUTDATED_WARNING("Outdated warning", true);

    private final String label;
    private final boolean flagsGuide;

    ContributionType(String label, boolean flagsGuide) {
        this.label = label;
        this.flagsGuide = flagsGuide;
    }

    public String getLabel() {
        return label;
    }

    /** Whether approving a contribution of this type marks the guide "Reported as possibly changed". */
    public boolean flagsGuide() {
        return flagsGuide;
    }
}
