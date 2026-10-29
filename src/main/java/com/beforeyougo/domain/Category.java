package com.beforeyougo.domain;

public enum Category {
    GOVERNMENT("Government"),
    EDUCATION("Education"),
    HEALTHCARE("Healthcare"),
    BANKING("Banking"),
    OTHER("Other");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
