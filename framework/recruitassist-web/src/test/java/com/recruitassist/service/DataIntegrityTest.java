package com.recruitassist.service;

import com.recruitassist.config.AppPaths;
import com.recruitassist.config.AppServices;
import com.recruitassist.model.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DataIntegrityTest {

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

    // --- User Data ---

    @Test
    @DisplayName("All users should have unique IDs")
    void uniqueUserIds() {
        List<UserProfile> users = services.userService().listAllUsers();
        Set<String> ids = users.stream().map(UserProfile::getUserId).collect(Collectors.toSet());
        assertEquals(users.size(), ids.size(), "Duplicate user IDs found");
    }

    @Test
    @DisplayName("All users should have valid roles")
    void validUserRoles() {
        for (UserProfile user : services.userService().listAllUsers()) {
            assertNotNull(user.getRole(), "User " + user.getUserId() + " has null role");
            assertTrue(Set.of(UserRole.TA, UserRole.MO, UserRole.ADMIN).contains(user.getRole()),
                    "User " + user.getUserId() + " has invalid role: " + user.getRole());
        }
    }

    @Test
    @DisplayName("All users should have non-blank username")
    void nonBlankUsernames() {
        for (UserProfile user : services.userService().listAllUsers()) {
            assertNotNull(user.getUsername(), "User " + user.getUserId() + " has null username");
            assertFalse(user.getUsername().isBlank(), "User " + user.getUserId() + " has blank username");
        }
    }

    @Test
    @DisplayName("Should have at least 1 TA, 1 MO, 1 Admin")
    void hasAllRoles() {
        List<UserProfile> users = services.userService().listAllUsers();
        assertTrue(users.stream().anyMatch(u -> u.getRole() == UserRole.TA), "No TA found");
        assertTrue(users.stream().anyMatch(u -> u.getRole() == UserRole.MO), "No MO found");
        assertTrue(users.stream().anyMatch(u -> u.getRole() == UserRole.ADMIN), "No Admin found");
    }

    // --- Job Data ---

    @Test
    @DisplayName("All jobs should have unique IDs")
    void uniqueJobIds() {
        List<JobPosting> jobs = services.jobService().listAllJobs();
        Set<String> ids = jobs.stream().map(JobPosting::getJobId).collect(Collectors.toSet());
        assertEquals(jobs.size(), ids.size(), "Duplicate job IDs found");
    }

    @Test
    @DisplayName("All jobs should have valid status")
    void validJobStatuses() {
        for (JobPosting job : services.jobService().listAllJobs()) {
            assertNotNull(job.getStatus(), "Job " + job.getJobId() + " has null status");
        }
    }

    @Test
    @DisplayName("All jobs should have positive quota")
    void positiveJobQuota() {
        for (JobPosting job : services.jobService().listAllJobs()) {
            assertTrue(job.getQuota() > 0, "Job " + job.getJobId() + " has non-positive quota: " + job.getQuota());
        }
    }

    @Test
    @DisplayName("All job owners should be valid MO users")
    void jobOwnersAreMO() {
        Map<String, UserProfile> usersById = services.userService().indexById();
        for (JobPosting job : services.jobService().listAllJobs()) {
            UserProfile owner = usersById.get(job.getOwnerId());
            assertNotNull(owner, "Job " + job.getJobId() + " has unknown owner: " + job.getOwnerId());
            assertEquals(UserRole.MO, owner.getRole(),
                    "Job " + job.getJobId() + " owner " + owner.getUserId() + " is not MO");
        }
    }

    // --- Application Data ---

    @Test
    @DisplayName("All applications should reference valid users and jobs")
    void applicationReferences() {
        Map<String, UserProfile> usersById = services.userService().indexById();
        Map<String, JobPosting> jobsById = services.jobService().indexById();

        for (ApplicationRecord app : services.applicationService().findAll()) {
            assertNotNull(usersById.get(app.getApplicantId()),
                    "Application " + app.getApplicationId() + " references unknown user: " + app.getApplicantId());
            assertNotNull(jobsById.get(app.getJobId()),
                    "Application " + app.getApplicationId() + " references unknown job: " + app.getJobId());
        }
    }

    @Test
    @DisplayName("All applicants should be TAs")
    void applicantsAreTA() {
        Map<String, UserProfile> usersById = services.userService().indexById();
        for (ApplicationRecord app : services.applicationService().findAll()) {
            UserProfile applicant = usersById.get(app.getApplicantId());
            if (applicant != null) {
                assertEquals(UserRole.TA, applicant.getRole(),
                        "Applicant " + app.getApplicantId() + " is not TA");
            }
        }
    }

    @Test
    @DisplayName("All applications should have valid status")
    void validApplicationStatuses() {
        for (ApplicationRecord app : services.applicationService().findAll()) {
            assertNotNull(app.getStatus(), "App " + app.getApplicationId() + " has null status");
        }
    }

    @Test
    @DisplayName("Accepted count should not exceed job quota")
    void acceptedWithinQuota() {
        Map<String, JobPosting> jobsById = services.jobService().indexById();
        Map<String, Long> acceptedByJob = services.applicationService().findAll().stream()
                .filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED)
                .collect(Collectors.groupingBy(ApplicationRecord::getJobId, Collectors.counting()));

        for (var entry : acceptedByJob.entrySet()) {
            JobPosting job = jobsById.get(entry.getKey());
            if (job != null) {
                assertTrue(entry.getValue() <= job.getQuota(),
                        "Job " + entry.getKey() + " has " + entry.getValue()
                                + " accepted but quota is " + job.getQuota());
            }
        }
    }

    // --- Workload ---

    @Test
    @DisplayName("Workload calculation should be non-negative")
    void nonNegativeWorkload() {
        Map<String, Integer> workload = services.workloadService().workloadByUserId();
        for (var entry : workload.entrySet()) {
            assertTrue(entry.getValue() >= 0,
                    "User " + entry.getKey() + " has negative workload: " + entry.getValue());
        }
    }
}
