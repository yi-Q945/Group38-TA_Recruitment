package com.recruitassist.repository;

import com.recruitassist.config.AppPaths;
import com.recruitassist.model.UserProfile;
import com.recruitassist.model.UserRole;
import com.recruitassist.util.JsonFileStore;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private final JsonFileStore jsonFileStore;

    public UserRepository(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<UserProfile> findAll() {
        return jsonFileStore.readAll(AppPaths.usersDir(), UserProfile.class).stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();
    }

    public List<UserProfile> findByRole(UserRole role) {
        return findAll().stream()
                .filter(user -> user.getRole() == role)
                .toList();
    }

    public Optional<UserProfile> findById(String userId) {
        return findAll().stream()
                .filter(user -> user.getUserId().equalsIgnoreCase(userId))
                .findFirst();
    }

    public Optional<UserProfile> findByUsername(String username) {
        return findAll().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public void save(UserProfile userProfile) {
        jsonFileStore.write(filePath(userProfile.getUserId()), userProfile);
    }

    private Path filePath(String userId) {
        return AppPaths.usersDir().resolve(userId + ".json");
    }
}
