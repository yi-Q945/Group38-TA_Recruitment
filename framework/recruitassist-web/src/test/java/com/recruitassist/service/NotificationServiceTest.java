package com.recruitassist.service;

import com.recruitassist.config.AppPaths;
import com.recruitassist.config.AppServices;
import com.recruitassist.model.ActionResult;
import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.ApplicationStatus;
import com.recruitassist.model.NotificationRecord;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty(AppPaths.BASE_DIR_PROPERTY);
    }

    @Test
    @DisplayName("MO status update should create unread TA notification")
    void statusUpdateCreatesNotification() throws Exception {
        System.setProperty(AppPaths.BASE_DIR_PROPERTY, tempDir.toString());
        AppPaths.ensureBaseStructure();
        AppServices services = new AppServices();
        UserProfile ta = taProfile();
        UserProfile mo = moProfile();
        services.userService().save(ta);
        services.userService().save(mo);

        ActionResult jobResult = services.jobService().createJob(
                mo,
                "Notification Java TA",
                "EBU8888",
                "Support Java notification labs.",
                "Java",
                "Communication",
                LocalDate.now().plusDays(7).toString(),
                "2",
                "3");
        assertTrue(jobResult.isSuccess(), jobResult.getMessage());
        String jobId = services.jobService().listJobsForOwner(mo.getUserId()).get(0).getJobId();

        ActionResult applyResult = services.applicationService().submitApplication(ta, jobId);
        assertTrue(applyResult.isSuccess(), applyResult.getMessage());
        ApplicationRecord application = services.applicationService().findByJobId(jobId).get(0);

        ActionResult updateResult = services.applicationService()
                .updateStatus(application.getApplicationId(), ApplicationStatus.SHORTLISTED, mo);
        assertTrue(updateResult.isSuccess(), updateResult.getMessage());

        List<NotificationRecord> notifications = services.notificationService().findForUser(ta.getUserId());
        assertEquals(1, notifications.size());
        NotificationRecord notification = notifications.get(0);
        assertFalse(notification.isRead());
        assertTrue(notification.getMessage().contains("Shortlisted"));
        assertEquals(1, services.notificationService().unreadCountForUser(ta.getUserId()));

        ActionResult readResult = services.notificationService().markAsRead(ta, notification.getNotificationId());
        assertTrue(readResult.isSuccess(), readResult.getMessage());
        assertEquals(0, services.notificationService().unreadCountForUser(ta.getUserId()));
    }

    private UserProfile taProfile() {
        UserProfile ta = new UserProfile();
        ta.setUserId("U9001");
        ta.setUsername("notify.ta");
        ta.setPassword(PasswordHasher.hash("demo123"));
        ta.setRole(UserRole.TA);
        ta.setName("Notify TA");
        ta.setStudentId("S9001");
        ta.setEmail("ta@example.com");
        ta.setProgramme("MSc Software Engineering");
        ta.setSkills(List.of("Java", "Testing"));
        ta.setAvailability("Weekdays");
        ta.setExperience("Supported Java labs.");
        return ta;
    }

    private UserProfile moProfile() {
        UserProfile mo = new UserProfile();
        mo.setUserId("U9101");
        mo.setUsername("notify.mo");
        mo.setPassword(PasswordHasher.hash("demo123"));
        mo.setRole(UserRole.MO);
        mo.setName("Notify MO");
        mo.setEmail("mo@example.com");
        return mo;
    }
}
