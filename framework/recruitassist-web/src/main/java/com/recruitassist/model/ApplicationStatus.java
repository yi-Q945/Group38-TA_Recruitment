package com.recruitassist.model;

import java.util.Arrays;
import java.util.Optional;

public enum ApplicationStatus {
    SUBMITTED,
    SHORTLISTED,
    ACCEPTED,
    REJECTED,
    WITHDRAWN;

    public static Optional<ApplicationStatus> from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(rawValue.trim()))
                .findFirst();
    }

    public String getLabel() {
        return switch (this) {
            case SUBMITTED -> "Submitted";
            case SHORTLISTED -> "Shortlisted";
            case ACCEPTED -> "Accepted";
            case REJECTED -> "Rejected";
            case WITHDRAWN -> "Withdrawn";
        };
    }

    public String getCssClass() {
        return name().toLowerCase();
    }

    public String getCode() {
        return name();
    }
}
