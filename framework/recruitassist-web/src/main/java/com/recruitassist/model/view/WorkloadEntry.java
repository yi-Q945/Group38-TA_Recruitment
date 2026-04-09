package com.recruitassist.model.view;

import com.recruitassist.model.UserProfile;

public class WorkloadEntry {
    private final UserProfile user;
    private final int acceptedHours;
    private final long activeApplications;
    private final int threshold;

    public WorkloadEntry(UserProfile user, int acceptedHours, long activeApplications, int threshold) {
        this.user = user;
        this.acceptedHours = acceptedHours;
        this.activeApplications = activeApplications;
        this.threshold = threshold;
    }

    public UserProfile getUser() {
        return user;
    }

    public int getAcceptedHours() {
        return acceptedHours;
    }

    public long getActiveApplications() {
        return activeApplications;
    }

    public int getThreshold() {
        return threshold;
    }

    public boolean isOverloaded() {
        return acceptedHours > threshold;
    }
}
