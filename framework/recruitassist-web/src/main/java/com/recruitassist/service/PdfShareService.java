package com.recruitassist.service;

import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;

public class PdfShareService {
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;

    public PdfShareService(UserService userService, JobService jobService, ApplicationService applicationService) {
        this.userService = userService;
        this.jobService = jobService;
        this.applicationService = applicationService;
    }

    public synchronized String ensureToken(UserProfile owner) {
        if (owner == null) {
            return "";
        }
        if (!owner.getPdfShareToken().isBlank()) {
            return owner.getPdfShareToken();
        }

        String token;
        do {
            token = generateToken();
        } while (findOwnerByToken(token).isPresent());

        owner.setPdfShareToken(token);
        userService.save(owner);
        return token;
    }

    public void ensureTokensForCvOwners(Collection<UserProfile> users) {
        if (users == null) {
            return;
        }
        users.stream()
                .filter(user -> user != null && user.isCvAvailable())
                .forEach(this::ensureToken);
    }

    public Optional<UserProfile> findOwnerByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String normalizedToken = token.trim();
        return userService.listAllUsers().stream()
                .filter(user -> user.getPdfShareToken().equals(normalizedToken))
                .findFirst();
    }

    public boolean canAccess(UserProfile viewer, UserProfile owner, String jobId) {
        if (viewer == null || owner == null || !owner.isCvAvailable()) {
            return false;
        }
        if (viewer.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (viewer.getUserId().equalsIgnoreCase(owner.getUserId())) {
            return true;
        }
        if (viewer.getRole() != UserRole.MO || jobId == null || jobId.isBlank()) {
            return false;
        }

        JobPosting job = jobService.findById(jobId.trim()).orElse(null);
        if (job == null || !job.getOwnerId().equalsIgnoreCase(viewer.getUserId())) {
            return false;
        }
        for (ApplicationRecord application : applicationService.findByJobId(job.getJobId())) {
            if (application.getApplicantId().equalsIgnoreCase(owner.getUserId())) {
                return true;
            }
        }
        return false;
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[32];
        TOKEN_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
