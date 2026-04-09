package com.recruitassist.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class JobPosting {
    private String jobId;
    private String ownerId;
    private String title;
    private String moduleCode;
    private String description;
    private List<String> requiredSkills = new ArrayList<>();
    private List<String> preferredSkills = new ArrayList<>();
    private String deadline;
    private int quota;
    private int workloadHours;
    private JobStatus status;

    public String getJobId() {
        return jobId == null ? "" : jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getOwnerId() {
        return ownerId == null ? "" : ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModuleCode() {
        return moduleCode == null ? "" : moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills == null ? List.of() : requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public List<String> getPreferredSkills() {
        return preferredSkills == null ? List.of() : preferredSkills;
    }

    public void setPreferredSkills(List<String> preferredSkills) {
        this.preferredSkills = preferredSkills;
    }

    public String getDeadline() {
        return deadline == null ? "" : deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public int getQuota() {
        return quota <= 0 ? 1 : quota;
    }

    public void setQuota(int quota) {
        this.quota = quota;
    }

    public int getWorkloadHours() {
        return Math.max(workloadHours, 0);
    }

    public void setWorkloadHours(int workloadHours) {
        this.workloadHours = workloadHours;
    }

    public JobStatus getStatus() {
        return status == null ? JobStatus.OPEN : status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public boolean isOpen() {
        return getStatus() == JobStatus.OPEN;
    }

    public boolean isExpired() {
        LocalDate deadlineDate = getDeadlineDate();
        return deadlineDate != null && deadlineDate.isBefore(LocalDate.now());
    }

    public boolean isAcceptingApplications(int acceptedCount) {
        return isOpen() && !isExpired() && acceptedCount < getQuota();
    }

    public LocalDate getDeadlineDate() {
        if (getDeadline().isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(getDeadline());
        } catch (Exception ignored) {
            return null;
        }
    }

    public String getDeadlineLabel() {
        return getDeadline().isBlank() ? "TBD" : getDeadline();
    }

    public String getDeadlineStatusLabel() {
        LocalDate deadlineDate = getDeadlineDate();
        if (deadlineDate == null) {
            return "Deadline pending";
        }
        if (isExpired()) {
            return "Deadline passed";
        }
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate);
        if (daysLeft == 0) {
            return "Closes today";
        }
        if (daysLeft == 1) {
            return "1 day left";
        }
        return daysLeft + " days left";
    }

    public String getRequiredSkillsSummary() {
        return getRequiredSkills().isEmpty() ? "No required skills" : String.join(", ", getRequiredSkills());
    }

    public String getPreferredSkillsSummary() {
        return getPreferredSkills().isEmpty() ? "No preferred skills" : String.join(", ", getPreferredSkills());
    }
}
