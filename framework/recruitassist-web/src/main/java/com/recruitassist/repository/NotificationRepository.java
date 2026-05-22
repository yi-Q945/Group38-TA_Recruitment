package com.recruitassist.repository;

import com.recruitassist.config.AppPaths;
import com.recruitassist.model.NotificationRecord;
import com.recruitassist.util.JsonFileStore;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NotificationRepository {
    private final JsonFileStore jsonFileStore;

    public NotificationRepository(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<NotificationRecord> findAll() {
        return jsonFileStore.readAll(AppPaths.notificationsDir(), NotificationRecord.class).stream()
                .sorted(Comparator.comparing(NotificationRecord::getCreatedAt, Comparator.reverseOrder()))
                .toList();
    }

    public List<NotificationRecord> findByRecipientId(String recipientId) {
        return findAll().stream()
                .filter(notification -> notification.getRecipientId().equalsIgnoreCase(recipientId))
                .toList();
    }

    public Optional<NotificationRecord> findById(String notificationId) {
        return findAll().stream()
                .filter(notification -> notification.getNotificationId().equalsIgnoreCase(notificationId))
                .findFirst();
    }

    public void save(NotificationRecord notification) {
        jsonFileStore.write(filePath(notification.getNotificationId()), notification);
    }

    private Path filePath(String notificationId) {
        return AppPaths.notificationsDir().resolve(notificationId + ".json");
    }
}
