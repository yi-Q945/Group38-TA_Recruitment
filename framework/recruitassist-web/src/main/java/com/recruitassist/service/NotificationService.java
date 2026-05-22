package com.recruitassist.service;

import com.recruitassist.model.ActionResult;
import com.recruitassist.model.ApplicationRecord;
import com.recruitassist.model.ApplicationStatus;
import com.recruitassist.model.JobPosting;
import com.recruitassist.model.NotificationRecord;
import com.recruitassist.model.UserProfile;
import com.recruitassist.repository.IdCounterRepository;
import com.recruitassist.repository.NotificationRepository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final IdCounterRepository idCounterRepository;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public NotificationService(NotificationRepository notificationRepository, IdCounterRepository idCounterRepository) {
        this.notificationRepository = notificationRepository;
        this.idCounterRepository = idCounterRepository;
    }

    public List<NotificationRecord> findForUser(String userId) {
        lock.readLock().lock();
        try {
            return notificationRepository.findByRecipientId(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<NotificationRecord> findRecentForUser(String userId, int limit) {
        return findForUser(userId).stream()
                .limit(Math.max(limit, 0))
                .toList();
    }

    public long unreadCountForUser(String userId) {
        return findForUser(userId).stream()
                .filter(notification -> !notification.isRead())
                .count();
    }

    public void notifyStatusChange(ApplicationRecord application, JobPosting job, ApplicationStatus newStatus) {
        if (application == null || job == null || newStatus == null || newStatus == ApplicationStatus.SUBMITTED) {
            return;
        }

        NotificationRecord notification = new NotificationRecord();
        notification.setNotificationId(idCounterRepository.nextNotificationId());
        notification.setRecipientId(application.getApplicantId());
        notification.setApplicationId(application.getApplicationId());
        notification.setJobId(application.getJobId());
        notification.setTitle("Application " + newStatus.getLabel());
        notification.setMessage("Your application for " + job.getModuleCode() + " · " + job.getTitle()
                + " was updated to " + newStatus.getLabel() + ".");
        notification.setCreatedAt(Instant.now().toString());
        notification.setRead(false);

        lock.writeLock().lock();
        try {
            notificationRepository.save(notification);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ActionResult markAsRead(UserProfile actor, String notificationId) {
        if (actor == null) {
            return ActionResult.failure("Please sign in first.");
        }

        lock.writeLock().lock();
        try {
            NotificationRecord notification = notificationRepository.findById(notificationId).orElse(null);
            if (notification == null) {
                return ActionResult.failure("Notification not found.");
            }
            if (!notification.getRecipientId().equalsIgnoreCase(actor.getUserId())) {
                return ActionResult.failure("You can only update your own notifications.");
            }
            notification.setRead(true);
            notificationRepository.save(notification);
            return ActionResult.success("Notification marked as read.");
        } finally {
            lock.writeLock().unlock();
        }
    }
}
