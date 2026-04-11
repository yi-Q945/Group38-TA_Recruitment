package com.recruitassist.model;

public enum UserRole {
    TA,
    MO,
    ADMIN;

    public String getLabel() {
        return switch (this) {
            case TA -> "Teaching Assistant";
            case MO -> "Module Organiser";
            case ADMIN -> "Admin";
        };
    }

    public String getCssClass() {
        return name().toLowerCase();
    }
}
