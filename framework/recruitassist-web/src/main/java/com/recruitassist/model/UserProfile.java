package com.recruitassist.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserProfile {
    private static final DateTimeFormatter CV_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private String userId;
    private String username;
    private String password;
    private UserRole role;
    private String name;
    private String studentId;
    private String email;
    private String programme;
    private List<String> skills = new ArrayList<>();
    private String availability;
    private String experience;
    private String cvText;
    private String cvFileName;
    private String cvUploadedAt;

    public String getUserId() {
        return userId == null ? "" : userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username == null ? "" : username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password == null ? "" : password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role == null ? UserRole.TA : role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentId() {
        return studentId == null ? "" : studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProgramme() {
        return programme == null ? "" : programme;
    }

    public void setProgramme(String programme) {
        this.programme = programme;
    }

    public List<String> getSkills() {
        return skills == null ? List.of() : skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getAvailability() {
        return availability == null ? "" : availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getExperience() {
        return experience == null ? "" : experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getCvText() {
        return cvText == null ? "" : cvText;
    }

    public void setCvText(String cvText) {
        this.cvText = cvText;
    }

    public String getCvFileName() {
        return cvFileName == null ? "" : cvFileName;
    }

    public void setCvFileName(String cvFileName) {
        this.cvFileName = cvFileName;
    }

    public String getCvUploadedAt() {
        return cvUploadedAt == null ? "" : cvUploadedAt;
    }

    public void setCvUploadedAt(String cvUploadedAt) {
        this.cvUploadedAt = cvUploadedAt;
    }

    public String getRoleLabel() {
        return getRole().getLabel();
    }

    public String getSkillsSummary() {
        return getSkills().isEmpty() ? "No skills listed yet" : String.join(", ", getSkills());
    }

    public boolean isCvAvailable() {
        return !getCvFileName().isBlank();
    }

    public String getCvFileLabel() {
        return isCvAvailable() ? getCvFileName() : "No CV uploaded";
    }

    public String getCvUploadedAtLabel() {
        if (getCvUploadedAt().isBlank()) {
            return "No upload recorded";
        }
        try {
            return CV_TIME_FORMATTER.format(Instant.parse(getCvUploadedAt()));
        } catch (Exception ignored) {
            return getCvUploadedAt();
        }
    }

    public boolean isProfileReady() {
        return !getName().isBlank()
                && !getStudentId().isBlank()
                && !getProgramme().isBlank()
                && !getSkills().isEmpty()
                && !getAvailability().isBlank();
    }

    public int getProfileSignalCount() {
        int signals = 0;
        if (!getSkills().isEmpty()) {
            signals++;
        }
        if (!getAvailability().isBlank()) {
            signals++;
        }
        if (!getExperience().isBlank()) {
            signals++;
        }
        if (!getCvText().isBlank() || isCvAvailable()) {
            signals++;
        }
        return signals;
    }

    public int getProfileSignalPercent() {
        return getProfileSignalCount() * 25;
    }
}
