package com.beforeyougo.domain;

/** Computed freshness badge for a guide, derived from its last verified date. */
public enum Freshness {
    FRESH("Fresh", "fresh"),
    AGEING("Ageing", "ageing"),
    POSSIBLY_OUTDATED("Possibly outdated", "outdated");

    private final String label;
    private final String cssClass;

    Freshness(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }
}
