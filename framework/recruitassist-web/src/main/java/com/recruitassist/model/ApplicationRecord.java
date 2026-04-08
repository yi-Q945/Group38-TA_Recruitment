package com.recruitassist.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ApplicationRecord {
    private static final DateTimeFormatter APPLY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private String applicationId;
    private String jobId;
    private String applicantId;
    private String applyTime;
    private ApplicationStatus status;
    private double recommendationScore;
    private List<String> explanation = new ArrayList<>();

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplyTime() {
        return applyTime == null ? "" : applyTime;
    }

    public void setApplyTime(String applyTime) {
        this.applyTime = applyTime;
    }

    public ApplicationStatus getStatus() {
        return status == null ? ApplicationStatus.SUBMITTED : status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public double getRecommendationScore() {
        return recommendationScore;
    }

    public void setRecommendationScore(double recommendationScore) {
        this.recommendationScore = recommendationScore;
    }

    public List<String> getExplanation() {
        return explanation == null ? List.of() : explanation;
    }

    public void setExplanation(List<String> explanation) {
        this.explanation = explanation;
    }

    public int getRecommendationPercent() {
        return (int) Math.round(recommendationScore * 100);
    }

    public String getExplanationSummary() {
        return getExplanation().isEmpty() ? "No explanation available" : String.join(" • ", getExplanation());
    }

    public String getApplyTimeLabel() {
        if (getApplyTime().isBlank()) {
            return "Not available";
        }
        try {
            return APPLY_TIME_FORMATTER.format(Instant.parse(getApplyTime()));
        } catch (Exception ignored) {
            return getApplyTime();
        }
    }
}
