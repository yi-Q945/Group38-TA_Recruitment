package com.recruitassist.service;

import com.recruitassist.config.AppPaths;
import com.recruitassist.config.AppServices;
import com.recruitassist.model.ActionResult;
import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationConcurrencyTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty(AppPaths.BASE_DIR_PROPERTY);
    }

    @Test
    @DisplayName("Concurrent duplicate applications should create one record")
    void concurrentDuplicateApplicationsCreateOneRecord() throws Exception {
        System.setProperty(AppPaths.BASE_DIR_PROPERTY, tempDir.toString());
        AppPaths.ensureBaseStructure();
        AppServices services = new AppServices();

        UserProfile ta = taProfile();
        UserProfile mo = moProfile();
        services.userService().save(ta);
        services.userService().save(mo);
        ActionResult created = services.jobService().createJob(
                mo,
                "Concurrent Java Lab TA",
                "EBU9999",
                "Support Java labs and concurrent testing exercises.",
                "Java, Testing",
                "Concurrency",
                LocalDate.now().plusDays(14).toString(),
                "1",
                "4");
        assertTrue(created.isSuccess(), created.getMessage());
        JobPosting job = services.jobService().listJobsForOwner(mo.getUserId()).get(0);

        int requestCount = 24;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Callable<ActionResult>> tasks = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                tasks.add(() -> {
                    ready.countDown();
                    start.await();
                    return services.applicationService().submitApplication(ta, job.getJobId());
                });
            }
            List<Future<ActionResult>> futures = tasks.stream()
                    .map(executor::submit)
                    .toList();
            ready.await();
            start.countDown();

            long successCount = 0;
            for (Future<ActionResult> future : futures) {
                if (future.get().isSuccess()) {
                    successCount++;
                }
            }

            List<ApplicationRecord> saved = services.applicationService().findByJobId(job.getJobId());
            assertEquals(1, successCount, "Only one concurrent submit should succeed");
            assertEquals(1, saved.size(), "Only one application record should be persisted");
            assertTrue(saved.get(0).getApplicationId().startsWith("A"));
        } finally {
            executor.shutdownNow();
        }
    }

    private UserProfile taProfile() {
        UserProfile ta = new UserProfile();
        ta.setUserId("U9001");
        ta.setUsername("concurrent.ta");
        ta.setPassword(PasswordHasher.hash("demo123"));
        ta.setRole(UserRole.TA);
        ta.setName("Concurrent TA");
        ta.setStudentId("S9001");
        ta.setEmail("ta@example.com");
        ta.setProgramme("MSc Software Engineering");
        ta.setSkills(List.of("Java", "Testing", "Concurrency"));
        ta.setAvailability("Weekdays and weekends");
        ta.setExperience("Supported Java labs and testing workshops.");
        return ta;
    }

    private UserProfile moProfile() {
        UserProfile mo = new UserProfile();
        mo.setUserId("U9101");
        mo.setUsername("concurrent.mo");
        mo.setPassword(PasswordHasher.hash("demo123"));
        mo.setRole(UserRole.MO);
        mo.setName("Concurrent MO");
        mo.setEmail("mo@example.com");
        return mo;
    }
}
