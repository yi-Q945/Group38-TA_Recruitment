package com.recruitassist.model;

import java.util.Arrays;
import java.util.Optional;

public enum JobStatus {
    OPEN,
    CLOSED;

    public static Optional<JobStatus> from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(rawValue.trim()))
                .findFirst();
    }

    public String getLabel() {
        return switch (this) {
            case OPEN -> "Open";
            case CLOSED -> "Closed";
        };
    }

    public String getCssClass() {
        return name().toLowerCase();
    }

    public String getCode() {
        return name();
    }
}
