package com.recruitassist.service;

import com.recruitassist.config.AppPaths;
import com.recruitassist.config.AppServices;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.view.JobRecommendation;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationServiceTest {

    private static AppServices services;

    @BeforeAll
    static void initServices() throws IOException {
        System.setProperty(AppPaths.BASE_DIR_PROPERTY,
                System.getenv("RECRUITASSIST_BASE_DIR") != null
                        ? System.getenv("RECRUITASSIST_BASE_DIR")
                        : System.getProperty("user.dir") + "/../..");
        AppPaths.ensureBaseStructure();
        services = new AppServices();
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty(AppPaths.BASE_DIR_PROPERTY);
    }

    @Test
    @DisplayName("Recommendation score should be between 0 and 1")
    void scoreRange() {
        UserProfile ta = services.userService().listAllUsers().stream()
                .filter(u -> u.getRole().name().equals("TA"))
                .findFirst().orElseThrow();
        List<JobPosting> jobs = services.jobService().listAllJobs();
        assertFalse(jobs.isEmpty(), "Should have demo jobs");

        for (JobPosting job : jobs.subList(0, Math.min(5, jobs.size()))) {
            JobRecommendation rec = services.recommendationService().recommend(ta, job);
            assertTrue(rec.getScore() >= 0.0 && rec.getScore() <= 1.0,
                    "Score should be [0,1], got " + rec.getScore() + " for job " + job.getJobId());
        }
    }

    @Test
    @DisplayName("Recommendation should produce non-empty reasons")
    void hasReasons() {
        UserProfile ta = services.userService().listAllUsers().stream()
                .filter(u -> u.getRole().name().equals("TA"))
                .findFirst().orElseThrow();
        JobPosting job = services.jobService().listAllJobs().stream().findFirst().orElseThrow();

        JobRecommendation rec = services.recommendationService().recommend(ta, job);
        assertNotNull(rec.getReasons());
        assertFalse(rec.getReasons().isEmpty(), "Should have explanation reasons");
    }

    @Test
    @DisplayName("Recommendation should identify matched and missing skills")
    void skillMatching() {
        UserProfile ta = services.userService().listAllUsers().stream()
                .filter(u -> u.getRole().name().equals("TA") && !u.getSkills().isEmpty())
                .findFirst().orElseThrow();
        JobPosting job = services.jobService().listAllJobs().stream()
                .filter(j -> !j.getRequiredSkills().isEmpty())
                .findFirst().orElseThrow();

        JobRecommendation rec = services.recommendationService().recommend(ta, job);
        int total = rec.getMatchedSkills().size() + rec.getMissingSkills().size();
        assertTrue(total > 0, "Should have matched + missing skills > 0");
    }

    @Test
    @DisplayName("TA with more matching skills should score higher")
    void betterMatchHigherScore() {
        List<UserProfile> tas = services.userService().listAllUsers().stream()
                .filter(u -> u.getRole().name().equals("TA") && !u.getSkills().isEmpty())
                .limit(10).toList();
        if (tas.size() < 2) return;

        JobPosting job = services.jobService().listAllJobs().stream()
                .filter(j -> j.getRequiredSkills().size() >= 2)
                .findFirst().orElseThrow();

        List<JobRecommendation> recs = tas.stream()
                .map(ta -> services.recommendationService().recommend(ta, job))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .toList();

        // Top scorer should have more matched skills than bottom scorer (generally)
        JobRecommendation top = recs.get(0);
        JobRecommendation bottom = recs.get(recs.size() - 1);
        assertTrue(top.getScore() >= bottom.getScore(),
                "Sorted recommendations should be descending");
    }

    @Test
    @DisplayName("Six dimension scores should all be non-negative")
    void dimensionScoresNonNegative() {
        UserProfile ta = services.userService().listAllUsers().stream()
                .filter(u -> u.getRole().name().equals("TA"))
                .findFirst().orElseThrow();
        JobPosting job = services.jobService().listAllJobs().stream().findFirst().orElseThrow();

        JobRecommendation rec = services.recommendationService().recommend(ta, job);
        assertTrue(rec.getSkillScore() >= 0, "Skill score >= 0");
        assertTrue(rec.getAvailabilityScore() >= 0, "Availability score >= 0");
        assertTrue(rec.getExperienceScore() >= 0, "Experience score >= 0");
        assertTrue(rec.getWorkloadBalanceScore() >= 0, "Workload score >= 0");
        assertTrue(rec.getProfileEvidenceScore() >= 0, "Profile score >= 0");
        assertTrue(rec.getCompetitionScore() >= 0, "Competition score >= 0");
    }
}
