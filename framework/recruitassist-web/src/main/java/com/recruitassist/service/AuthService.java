package com.recruitassist.service;

import com.recruitassist.model.UserProfile;

import java.util.Optional;

public class AuthService {
    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public Optional<UserProfile> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        String normalizedUsername = username.trim();
        String normalizedPassword = password.trim();
        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            return Optional.empty();
        }

        return userService.findByUsername(normalizedUsername)
                .filter(user -> user.getPassword().equals(normalizedPassword));
    }
}
