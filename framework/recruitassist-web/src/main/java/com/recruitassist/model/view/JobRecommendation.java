package com.recruitassist.model.view;

import com.recruitassist.model.JobPosting;

import java.util.List;

public class JobRecommendation {
    private final JobPosting job;
    private final double score;
    private final List<String> matchedSkills;
    private final List<String> missingSkills;
    private final List<String> preferredMatchedSkills;
    private final List<String> reasons;
    private final double skillScore;
    private final double availabilityScore;
    private final double experienceScore;
    private final double workloadBalanceScore;
    private final double profileEvidenceScore;
    private final double competitionScore;
    private final int currentWorkloadHours;
    private final int projectedWorkloadHours;
    private final int workloadThresholdHours;
    private final int activeApplicantCount;
    private final int remainingSlots;

    public JobRecommendation(
            JobPosting job,
            double score,
            List<String> matchedSkills,
            List<String> missingSkills,
            List<String> preferredMatchedSkills,
            List<String> reasons,
            double skillScore,
            double availabilityScore,
            double experienceScore,
            double workloadBalanceScore,
            double profileEvidenceScore,
            double competitionScore,
            int currentWorkloadHours,
            int projectedWorkloadHours,
            int workloadThresholdHours,
            int activeApplicantCount,
            int remainingSlots) {
        this.job = job;
        this.score = score;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.preferredMatchedSkills = preferredMatchedSkills;
        this.reasons = reasons;
        this.skillScore = skillScore;
        this.availabilityScore = availabilityScore;
        this.experienceScore = experienceScore;
        this.workloadBalanceScore = workloadBalanceScore;
        this.profileEvidenceScore = profileEvidenceScore;
        this.competitionScore = competitionScore;
        this.currentWorkloadHours = currentWorkloadHours;
        this.projectedWorkloadHours = projectedWorkloadHours;
        this.workloadThresholdHours = workloadThresholdHours;
        this.activeApplicantCount = activeApplicantCount;
        this.remainingSlots = remainingSlots;
    }

    public JobPosting getJob() {
        return job;
    }

    public double getScore() {
        return score;
    }

    public int getScorePercent() {
        return toPercent(score);
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public List<String> getPreferredMatchedSkills() {
        return preferredMatchedSkills;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public double getSkillScore() {
        return skillScore;
    }

    public int getSkillScorePercent() {
        return toPercent(skillScore);
    }

    public double getAvailabilityScore() {
        return availabilityScore;
    }

    public int getAvailabilityScorePercent() {
        return toPercent(availabilityScore);
    }

    public double getExperienceScore() {
        return experienceScore;
    }

    public int getExperienceScorePercent() {
        return toPercent(experienceScore);
    }

    public double getWorkloadBalanceScore() {
        return workloadBalanceScore;
    }

    public int getWorkloadBalanceScorePercent() {
        return toPercent(workloadBalanceScore);
    }

    public double getProfileEvidenceScore() {
        return profileEvidenceScore;
    }

    public int getProfileEvidenceScorePercent() {
        return toPercent(profileEvidenceScore);
    }

    public double getCompetitionScore() {
        return competitionScore;
    }

    public int getCompetitionScorePercent() {
        return toPercent(competitionScore);
    }

    public int getCurrentWorkloadHours() {
        return currentWorkloadHours;
    }

    public int getProjectedWorkloadHours() {
        return projectedWorkloadHours;
    }

    public int getWorkloadThresholdHours() {
        return workloadThresholdHours;
    }

    public int getActiveApplicantCount() {
        return activeApplicantCount;
    }

    public int getRemainingSlots() {
        return remainingSlots;
    }

    public String getMatchedSkillsSummary() {
        return matchedSkills.isEmpty() ? "No matched skills yet" : String.join(", ", matchedSkills);
    }

    public String getMissingSkillsSummary() {
        return missingSkills.isEmpty() ? "No critical gaps" : String.join(", ", missingSkills);
    }

    public String getPreferredMatchedSkillsSummary() {
        return preferredMatchedSkills.isEmpty()
                ? "No preferred skills matched yet"
                : String.join(", ", preferredMatchedSkills);
    }

    public String getFitLabel() {
        if (score >= 0.85) {
            return "Strong fit";
        }
        if (score >= 0.65) {
            return "Good fit";
        }
        if (score >= 0.45) {
            return "Potential fit";
        }
        return "Needs development";
    }

    public String getActionLabel() {
        if (score >= 0.82 && competitionScore >= 0.6) {
            return "Apply early";
        }
        if (score >= 0.68) {
            return "Worth prioritising";
        }
        if (score >= 0.5) {
            return "Promising with some gaps";
        }
        return "Strengthen profile first";
    }

    public String getEvidenceLabel() {
        if (profileEvidenceScore >= 0.8) {
            return "Evidence-rich profile";
        }
        if (profileEvidenceScore >= 0.6) {
            return "Solid supporting evidence";
        }
        if (profileEvidenceScore >= 0.4) {
            return "Some evidence available";
        }
        return "Limited evidence on profile";
    }

    public String getProjectedWorkloadLabel() {
        return projectedWorkloadHours + " / " + workloadThresholdHours + " h after acceptance";
    }

    public String getCompetitionSummary() {
        if (remainingSlots <= 0) {
            return activeApplicantCount + " active applicants for a fully allocated quota";
        }
        return activeApplicantCount + " active applicants for " + remainingSlots + " remaining slot"
                + (remainingSlots == 1 ? "" : "s");
    }

    public String getWorkloadSummary() {
        return currentWorkloadHours + "h now · " + projectedWorkloadHours + "h if accepted";
    }

    private int toPercent(double value) {
        return (int) Math.round(Math.max(0.0, Math.min(value, 1.0)) * 100);
    }
}
